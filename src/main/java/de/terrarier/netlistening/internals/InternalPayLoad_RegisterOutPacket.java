package de.terrarier.netlistening.internals;

import de.terrarier.netlistening.api.type.DataType;
import de.terrarier.netlistening.network.PacketCache;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class InternalPayLoad_RegisterOutPacket extends InternalPayload_RegisterPacket {

    public InternalPayLoad_RegisterOutPacket(@NotNull DataType<?>... types) {
        super((byte) 0x2, types);
    }

    public InternalPayLoad_RegisterOutPacket(int id, @NotNull DataType<?>... types) {
        super((byte) 0x2, id, types);
    }

    @Override
    protected void register0(@NotNull PacketCache cache, int packetId) {
        if (packetId == 0) {
            cache.registerOutPacket(types).register();
        } else {
            cache.registerOutPacket(packetId, types).register();
        }
    }

    @Override
    protected @NotNull InternalPayload_RegisterPacket getPayload(int packetId) {
        return new InternalPayLoad_RegisterInPacket(packetId, types);
    }

}
