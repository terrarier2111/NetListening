package de.terrarier.netlistening.internals;

import de.terrarier.netlistening.api.type.DataType;
import de.terrarier.netlistening.network.PacketCache;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class InternalPayLoad_RegisterOutPacket extends InternalPayload_RegisterPacket {
	
	public InternalPayLoad_RegisterOutPacket(DataType<?>... types) {
		super((byte) 0x2, types);
	}
	
	public InternalPayLoad_RegisterOutPacket(int id, DataType<?>... types) {
		super((byte) 0x2, id, types);
	}

	@Override
	protected void register0(PacketCache cache, int packetId) {
		if(packetId == 0) {
			cache.registerOutPacket(types).register();
		}else {
			cache.registerOutPacket(packetId, types).register();
		}
	}

	@Override
	protected InternalPayload getPayload(int packetId) {
		return new InternalPayLoad_RegisterInPacket(packetId, types);
	}

}
