package de.terrarier.netlistening.internals;

import de.terrarier.netlistening.api.PacketCaching;
import de.terrarier.netlistening.api.compression.NibbleUtil;
import de.terrarier.netlistening.api.compression.VarIntUtil;
import de.terrarier.netlistening.api.type.DataType;
import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.network.PacketCache;
import de.terrarier.netlistening.network.PacketSkeleton;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@ApiStatus.Internal
public final class InternalPayloadRegisterPacket extends InternalPayload {

    private final int packetId;
    private DataType<?>[] types;

    public InternalPayloadRegisterPacket(int packetId, @NotNull DataType<?>... types) {
        super((byte) 0x1);
        this.packetId = packetId;
        this.types = types;
    }

    @Override
    void write(@NotNull ApplicationImpl application, @NotNull ByteBuf buffer) {
        final int typesLength = types.length;

        if(typesLength == 0) {
            throw new IllegalStateException("Tried to send an empty packet!");
        }
        checkWriteable(application, buffer, getSize(application));

        InternalUtil.writeIntUnchecked(application, buffer, packetId);
        buffer.writeShort(typesLength);

        final boolean nibbleCompression = application.getCompressionSetting().isNibbleCompression();
        for(int i = 0; i < typesLength; i++) {
            byte id = (byte) (types[i].getId() - 1);
            if(id < 0x0) {
                throw new IllegalArgumentException("Tried to send a packet containing an internal payload!");
            }

            if(nibbleCompression && typesLength > ++i) {
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
    public void read(@NotNull ApplicationImpl application, @NotNull Channel channel, @NotNull ByteBuf buffer)
            throws CancelReadingSignal {
        checkReadable(buffer, 4);

        final int packetId;
        try {
            packetId = InternalUtil.readInt(application, buffer);
        } catch (VarIntUtil.VarIntParseException e) {
            throw new CancelReadingSignal(3 + e.requiredBytes);
        }

        checkReadable(buffer, 2 + 1);

        final short size = buffer.readShort();
        final boolean nibbleCompression = application.getCompressionSetting().isNibbleCompression();
        final int byteSize = nibbleCompression ? NibbleUtil.nibbleToByteCount(size) : size;
        checkReadable(buffer, byteSize);

        types = new DataType[size];
        byte nibblePair = 0;
        for(int i = 0; i < size; i++) {
            final byte id;
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
        if (application.isClient()) {
            cache.forceRegisterPacket(packetId, types);
        } else {
            final PacketSkeleton packet = cache.tryRegisterPacket(packetId, types);
            if(packet.getId() == packetId) {
                if (application.getCaching() == PacketCaching.GLOBAL) {
                    cache.broadcastRegister(application, this, channel, null);
                }
            }else {
                final InternalPayloadRegisterPacket register = new InternalPayloadRegisterPacket(packet.getId());
                if (application.getCaching() == PacketCaching.GLOBAL) {
                    cache.broadcastRegister(application, register, null, null);
                }else {
                    final ByteBuf registerBuffer = Unpooled.buffer(
                            (application.getCompressionSetting().isVarIntCompression() ? 2 : 5) + getSize(application));
                    DataType.getDTIP().write0(application, registerBuffer, register);
                }
            }
            packet.register();
        }
    }

    public int getSize(@NotNull ApplicationImpl application) {
        int size = 2 + InternalUtil.getSize(application, packetId);

        if(application.getCompressionSetting().isNibbleCompression()) {
            size += NibbleUtil.nibbleToByteCount(types.length);
        }else {
            size += types.length;
        }
        return size;
    }

}