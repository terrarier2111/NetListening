package de.terrarier.netlistening.internals;

import de.terrarier.netlistening.api.type.DataType;
import de.terrarier.netlistening.network.PacketCache;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class InternalPayLoad_RegisterInPacket extends InternalPayload_RegisterPacket {

    public InternalPayLoad_RegisterInPacket(@NotNull DataType<?>... types) {
        super((byte) 0x1, types);
    }

    public InternalPayLoad_RegisterInPacket(int id, @NotNull DataType<?>... types) {
        super((byte) 0x1, id, types);
    }

    @Override
    protected void register0(@NotNull PacketCache cache, int packetId) {
        if (packetId == 0) {
            cache.registerInPacket(types);
        } else {
            cache.registerInPacket(packetId, types);
        }
    }

    @Override
    protected @NotNull InternalPayload_RegisterPacket getPayload(int packetId) {
        return new InternalPayLoad_RegisterOutPacket(packetId, types);
    }

}
