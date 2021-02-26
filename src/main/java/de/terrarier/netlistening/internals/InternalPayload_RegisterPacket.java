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
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public abstract class InternalPayload_RegisterPacket extends InternalPayload {

    protected DataType<?>[] types;
    private int packetId;

    protected InternalPayload_RegisterPacket(byte id, @NotNull DataType<?>... types) {
        super(id);
        this.types = types;
    }

    protected InternalPayload_RegisterPacket(byte id, int packetId, @NotNull DataType<?>... types) {
        this(id, types);
        this.packetId = packetId;
    }

    @Override
    protected final void write(@NotNull Application application, @NotNull ByteBuf buffer) {
        final int typesLength = types.length;

        if(typesLength == 0) {
            throw new IllegalStateException("Tried to send an empty packet!");
        }
        checkWriteable(application, buffer, getSize(application));

        if(packetId != 0x0) {
            InternalUtil.writeIntUnchecked(application, buffer, packetId);
        }
        buffer.writeShort(typesLength);

        final boolean nibbleCompression = application.getCompressionSetting().isNibbleCompression();
        for(int i = 0; i < typesLength; i++) {
            final byte id = (byte) (types[i].getId() - 1);
            if(id < 0x0) {
                throw new IllegalArgumentException("Tried to send a packet containing an internal payload!");
            }

            if(nibbleCompression && typesLength > ++i) {
                final byte other = (byte) (types[i].getId() - 1);
                if(other < 0x0) {
                    throw new IllegalArgumentException("Tried to send a packet containing an internal payload!");
                }
                buffer.writeByte(NibbleUtil.buildNibblePair(id, other));
            }else {
                buffer.writeByte(id);
            }
        }
    }

    @Override
    public final void read(@NotNull Application application, @NotNull Channel channel, @NotNull ByteBuf buffer)
            throws CancelReadingSignal {
        checkReadable(buffer, 4);
        int packetId = 0;
        final boolean useSimplePacketSync = application.getPacketSynchronization() == PacketSynchronization.SIMPLE;

        if(useSimplePacketSync) {
            try {
                packetId = InternalUtil.readInt(application, buffer);
            } catch (VarIntUtil.VarIntParseException e) {
                throw new CancelReadingSignal(3 + e.requiredBytes);
            }
        }

        checkReadable(buffer, 2);

        final short size = buffer.readShort();
        final boolean nibbleCompression = application.getCompressionSetting().isNibbleCompression();
        final int byteSize = nibbleCompression ? NibbleUtil.nibbleToByteCount(size) : size;
        checkReadable(buffer, byteSize);

        types = new DataType[size];
        byte nibblePair = 0;
        for(int i = 0; i < size; i++) {
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
                throw new IllegalStateException("The connection tried to register a packet containing an internal payload!");
            }
            types[i] = DataType.fromId((byte) (id + 1));
        }

        final PacketCache cache = application.getCaching() != PacketCaching.INDIVIDUAL ? application.getCache() :
                ((ConnectionImpl) application.getConnection(channel)).getCache();
        register0(cache, packetId);

        // TODO: Check for an empty buffer and setting as an initial buffer although the init phase is already over
        if (application.getCaching() == PacketCaching.GLOBAL) {
            final Collection<Connection> connections = application.getConnections();
            if (connections.size() > 1) {
                final InternalPayload_RegisterPacket payload = getPayload(packetId);
                final ByteBuf registerBuffer = Unpooled.buffer(
                        (application.getCompressionSetting().isVarIntCompression() ? 2 : 5) + payload.getSize(application));
                DataType.getDTIP().write0(application, registerBuffer, payload);

                for (Connection connection : connections) {
                    if (packetId != 0x0 || !connection.getChannel().equals(channel)) {
                        registerBuffer.retain();
                        if (connection.isConnected()) {
                            connection.getChannel().writeAndFlush(registerBuffer);
                        } else {
                            ((ConnectionImpl) connection).writeToInitialBuffer(registerBuffer);
                        }
                    }
                }
                registerBuffer.release();
            }
        }
    }

    private int getSize(@NotNull Application application) {
        int size = 2;
        if(packetId != 0x0) {
            size += InternalUtil.getSize(application, packetId);
        }
        if(application.getCompressionSetting().isNibbleCompression()) {
            size += NibbleUtil.nibbleToByteCount(types.length);
        }else {
            size += types.length;
        }
        return size;
    }

    protected abstract void register0(@NotNull PacketCache cache, int packetId);

    @NotNull
    protected abstract InternalPayload_RegisterPacket getPayload(int packetId);

}
