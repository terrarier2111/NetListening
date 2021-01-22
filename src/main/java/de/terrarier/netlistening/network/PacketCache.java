package de.terrarier.netlistening.network;

import de.terrarier.netlistening.api.type.DataType;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class PacketCache {

	private static final PacketSkeleton INTERNAL_PAYLOAD_PACKET_SKELETON = new PacketSkeleton(0x0, DataType.getDTIP());
	private static final PacketSkeleton ENCRYPTION_PACKET_SKELETON = new PacketSkeleton(0x3, DataType.getDTE());
	private static final PacketSkeleton HMAC_PACKET_SKELETON = new PacketSkeleton(0x4, DataType.getDTHMAC());
	private final Map<Integer, PacketSkeleton> inPackets = new ConcurrentHashMap<>();
	private final Map<Integer, PacketSkeleton> outPackets = new ConcurrentHashMap<>();
	private final AtomicInteger inId = new AtomicInteger(5);
	private final AtomicInteger outId = new AtomicInteger(5);
	
	public PacketCache() {
		outPackets.put(0x0, INTERNAL_PAYLOAD_PACKET_SKELETON);
		inPackets.put(0x3, ENCRYPTION_PACKET_SKELETON);
		inPackets.put(0x4, HMAC_PACKET_SKELETON);
	}

	@NotNull
	public Map<Integer, PacketSkeleton> getOutPackets() {
		return outPackets;
	}

	@NotNull
	public Map<Integer, PacketSkeleton> getInPackets() {
		return inPackets;
	}
	
	public void registerInPacket(@NotNull DataType<?>... data) {
		registerInPacket(inId.getAndIncrement(), data);
	}

	@NotNull
	public PacketSkeleton registerOutPacket(@NotNull DataType<?>... data) {
		return registerOutPacket(outId.getAndIncrement(), data);
	}
	
	public void registerInPacket(int id, @NotNull DataType<?>... data) {
		if(id > inId.get()) {
			inId.set(id);
		}

		inPackets.put(id, new PacketSkeleton(id, data));
	}

	@NotNull
	public PacketSkeleton registerOutPacket(int id, @NotNull DataType<?>... data) {
		if(id > outId.get()) {
			outId.set(id);
		}

		final PacketSkeleton packet = new PacketSkeleton(id, data);
		outPackets.put(id, packet);
		return packet;
	}

	protected PacketSkeleton getOutPacket(@NotNull DataType<?>... data) {
		final int dataLength = data.length;
		for(PacketSkeleton packet : outPackets.values()) {
			final DataType<?>[] tmp = packet.getData();

			if(tmp.length == dataLength) {
				boolean invalid = false;
				for(int i = 0; i < dataLength; i++) {
					if(!data[i].equals(tmp[i])) {
						invalid = true;
						break;
					}
				}
				if(!invalid) {
					return packet;
				}
			}
		}
		return null;
	}

	protected PacketSkeleton getInPacketFromId(int id) {
		return inPackets.get(id);
	}

	public void clear() {
		outPackets.clear();
		inPackets.clear();
		// TODO: Check if we have to reset this to its default.
	}
	
}
