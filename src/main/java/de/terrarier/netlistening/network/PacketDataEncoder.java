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
import de.terrarier.netlistening.api.encryption.hash.HmacApplicationPolicy;
import de.terrarier.netlistening.api.encryption.hash.HmacSetting;
import de.terrarier.netlistening.api.type.DataType;
import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.internal.AssumeNotNull;
import de.terrarier.netlistening.internal.CancelSignal;
import de.terrarier.netlistening.internal.InternalPayloadRegisterPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.MessageToByteEncoder;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.concurrent.ExecutorService;

import static de.terrarier.netlistening.internal.InternalUtil.writeInt;

/**
 * @author Terrarier2111
 * @since 1.0
 */
@ApiStatus.Internal
public final class PacketDataEncoder extends MessageToByteEncoder<DataContainer> {

    private final ApplicationImpl application;
    private final ConnectionImpl connection;
    private final ExecutorService delayedExecutor;

    public PacketDataEncoder(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ConnectionImpl connection,
                             ExecutorService delayedExecutor) {
        this.application = application;
        this.connection = connection;
        this.delayedExecutor = delayedExecutor;
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
        final boolean[] notifier = new boolean[1];
        final PacketSkeleton packet = cache.getOrRegisterPacket(notifier, types);

        if (notifier[0]) {
            synchronized (packet) {
                final InternalPayloadRegisterPacket register = new InternalPayloadRegisterPacket(packet.getId(), types);
                final ByteBuf registerBuffer = Unpooled.buffer(4 + 1 + dataSize);
                DataType.getDTIP().write(application, registerBuffer, register);
                buffer.writeBytes(registerBuffer);
                if (application.getCaching() == PacketCaching.GLOBAL) {
                    cache.broadcastRegister(application, register, connection, registerBuffer);
                } else {
                    registerBuffer.release();
                }
                packet.register();
            }
        } else if (application instanceof Server && !packet.isRegistered()) {
            if (delayedExecutor.isShutdown()) {
                return;
            }
            // Sending data delayed, awaiting the packet's registration to finish.
            delayedExecutor.execute(() -> {
                final Channel channel = ctx.channel();
                final ChannelPromise voidPromise = channel.voidPromise();
                if (!packet.isRegistered()) {
                    try {
                        packet.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                channel.writeAndFlush(data, voidPromise);
            });
            return;
        }
        final EncryptionSetting encryptionSetting = application.getEncryptionSetting();
        final boolean encrypted = data.isEncrypted();
        final HmacSetting hmacSetting;

        if (encryptionSetting == null ||
                (((hmacSetting = encryptionSetting.getHmacSetting()) == null ||
                        hmacSetting.getApplicationPolicy() == HmacApplicationPolicy.ENCRYPTED) && !encrypted)) {
            final ByteBuf dstBuffer = serialize ? ctx.alloc().buffer() : buffer;
            if (writeToBuffer(dstBuffer, data, packet.getId(), serialize) && serialize) {
                // This is here in order to prevent packets from being sent which contain unserializable data.
                buffer.writeBytes(dstBuffer);
                dstBuffer.release();
            }
            return;
        }

        final boolean hmac = (encrypted || hmacSetting.getApplicationPolicy() == HmacApplicationPolicy.ALL) &&
                hmacSetting != null;
        final boolean separateBuffer = serialize || hmac || encrypted;
        final ByteBuf tmpBuffer = separateBuffer ? ctx.alloc().buffer() : buffer;
        if (!writeToBuffer(tmpBuffer, data, packet.getId(), separateBuffer)) {
            // This is here in order to prevent packets from being sent which contain unserializable data.
            return;
        }

        if (encrypted) {
            DataType.getDTE().write0(application, connection, tmpBuffer, null);
        }
        if (hmac) {
            DataType.getDTHMAC().write0(application, connection, buffer, tmpBuffer);
        } else if (serialize) {
            buffer.writeBytes(tmpBuffer);
            tmpBuffer.release();
        }
    }

    private boolean writeToBuffer(@AssumeNotNull ByteBuf buffer, @AssumeNotNull DataContainer data, int packetId,
                                  boolean releaseOnError) {
        writeInt(application, buffer, packetId);
        final List<DataComponent<?>> dataComponentList = data.getData();
        final int dataSize = dataComponentList.size();
        for (int i = 0; i < dataSize; i++) {
            final DataComponent<?> component = dataComponentList.get(i);
            try {
                component.getType().writeUnchecked(application, connection, buffer, component.getData());
            } catch (CancelSignal signal) {
                // Releases data in order to prevent memory leaks in case of an error.
                if (releaseOnError) {
                    buffer.release();
                }
                return false;
            }
        }
        return true;
    }

}
