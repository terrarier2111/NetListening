package de.terrarier.netlistening.internals;

import de.terrarier.netlistening.api.type.DataType;
import de.terrarier.netlistening.network.PacketCache;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class InternalPayLoad_RegisterInPacket extends InternalPayload_RegisterPacket {
	
	public InternalPayLoad_RegisterInPacket(DataType<?>... types) {
		super((byte) 0x1, types);
	}
	
	public InternalPayLoad_RegisterInPacket(int id, DataType<?>... types) {
		super((byte) 0x1, id, types);
	}

	@Override
	protected void register0(PacketCache cache, int packetId) {
		if(packetId == 0) {
			cache.registerInPacket(types);
		} else {
			cache.registerInPacket(packetId, types);
		}
	}

	@Override
	protected InternalPayload getPayload(int packetId) {
		return new InternalPayLoad_RegisterOutPacket(packetId, types);
	}

}
