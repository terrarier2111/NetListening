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
package de.terrarier.netlistening.internals;

import de.terrarier.netlistening.Client;
import de.terrarier.netlistening.api.PacketCaching;
import de.terrarier.netlistening.api.compression.NibbleUtil;
import de.terrarier.netlistening.api.event.EventManager;
import de.terrarier.netlistening.api.event.InvalidDataEvent;
import de.terrarier.netlistening.api.event.ListenerType;
import de.terrarier.netlistening.api.type.DataType;
import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.network.PacketCache;
import de.terrarier.netlistening.network.PacketSkeleton;
import de.terrarier.netlistening.utils.ConversionUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.ApiStatus;

/**
 * @author Terrarier2111
 * @since 1.0
 */
@ApiStatus.Internal
public final class InternalPayloadRegisterPacket extends InternalPayload {

    private final int packetId;
    private final DataType<?>[] types;

    public InternalPayloadRegisterPacket(int packetId, @AssumeNotNull DataType<?>... types) {
        super((byte) 0x1);
        this.packetId = packetId;
        this.types = types;
    }

    @Override
    void write(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ByteBuf buffer) {
        final int typesLength = types.length;

        if (typesLength == 0) {
            throw new IllegalArgumentException("Tried to send an empty packet!");
        }
        checkWriteable(application, buffer, getSize(application));
        InternalUtil.writeIntUnchecked(application, buffer, packetId);
        buffer.writeShort(typesLength);

        final boolean nibbleCompression = application.getCompressionSetting().isNibbleCompression();
        for (int i = 0; i < typesLength; i++) {
            byte id = (byte) (types[i].getId() - 1);
            if (id < 0x0) {
                throw new IllegalArgumentException("Tried to send a packet containing an internal payload!");
            }

            if (nibbleCompression && typesLength > ++i) {
                final byte other = (byte) (types[i].getId() - 1);
                if (other < 0x0) {
                    throw new IllegalArgumentException("Tried to send a packet containing an internal payload!");
                }
                id = NibbleUtil.buildNibblePair(id, other);
            }
            buffer.writeByte(id);
        }
    }

    @Override
    void read(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ConnectionImpl connection,
              @AssumeNotNull ByteBuf buffer) throws CancelReadSignal {
        checkReadable(buffer, 4);

        final int packetId;
        try {
            packetId = InternalUtil.readInt(application, buffer);
        } catch (CancelReadSignal e) {
            throw new CancelReadSignal(3 + e.size);
        }
        checkReadable(buffer, 2 + 1);

        final short size = buffer.readShort();
        final boolean nibbleCompression = application.getCompressionSetting().isNibbleCompression();
        final int byteSize = nibbleCompression ? NibbleUtil.nibbleToByteCount(size) : size;
        checkReadable(buffer, byteSize);

        final DataType<?>[] types = new DataType[size];
        byte nibblePair = 0;
        for (int i = 0; i < size; i++) {
            final byte id;
            if (nibbleCompression) {
                if (nibblePair != 0) {
                    id = NibbleUtil.lowNibble(nibblePair);
                    nibblePair = 0;
                } else {
                    nibblePair = buffer.readByte();
                    id = NibbleUtil.highNibble(nibblePair);
                }
            } else {
                id = buffer.readByte();
            }
            if (id < 0x0) {
                final InvalidDataEvent event = new InvalidDataEvent(connection,
                        InvalidDataEvent.DataInvalidReason.INVALID_DATA_TYPE,
                        ConversionUtil.intToBytes(id));

                if (application.getEventManager().callEvent(ListenerType.INVALID_DATA, EventManager.CancelAction.IGNORE,
                        event)) {
                    return;
                }

                throw new IllegalStateException("The connection tried to register a packet containing an internal payload!");
            }
            final byte actualId = (byte) (id + 1);
            try {
                types[i] = DataType.fromId(actualId);
            } catch (IllegalArgumentException e) {
                final InvalidDataEvent event = new InvalidDataEvent(connection,
                        InvalidDataEvent.DataInvalidReason.INVALID_DATA_TYPE, new byte[]{actualId});
                if (application.getEventManager().callEvent(ListenerType.INVALID_DATA, EventManager.CancelAction.IGNORE,
                        event)) {
                    return;
                }
                throw e;
            }
        }

        final PacketCache cache = application.getCaching() != PacketCaching.INDIVIDUAL ? application.getCache() :
                connection.getCache();
        if (application instanceof Client) {
            cache.forceRegisterPacket(packetId, types);
            return;
        }

        final PacketSkeleton packet = cache.tryRegisterPacket(packetId, connection, types);
        if (packet.getId() == packetId) {
            if (application.getCaching() == PacketCaching.GLOBAL) {
                cache.broadcastRegister(application, new InternalPayloadRegisterPacket(packetId, types), connection,
                        null);
            }
        } else {
            // TODO: Check if we have to "fix" this (we probably have to).
            final InternalPayloadRegisterPacket register = new InternalPayloadRegisterPacket(packet.getId(), types);
            if (application.getCaching() == PacketCaching.GLOBAL) {
                cache.broadcastRegister(application, register, null, null);
            } else {
                final ByteBuf registerBuffer = Unpooled.buffer(
                        InternalUtil.getSingleByteSize(application) + 1 + getSize(application));
                DataType.getDTIP().write0(application, registerBuffer, register);
            }
        }
        packet.register();
    }

    public int getSize(@AssumeNotNull ApplicationImpl application) {
        final int size = 2 + InternalUtil.getSize(application, packetId);

        if (application.getCompressionSetting().isNibbleCompression()) {
            return size + NibbleUtil.nibbleToByteCount(types.length);
        }
        return size + types.length;
    }

}
