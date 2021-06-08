/*
Copyright 2021 Terrarier2111

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package de.terrarier.netlistening.impl;

import de.terrarier.netlistening.Application;
import de.terrarier.netlistening.Client;
import de.terrarier.netlistening.Connection;
import de.terrarier.netlistening.Server;
import de.terrarier.netlistening.api.DataContainer;
import de.terrarier.netlistening.api.PacketCaching;
import de.terrarier.netlistening.api.encryption.EncryptionOptions;
import de.terrarier.netlistening.api.encryption.SymmetricEncryptionContext;
import de.terrarier.netlistening.api.encryption.SymmetricEncryptionUtil;
import de.terrarier.netlistening.api.type.DataType;
import de.terrarier.netlistening.internals.*;
import de.terrarier.netlistening.network.PacketCache;
import de.terrarier.netlistening.network.PacketIdTranslationCache;
import de.terrarier.netlistening.network.PacketSkeleton;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPromise;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import javax.crypto.SecretKey;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static de.terrarier.netlistening.utils.ByteBufUtilExtension.correctSize;
import static de.terrarier.netlistening.utils.ByteBufUtilExtension.getBytes;

/**
 * @author Terrarier2111
 * @since 1.0
 */
public final class ConnectionImpl implements Connection {

    static final AtomicInteger ID = new AtomicInteger();
    private final ApplicationImpl application;
    private final Channel channel;
    private final int id = ID.getAndIncrement();
    private final PacketCache cache;
    private PacketIdTranslationCache packetIdTranslationCache;
    private volatile boolean receivedPacket;
    private volatile DataSendState dataSendState = DataSendState.IDLE;
    private ByteBuf preConnectBuffer;
    private List<DataContainer> preConnectSendQueue;
    private SymmetricEncryptionContext encryptionContext;
    private byte[] hmacKey;
    // TODO: Improve and test delayed data sending mechanics.

    ConnectionImpl(@AssumeNotNull ApplicationImpl application, @AssumeNotNull Channel channel) {
        this.application = application;
        this.channel = channel;
        if (application.getCaching() != PacketCaching.INDIVIDUAL) {
            cache = application.getCache();
        } else {
            cache = new PacketCache();
        }
        if (application instanceof Server) {
            packetIdTranslationCache = new PacketIdTranslationCache(this, application);
        }
    }

    /**
     * @see Connection#sendData(DataContainer)
     */
    @Override
    public void sendData(@NotNull DataContainer data) {
        if (application instanceof Client) {
            application.sendData(data);
            return;
        }
        final boolean connected = isConnected();
        checkReceived();
        if (connected && dataSendState.isAtLeast(DataSendState.SENDING)) {
            if (dataSendState == DataSendState.FINISHED) {
                channel.writeAndFlush(data, channel.voidPromise());
            } else {
                // TODO: Handle stuff incoming before!
            }
        } else {
            synchronized (this) {
                if (preConnectSendQueue == null) {
                    preConnectSendQueue = new ArrayList<>();
                }

                preConnectSendQueue.add(data);
            }
        }
    }

    /**
     * @see Connection#sendData(boolean, Object...)
     */
    @Deprecated
    @Override
    public void sendData(boolean encrypted, @NotNull Object... data) {
        final DataContainer dataContainer = new DataContainer();
        dataContainer.setEncrypted(encrypted);
        dataContainer.addAll(data);
        sendData(dataContainer);
    }

    /**
     * @see Connection#disconnect()
     */
    @Override
    public void disconnect() {
        if (!isConnected()) {
            throw new IllegalStateException(
                    "The connection " + Integer.toHexString(id) + " is not connected!");
        }
        if (application instanceof Server) {
            ((ServerImpl) application).disconnect0(this);
        }
        disconnect0();
    }

    void disconnect0() {
        if (application.getCaching() != PacketCaching.GLOBAL) {
            cache.clear();
        }
        channel.close();
    }

    /**
     * @see Connection#isConnected()
     */
    @Override
    public boolean isConnected() {
        return channel.isActive() || channel.isOpen();
    }

    @AssumeNotNull
    @Deprecated
    public Application getApplication() {
        return application;
    }

    /**
     * @see Connection#getChannel()
     */
    @AssumeNotNull
    @Override
    public Channel getChannel() {
        return channel;
    }

    /**
     * @see Connection#getRemoteAddress()
     */
    @AssumeNotNull
    @Override
    public InetSocketAddress getRemoteAddress() {
        return (InetSocketAddress) channel.remoteAddress();
    }

    // TODO: Add any sort of documentation to this method!
    public SymmetricEncryptionContext getEncryptionContext() {
        return encryptionContext;
    }

    /**
     * Defines the encryption context for this connection which determines
     * how data sent to/received from this connection gets en-/decrypted.
     *
     * @param options      the options which should be used to interpret the key data.
     * @param symmetricKey the data which should be used to generate the key.
     */
    public void setSymmetricKey(@NotNull EncryptionOptions options, byte @NotNull [] symmetricKey) {
        final SecretKey secretKey = SymmetricEncryptionUtil.readSecretKey(symmetricKey,
                options);
        encryptionContext = new SymmetricEncryptionContext(secretKey, options);
    }

    /**
     * Defines the encryption context for this connection which determines
     * how data sent to/received from this connection gets en-/decrypted.
     *
     * @param application the application to which this connection is related to.
     * @param secretKey   the secret key which should be used to encrypt data.
     */
    public void setSymmetricKey(@NotNull ApplicationImpl application, @NotNull SecretKey secretKey) {
        final EncryptionOptions options = application.getEncryptionSetting().getSymmetricSetting();
        encryptionContext = new SymmetricEncryptionContext(secretKey, options);
    }

    /**
     * @return the internal symmetric key which is used to hash data.
     * If hashing is disabled, it returns null.
     */
    public byte[] getHmacKey() {
        return hmacKey;
    }

    /**
     * Sets an internal symmetric key of the connection.
     *
     * @param key the key which should be used to hash data.
     */
    public void setHmacKey(byte @NotNull [] key) {
        hmacKey = key;
    }

    /**
     * Sets an internal symmetric key of the connection.
     *
     * @param secretKey the SecretKey which should be used to generate hmacs for data.
     */
    public void setHmacKey(@NotNull SecretKey secretKey) {
        setHmacKey(secretKey.getEncoded());
    }

    /**
     * @see Connection#getId()
     */
    @Override
    public int getId() {
        return id;
    }

    @ApiStatus.Internal
    public boolean isStable() {
        return dataSendState.isAtLeast(DataSendState.SENDING) && receivedPacket;
    }

    @ApiStatus.Internal
    @AssumeNotNull
    public PacketCache getCache() {
        return cache;
    }

    @ApiStatus.Internal
    public PacketIdTranslationCache getPacketIdTranslationCache() {
        return packetIdTranslationCache;
    }

    private void checkReceived() {
        if (!receivedPacket) {
            receivedPacket = true;
            final boolean connected = isConnected();

            final DataTypeInternalPayload dtip = DataType.getDTIP();
            final PacketCaching caching = application.getCaching();
            final ByteBuf buffer;
            synchronized (this) {
                if (connected) {
                    buffer = Unpooled.buffer();
                } else {
                    if (preConnectBuffer == null) {
                        preConnectBuffer = Unpooled.buffer();
                    }
                    buffer = preConnectBuffer;
                }
                buffer.writeInt(0x0);
                dtip.write(application, buffer, InternalPayload.HANDSHAKE);
                if (caching == PacketCaching.GLOBAL) {
                    final Map<Integer, PacketSkeleton> packets = cache.getPackets();
                    final int packetsSize = packets.size();
                    if (packetsSize > 3) {
                        for (int id = 5; id < packetsSize + 2; id++) {
                            final DataType<?>[] data = packets.get(id).getData();
                            dtip.write0(application, buffer, new InternalPayloadRegisterPacket(id, data));
                        }
                    }
                }
                if (connected) {
                    channel.writeAndFlush(buffer, channel.voidPromise());
                }
            }
        }
    }

    @ApiStatus.Internal
    public void check() {
        if (!dataSendState.isAtLeast(DataSendState.SENDING)) {
            dataSendState = DataSendState.SENDING;

            synchronized (this) {
                if (preConnectBuffer != null && preConnectBuffer.isReadable()) {
                    channel.writeAndFlush(preConnectBuffer, channel.voidPromise());
                    preConnectBuffer = null;
                } else {
                    // Writing the init data to the channel (without hitting the pre connect buffer).
                    checkReceived();
                }
            }

            if (application.getEncryptionSetting() == null) {
                prepare();
            } else {
                dataSendState = DataSendState.WAITING_FOR_FINISH;
            }
        }
    }

    @ApiStatus.Internal
    public void writeToInitialBuffer(@AssumeNotNull ByteBuf buffer) {
        final DataSendState dataSendState = this.dataSendState; // caching volatile field get result
        if (!dataSendState.isAtLeast(DataSendState.SENDING)) {
            checkReceived();
            transferData(buffer);
        } else {
            if (!trySend(buffer)) {
                // TODO: Test logic to send data delayed!
                if (dataSendState != DataSendState.WAITING_FOR_FINISH) { // Check if the connection isn't in the waiting state and therefore
                    // currently it isn't waiting for a response from the other end of the connection.
                    while (true) {
                        if (this.dataSendState == DataSendState.WAITING_FOR_FINISH) {
                            break;
                        } else if (trySend(buffer)) {
                            return;
                        }
                    }
                }
                synchronized (this) {
                    if (preConnectBuffer == null) {
                        preConnectBuffer = Unpooled.buffer();
                    }
                }
                transferData(buffer);
            }
        }
    }

    private boolean trySend(@AssumeNotNull ByteBuf buffer) {
        final DataSendState dataSendState = this.dataSendState; // caching volatile field get result
        if (dataSendState.isAtLeast(DataSendState.FINISHING)) {
            if (dataSendState == DataSendState.FINISHING) {
                // We are waiting until the execution of the prepare method has finished.
                while (this.dataSendState != DataSendState.FINISHED);
            }
            channel.writeAndFlush(buffer, channel.voidPromise());
            return true;
        }
        return false;
    }

    private void transferData(@AssumeNotNull ByteBuf buffer) {
        final int readable = buffer.readableBytes();
        final byte[] bytes = getBytes(buffer, readable);
        final int applicationBuffer = application.getBuffer();
        synchronized (this) {
            correctSize(preConnectBuffer, readable, applicationBuffer);
            preConnectBuffer.writeBytes(bytes);
        }
        buffer.release();
    }

    @ApiStatus.Internal
    public void prepare() {
        dataSendState = DataSendState.FINISHING;
        final ChannelPromise voidPromise = channel.voidPromise();
        synchronized (this) {
            if (preConnectSendQueue != null) {
                final List<DataContainer> sendQueue = preConnectSendQueue;
                preConnectSendQueue = null;
                final int sendQueueSize = sendQueue.size();
                for (int i = 0; i < sendQueueSize; i++) {
                    final DataContainer data = sendQueue.get(i);
                    channel.writeAndFlush(data, voidPromise);
                }
                sendQueue.clear();
            }
        }

        final int lowSize = InternalUtil.getSingleByteSize(application);
        final ByteBuf buffer = Unpooled.buffer(lowSize + 1 + lowSize);
        DataType.getDTIP().write0(application, buffer, InternalPayload.PUSH_REQUEST);
        channel.writeAndFlush(buffer, voidPromise);
        // TODO: IMPORTANT: Check if we should write the preConnectBuffer first.
        synchronized (this) {
            if (preConnectBuffer != null) {
                channel.writeAndFlush(preConnectBuffer, voidPromise);
                preConnectBuffer = null;
            }
        }
        dataSendState = DataSendState.FINISHED;
    }

    private enum DataSendState {

        IDLE, SENDING, WAITING_FOR_FINISH, FINISHING, FINISHED;

        private boolean isAtLeast(@NotNull DataSendState state) {
            return ordinal() >= state.ordinal();
        }

    }

}
