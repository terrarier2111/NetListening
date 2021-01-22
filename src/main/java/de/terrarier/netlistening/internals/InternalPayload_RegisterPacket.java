package de.terrarier.netlistening.internals;

import de.terrarier.netlistening.Application;
import de.terrarier.netlistening.Connection;
import de.terrarier.netlistening.api.PacketCaching;
import de.terrarier.netlistening.api.compression.NibbleUtil;
import de.terrarier.netlistening.api.compression.VarIntUtil;
import de.terrarier.netlistening.api.type.DataType;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.network.PacketCache;
import de.terrarier.netlistening.network.PacketSynchronization;
import de.terrarier.netlistening.utils.ArrayUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public abstract class InternalPayload_RegisterPacket extends InternalPayload {

    protected DataType<?>[] types;
    private int packetId;

    protected InternalPayload_RegisterPacket(byte id, DataType<?>... types) {
        super(id);
        this.types = types;
    }

    protected InternalPayload_RegisterPacket(byte id, int packetId, DataType<?>... types) {
        super(id);
        this.packetId = packetId;
        this.types = types;
    }

    @Override
    protected final void write(@NotNull Application application, @NotNull ByteBuf buffer) {
        int reduction = 0;
        for(DataType<?> type : types) {
            if(type.getId() == 0x0) {
                reduction++;
            }
        }
        final int size = types.length - reduction;

        if(size == 0) {
            throw new IllegalStateException("Detected a corrupted packet!"); // TODO: Call invalid data event.
            // separate messages:
            // Tried to send a corrupted packet!
            // Received a corrupted packet!
        }

        final boolean nibbleCompression = application.getCompressionSetting().isNibbleCompression();
        final int byteSize = nibbleCompression ? NibbleUtil.nibbleToByteSize(size) : size;

        final boolean simplePacketSync = packetId != 0x0;
        final int additionalBufferSpace = simplePacketSync ? InternalUtil.getSize(application, packetId) : 0;

        checkWriteable(application, buffer, byteSize + 2 + additionalBufferSpace);

        if(simplePacketSync) {
            InternalUtil.writeInt(application, buffer, packetId);
        }

        buffer.writeShort(size);

        final int increment = nibbleCompression ? 2 : 1;
        for(int i = 0; i < types.length; i += increment) {
            final byte id = (byte) (types[i].getId() - 1);
            if(id == -1) {
                i--;
                continue;
            }

            if(nibbleCompression) {
                if(types.length > i + 1) {
                    final byte other = (byte) (types[i + 1].getId() - 1);
                    if(other == -1) {
                        buffer.writeByte(id);
                        continue;
                    }
                    buffer.writeByte(NibbleUtil.buildByte(id, other));
                }else {
                    buffer.writeByte(id);
                }
            }else {
                buffer.writeByte(id);
            }
        }
    }

    @Override
    public final void read(@NotNull Application application, @NotNull Channel channel, @NotNull ByteBuf buffer) throws CancelReadingSignal {
        checkReadable(buffer, 0, 4);
        int packetId = 0;
        int idSize = 0;
        final boolean useSimplePacketSync = application.getPacketSynchronization() == PacketSynchronization.SIMPLE;

        if(useSimplePacketSync) {
            try {
                packetId = InternalUtil.readInt(application, buffer);
            } catch (VarIntUtil.VarIntParseException e) {
                throw new CancelReadingSignal(3 + e.requiredBytes);
            }
            idSize = InternalUtil.getSize(application, packetId);
        }

        checkReadable(buffer, idSize, 2);

        final short size = buffer.readShort();
        final boolean nibbleCompression = application.getCompressionSetting().isNibbleCompression();
        final int byteSize = nibbleCompression ? NibbleUtil.nibbleToByteSize(size) : size;

        checkReadable(buffer, 2 + idSize, byteSize);

        types = new DataType[size];

        int reduction = 0;
        byte nibblePair = 0;
        for(int i = 0; i < size - reduction; i++) {
            byte id;
            if(nibbleCompression) {
                if(nibblePair != 0) {
                    id = NibbleUtil.getSecondNibble(nibblePair);
                    nibblePair = 0;
                }else {
                    nibblePair = buffer.readByte();
                    id = NibbleUtil.getFirstNibble(nibblePair);
                }
            }else {
                id = buffer.readByte();
            }
            if(id < 0x0) {
                i--;
                reduction++;
                continue;
            }
            types[i] = DataType.fromId((byte) (id + 1));
        }

        if(reduction > 0) {
            types = ArrayUtil.reduceSize(types, reduction);
        }
        register0(((ConnectionImpl) application.getConnection(channel)).getCache(), packetId);

        // TODO: Check for an empty buffer and setting as an initial buffer although the init phase is already over
        if (application.getCaching() == PacketCaching.GLOBAL) {
            final Set<Connection> connections = application.getConnections();
            if (connections.size() > 1) {
                final ByteBuf registerBuffer = Unpooled.buffer();
                DataType.getDTCP().write0(application, registerBuffer, getPayload(packetId));

                for (Connection connection : connections) {
                    if (packetId != 0x0 || !connection.getChannel().equals(channel)) {
                        registerBuffer.retain();
                        if (connection.isConnected()) {
                            connection.getChannel().writeAndFlush(registerBuffer);
                        } else {
                            ((ConnectionImpl) connection).addInitialBuffer(registerBuffer);
                        }
                    }
                }
                registerBuffer.release();
            }
        }
    }

    protected abstract void register0(PacketCache cache, int packetId);

    protected abstract InternalPayload getPayload(int packetId);

}
