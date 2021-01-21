package de.terrarier.netlistening.internals;

import de.terrarier.netlistening.Application;
import de.terrarier.netlistening.Connection;
import de.terrarier.netlistening.api.type.DataType;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.network.PacketSynchronization;
import de.terrarier.netlistening.api.PacketCaching;
import de.terrarier.netlistening.utils.VarIntUtil.VarIntParseException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class InternalPayLoad_RegisterInPacket extends InternalPayload {
	
	private DataType<?>[] types;
	private int id;
	
	public InternalPayLoad_RegisterInPacket(DataType<?>... types) {
		super((byte) 0x1);
		this.types = types;
	}
	
	public InternalPayLoad_RegisterInPacket(int id, DataType<?>... types) {
		super((byte) 0x1);
		if(id != 0) {
			this.id = id;
		}
		this.types = types;
	}

	@Override
	protected void write(@NotNull Application application, @NotNull ByteBuf buffer) {
		int reduce = 0;
		for(DataType<?> type : types) {
			if(type.getId() == 0x0) {
				reduce++;
			}
		}
		final int size = types.length - reduce;

		if(size == 0) {
			throw new IllegalStateException("Received a corrupted packet!");
		}
		
		final boolean simplePacketSync = id != 0x0;
		final int additionalBufferSpace = simplePacketSync ? InternalUtil.getSize(application, id) : 0;
		
		checkWriteable(application, buffer, size + 2 + additionalBufferSpace);
		
		if(simplePacketSync) {
			InternalUtil.writeInt(application, buffer, id);
		}
		
		buffer.writeShort(size);
		
		for(DataType<?> type : types) {
			final byte id = type.getId();
			if(id == 0x0) {
				continue;
			}
			
			buffer.writeByte(id);
		}
	}

	@Override
	public void read(@NotNull Application application, @NotNull Channel channel, @NotNull ByteBuf buffer) throws CancelReadingSignal {
		checkReadable(buffer, 0, 4);
		int packetId = 0;
		int idSize = 0;

		if(application.getPacketSynchronization() == PacketSynchronization.SIMPLE) {
			try {
				packetId = InternalUtil.readInt(application, buffer);
			} catch (VarIntParseException e) {
				throw new CancelReadingSignal(3 + e.requiredBytes);
			}
			idSize = InternalUtil.getSize(application, packetId);
		}
		
		checkReadable(buffer, idSize, 2);
		
		final short length = buffer.readShort();
		
		checkReadable(buffer, 2 + idSize, length);
		
		types = new DataType[length];
		
		int reduction = 0;
		for(int i = 0; i < length - reduction; i++) {
			final byte id = buffer.readByte();
			if(id < 0x1) {
				i--;
				reduction++;
				continue;
			}
			types[i] = DataType.fromId(id);
		}
		
		if(packetId == 0) {
			((ConnectionImpl) application.getConnection(channel)).getCache().registerInPacket(types);
		} else {
			((ConnectionImpl) application.getConnection(channel)).getCache().registerInPacket(packetId, types);
		}

		if (application.getCaching() == PacketCaching.GLOBAL) {
			final Set<Connection> connections = application.getConnections();
			if (connections.size() > 1) {
				final ByteBuf registerBuffer = Unpooled.buffer();
				DataType.getDTCP().write0(application, registerBuffer, new InternalPayLoad_RegisterOutPacket(packetId, types));

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

}
