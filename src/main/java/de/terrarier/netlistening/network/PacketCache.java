package de.terrarier.netlistening.network;

import de.terrarier.netlistening.Connection;
import de.terrarier.netlistening.api.type.DataType;
import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.internals.InternalPayload_RegisterPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @since 1.0
 * @author Terrarier2111
 */
@ApiStatus.Internal
public final class PacketCache {

	private static final PacketSkeleton INTERNAL_PAYLOAD_PACKET_SKELETON = new PacketSkeleton(0x0, DataType.getDTIP());
	private static final PacketSkeleton ENCRYPTION_PACKET_SKELETON = new PacketSkeleton(0x3, DataType.getDTE());
	private static final PacketSkeleton HMAC_PACKET_SKELETON = new PacketSkeleton(0x4, DataType.getDTHMAC());
	private final Map<Integer, PacketSkeleton> packets = new ConcurrentHashMap<>();
	private final AtomicInteger id = new AtomicInteger(5);
	private final ReadWriteLock lock = new ReentrantReadWriteLock(true);
	
	public PacketCache() {
		packets.put(0x0, INTERNAL_PAYLOAD_PACKET_SKELETON);
		packets.put(0x3, ENCRYPTION_PACKET_SKELETON);
		packets.put(0x4, HMAC_PACKET_SKELETON);
	}

	@NotNull
	public Map<Integer, PacketSkeleton> getPackets() {
		return packets;
	}

	@NotNull
	protected PacketSkeleton registerPacket(@NotNull DataType<?>... data) {
		lock.writeLock().lock();
		try {
			return registerPacket0(id.getAndIncrement(), data);
		}finally {
			lock.writeLock().unlock();
		}
	}

	@NotNull
	public PacketSkeleton tryRegisterPacket(int id, @NotNull DataType<?>... data) {
		lock.writeLock().lock();
		try {
			boolean valid = id > this.id.get();
			if (valid) {
				this.id.set(id);
			}else {
				valid = packets.get(id) == null;
			}

			return registerPacket0(valid ? id : this.id.getAndIncrement(), data);
		}finally {
			lock.writeLock().unlock();
		}
	}

	public void forceRegisterPacket(int id, @NotNull DataType<?>... data) {
		lock.writeLock().lock();
		try {
			final int curr = this.id.get();
			if (id > curr) {
				this.id.set(id);
			}else if (id == curr) {
				this.id.getAndIncrement();
			}

			registerPacket0(id, data);
		}finally {
			lock.writeLock().unlock();
		}
	}

	@NotNull
	private PacketSkeleton registerPacket0(int id, @NotNull DataType<?>... data) {
		final PacketSkeleton packet = new PacketSkeleton(id, data);
		packets.put(id, packet);
		return packet;
	}

	protected PacketSkeleton getPacket(@NotNull DataType<?>... data) {
		final int dataLength = data.length;
		final int dataHash = Arrays.hashCode(data);

		lock.readLock().lock();
		try {
			for (PacketSkeleton packet : packets.values()) {
				if (packet.getData().length == dataLength && dataHash == packet.hashCode()) {
					return packet;
				}
			}
		}finally {
			lock.readLock().unlock();
		}
		return null;
	}

	protected PacketSkeleton getPacket(int id) {
		return packets.get(id);
	}

	public void broadcastRegister(@NotNull ApplicationImpl application, @NotNull InternalPayload_RegisterPacket payload, Channel ignored, ByteBuf buffer) {
		final Collection<Connection> connections = application.getConnections();
		if (ignored == null || connections.size() > 1) {
			final ByteBuf registerBuffer = buffer != null ? buffer : Unpooled.buffer(
					(application.getCompressionSetting().isVarIntCompression() ? 2 : 5) + payload.getSize(application));

			if(buffer == null)
				DataType.getDTIP().write0(application, registerBuffer, payload);

			for (Connection connection : connections) {
				if (ignored == null || !connection.getChannel().equals(ignored)) {
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

	public void clear() {
		packets.clear();
		// TODO: Check if we have to reset this to its default.
	}
	
}
