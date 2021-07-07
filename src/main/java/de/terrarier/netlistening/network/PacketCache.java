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

import de.terrarier.netlistening.Connection;
import de.terrarier.netlistening.api.type.DataType;
import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.internal.AssumeNotNull;
import de.terrarier.netlistening.internal.InternalPayloadRegisterPacket;
import de.terrarier.netlistening.internal.InternalUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Terrarier2111
 * @since 1.0
 */
@ApiStatus.Internal
public final class PacketCache {

    private static final PacketSkeleton ENCRYPTION_PACKET_SKELETON = new PacketSkeleton(0x3, DataType.getDTE());
    private static final PacketSkeleton HMAC_PACKET_SKELETON = new PacketSkeleton(0x4, DataType.getDTHMAC());
    private final Map<Integer, PacketSkeleton> idPacketMapping = new ConcurrentHashMap<>();
    private final Map<DataType<?>[], PacketSkeleton> dataTypePacketMapping = new ConcurrentHashMap<>();
    private final Lock lock = new ReentrantLock(true);
    private volatile int id = 5;

    public PacketCache() {
        idPacketMapping.put(0x3, ENCRYPTION_PACKET_SKELETON);
        idPacketMapping.put(0x4, HMAC_PACKET_SKELETON);
    }

    @AssumeNotNull
    public Map<Integer, PacketSkeleton> getPackets() {
        return idPacketMapping;
    }

    @AssumeNotNull
    public PacketSkeleton tryRegisterPacket(int id, @AssumeNotNull ConnectionImpl connection,
                                            @AssumeNotNull DataType<?>... data) {
        lock.lock();
        try {
            final int currId = this.id;
            final boolean valid = id == currId;
            if (!valid) {
                final PacketSkeleton packet = getPacket(data);
                if (packet != null) {
                    connection.getPacketIdTranslationCache().insert(id, packet.getId());
                    return packet;
                }
                connection.getPacketIdTranslationCache().insert(id, currId);
            }
            return registerPacket(this.id++, data);
        } finally {
            lock.unlock();
        }
    }

    public void forceRegisterPacket(int id, @AssumeNotNull DataType<?>... data) {
        lock.lock();
        try {
            final int curr = this.id;
            if (id > curr) {
                this.id = id;
            } else if (id == curr) {
                this.id++;
            }

            registerPacket(id, data);
        } finally {
            lock.unlock();
        }
    }

    @AssumeNotNull
    private PacketSkeleton registerPacket(int id, @AssumeNotNull DataType<?>... data) {
        final PacketSkeleton packet = new PacketSkeleton(id, data);
        idPacketMapping.put(id, packet);
        dataTypePacketMapping.put(data, packet);
        return packet;
    }

    PacketSkeleton getPacket(@AssumeNotNull DataType<?>... data) {
        return dataTypePacketMapping.get(data);
    }

    @AssumeNotNull
    PacketSkeleton getOrRegisterPacket(@AssumeNotNull boolean[] notifier, @AssumeNotNull DataType<?>... data) {
        lock.lock();
        try {
            final PacketSkeleton packet = getPacket(data);
            if (packet != null) {
                return packet;
            }
            notifier[0] = true;
            return registerPacket(id++, data);
        } finally {
            lock.unlock();
        }
    }

    PacketSkeleton getPacket(int id) {
        return idPacketMapping.get(id);
    }

    public void broadcastRegister(@AssumeNotNull ApplicationImpl application,
                                  @AssumeNotNull InternalPayloadRegisterPacket payload,
                                  Connection ignored, ByteBuf buffer) {
        final Collection<ConnectionImpl> connections = application.getConnectionsRaw();
        if (ignored == null || connections.size() > 1) {
            final ByteBuf registerBuffer = buffer != null ? buffer : Unpooled.buffer(
                    1 + InternalUtil.singleOctetIntSize(application) + payload.getSize(application));

            if (buffer == null) {
                DataType.getDTIP().write0(application, registerBuffer, payload);
            }

            for (ConnectionImpl connection : connections) {
                if (ignored == null || connection.getId() != ignored.getId()) {
                    registerBuffer.retain();
                    if (connection.isConnected()) {
                        final Channel channel = connection.getChannel();
                        channel.writeAndFlush(registerBuffer, channel.voidPromise());
                    } else {
                        connection.writeToInitialBuffer(registerBuffer);
                    }
                }
            }
            registerBuffer.release();
        }
    }

    public void swapId(int former, int next) {
        lock.lock();
        try {
            if (next > id) {
                id = next;
            }
            idPacketMapping.put(next, idPacketMapping.get(former));
        } finally {
            lock.unlock();
        }
    }

    public void clear() {
        idPacketMapping.clear();
        dataTypePacketMapping.clear();
    }

}
