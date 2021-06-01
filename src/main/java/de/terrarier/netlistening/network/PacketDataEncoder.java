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
package de.terrarier.netlistening.network;

import de.terrarier.netlistening.Server;
import de.terrarier.netlistening.api.DataComponent;
import de.terrarier.netlistening.api.DataContainer;
import de.terrarier.netlistening.api.PacketCaching;
import de.terrarier.netlistening.api.encryption.EncryptionSetting;
import de.terrarier.netlistening.api.encryption.hash.HashUtil;
import de.terrarier.netlistening.api.encryption.hash.HmacApplicationPolicy;
import de.terrarier.netlistening.api.encryption.hash.HmacSetting;
import de.terrarier.netlistening.api.type.DataType;
import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.internals.AssumeNotNull;
import de.terrarier.netlistening.internals.CancelSignal;
import de.terrarier.netlistening.internals.InternalPayloadRegisterPacket;
import de.terrarier.netlistening.internals.InternalUtil;
import de.terrarier.netlistening.utils.ByteBufUtilExtension;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.jetbrains.annotations.ApiStatus;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * @author Terrarier2111
 * @since 1.0
 */
@ApiStatus.Internal
public final class PacketDataEncoder extends MessageToByteEncoder<DataContainer> {

    private final ApplicationImpl application;
    private final ExecutorService delayedExecutor;
    private final ConnectionImpl connection;

    public PacketDataEncoder(@AssumeNotNull ApplicationImpl application, ExecutorService delayedExecutor,
                             @AssumeNotNull ConnectionImpl connection) {
        this.application = application;
        this.delayedExecutor = delayedExecutor;
        this.connection = connection;
    }

    @Override
    protected void encode(@AssumeNotNull ChannelHandlerContext ctx, @AssumeNotNull DataContainer data,
                          @AssumeNotNull ByteBuf buffer) {
        final List<DataComponent<?>> containedData = data.getData();
        final int dataSize = containedData.size();

        if (dataSize < 1) {
            throw new IllegalArgumentException("Tried to send an empty packet!");
        }

        final DataType<?>[] types = new DataType[dataSize];
        boolean serialize = false;
        for (int i = 0; i < dataSize; i++) {
            final DataType<?> type = containedData.get(i).getType();
            types[i] = type;
            if (type == DataType.OBJECT) {
                serialize = true;
            }
        }
        final PacketCache cache = application.getCache();
        PacketSkeleton packet = cache.getPacket(types);

        if (packet == null) {
            packet = cache.registerPacket(types);
            final InternalPayloadRegisterPacket register = new InternalPayloadRegisterPacket(packet.getId(), types);
            final ByteBuf registerBuffer = Unpooled.buffer(5 + dataSize);
            DataType.getDTIP().write0(application, registerBuffer, register);
            buffer.writeBytes(registerBuffer);
            if (application.getCaching() == PacketCaching.GLOBAL) {
                cache.broadcastRegister(application, register, connection, registerBuffer);
            } else {
                registerBuffer.release();
            }
            packet.register();
        }

        if (application instanceof Server && !packet.isRegistered()) {
            if (delayedExecutor.isShutdown()) {
                return;
            }
            final PacketSkeleton finalPacket = packet;
            // Sending data delayed, awaiting the packet's registration to finish.
            delayedExecutor.execute(() -> {
                final Channel channel = ctx.channel();
                while (!finalPacket.isRegistered());
                channel.writeAndFlush(data);
            });
            return;
        }
        final ByteBuf dstBuffer = serialize ? ctx.alloc().buffer() : buffer;
        final EncryptionSetting encryptionSetting = application.getEncryptionSetting();
        final boolean encrypted = data.isEncrypted();
        final HmacSetting hmacSetting;

        try {
            if (encryptionSetting == null ||
                    (((hmacSetting = encryptionSetting.getHmacSetting()) == null ||
                            hmacSetting.getApplicationPolicy() == HmacApplicationPolicy.ENCRYPTED) && !encrypted)) {
                writeToBuffer(dstBuffer, data, packet.getId());
                if (serialize) {
                    buffer.writeBytes(dstBuffer);
                    dstBuffer.release();
                }
                return;
            }

            final boolean hmac = (encrypted || hmacSetting.getApplicationPolicy() == HmacApplicationPolicy.ALL) &&
                    hmacSetting != null;
            final ByteBuf hmacBuffer = hmac ? ctx.alloc().buffer() : dstBuffer;
            final ByteBuf encryptionBuffer = encrypted ? ctx.alloc().buffer() : hmacBuffer;
            writeToBuffer(encryptionBuffer, data, packet.getId());
            if (encrypted) {
                InternalUtil.writeInt(application, hmacBuffer, 0x3);
                final byte[] encryptedData = connection.getEncryptionContext().encrypt(
                        ByteBufUtilExtension.getBytes(encryptionBuffer));
                encryptionBuffer.release();
                final int size = encryptedData.length;
                ByteBufUtilExtension.correctSize(hmacBuffer, 4 + size, application.getBuffer());
                hmacBuffer.writeInt(size);
                hmacBuffer.writeBytes(encryptedData);
            }

            if (serialize) {
                buffer.writeBytes(dstBuffer);
                dstBuffer.release();
            }
            if (hmac) {
                appendHmac(hmacBuffer, buffer, connection);
            }
        } catch (CancelSignal ignored) {
            // This is here in order to prevent packets from being sent which contain unserializable data.
            if (serialize) { // TODO: Check if this check is needed!
                dstBuffer.release();
            }
        }
    }

    private void writeToBuffer(@AssumeNotNull ByteBuf buffer, @AssumeNotNull DataContainer data, int packetId)
            throws CancelSignal {
        InternalUtil.writeInt(application, buffer, packetId);
        final List<DataComponent<?>> dataComponentList = data.getData();
        final int dataSize = dataComponentList.size();
        for (int i = 0; i < dataSize; i++) {
            final DataComponent<?> component = dataComponentList.get(i);
            component.getType().writeUnchecked(application, buffer, component.getData());
        }
    }

    private void appendHmac(@AssumeNotNull ByteBuf src, @AssumeNotNull ByteBuf dst,
                            @AssumeNotNull ConnectionImpl connection) {
        final byte[] data = ByteBufUtilExtension.getBytes(src);
        src.release();
        try {
            final byte[] hash = HashUtil.calculateHMAC(data, connection.getHmacKey(),
                    application.getEncryptionSetting().getHmacSetting().getHashingAlgorithm());
            final int dataLength = data.length;
            final short hashLength = (short) hash.length;
            InternalUtil.writeInt(application, dst, 0x4);
            ByteBufUtilExtension.correctSize(dst, 4 + 2 + dataLength + hashLength, application.getBuffer());
            dst.writeInt(dataLength);
            dst.writeShort(hashLength);
            dst.writeBytes(data);
            dst.writeBytes(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            application.getEventManager().handleExceptionThrown(e);
        }
    }

}
