package de.terrarier.netlistening.network;

import de.terrarier.netlistening.Application;
import de.terrarier.netlistening.Connection;
import de.terrarier.netlistening.api.DataComponent;
import de.terrarier.netlistening.api.DataContainer;
import de.terrarier.netlistening.api.PacketCaching;
import de.terrarier.netlistening.api.encryption.hash.HashUtil;
import de.terrarier.netlistening.api.encryption.hash.HmacSetting;
import de.terrarier.netlistening.api.encryption.hash.HmacUseCase;
import de.terrarier.netlistening.api.type.DataType;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.internals.InternalPayLoad_RegisterInPacket;
import de.terrarier.netlistening.internals.InternalUtil;
import de.terrarier.netlistening.utils.ByteBufUtilExtension;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.jetbrains.annotations.NotNull;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class PacketDataEncoder extends MessageToByteEncoder<DataContainer> {

	private static final DataComponent<?>[] EMPTY_DATA_COMPONENTS = new DataComponent[0];
	private final Application application;
	private ExecutorService delayedExecutor;

	public PacketDataEncoder(@NotNull Application application) {
		this.application = application;
	}

	@Override
	protected void encode(@NotNull ChannelHandlerContext ctx, @NotNull DataContainer data, @NotNull ByteBuf buffer) {
		final List<DataComponent<?>> containedData = data.getData();

		if (containedData.size() > 0) {
			final DataComponent<?>[] rawData = containedData.toArray(EMPTY_DATA_COMPONENTS);
			final int rawDataLength = rawData.length;
			final DataType<?>[] types = new DataType<?>[rawDataLength];
			for (int index = 0; index < rawDataLength; index++) {
				types[index] = rawData[index].getType();
			}
			final PacketCache cache = application.getCache();
			PacketSkeleton packet = cache.getOutPacket(types);
			if (packet == null) {
				packet = cache.registerOutPacket(types);
				final InternalPayLoad_RegisterInPacket register = new InternalPayLoad_RegisterInPacket(
						application.getPacketSynchronization() == PacketSynchronization.SIMPLE ? packet.getId() : 0, types);
				final ByteBuf registerBuffer = Unpooled.buffer(5 + rawDataLength);
				DataType.getDTCP().write0(application, registerBuffer, register);
				buffer.writeBytes(ByteBufUtilExtension.getBytes(registerBuffer));
				if (application.getCaching() == PacketCaching.GLOBAL) {
					final Set<Connection> connections = application.getConnections();
					if (connections.size() > 1) {
						final Channel channel = ctx.channel();
						for (Connection connection : connections) {
							if (!connection.getChannel().equals(channel)) {
								registerBuffer.retain();
								if (connection.isConnected()) {
									connection.getChannel().writeAndFlush(registerBuffer);
								} else {
									((ConnectionImpl) connection).addInitialBuffer(registerBuffer);
								}
							}
						}
					}
				}
				registerBuffer.release();
				packet.register();
			}

			if (!packet.isRegistered()) {
				final Channel channel = ctx.channel();
				if(delayedExecutor == null) {
					delayedExecutor = Executors.newSingleThreadExecutor();
				}
				// Sending data delayed, awaiting the packet's registration to finish
				while (!packet.isRegistered());
				delayedExecutor.execute(() -> channel.writeAndFlush(data));
				return;
			}

			final HmacSetting hmacSetting = application.getEncryptionSetting().getHmacSetting();
			boolean hmac = hmacSetting != null;
			if (data.isEncrypted()) {
				final ByteBuf target = hmac ? Unpooled.buffer() : buffer;

				InternalUtil.writeInt(application, target, 0x3);
				final ByteBuf tmpBuffer = Unpooled.buffer();
				writeToBuffer(tmpBuffer, data, packet.getId());
				final ConnectionImpl connection = (ConnectionImpl) application.getConnection(ctx.channel());
				final byte[] encryptedData = connection.getEncryptionContext().encrypt(ByteBufUtilExtension.getBytes(tmpBuffer));
				tmpBuffer.release();
				final int size = encryptedData.length;
				ByteBufUtilExtension.correctSize(target, size + 4, application.getBuffer());
				target.writeInt(size);
				target.writeBytes(encryptedData);

				if(hmac) {
					appendHmac(target, buffer, application, connection);
				}
				return;
			}
			if(hmac) {
				hmac = hmacSetting.getUseCase() == HmacUseCase.ALL;
			}

			final ByteBuf target = hmac ? Unpooled.buffer() : buffer;
			writeToBuffer(target, data, packet.getId());
			if(hmac) {
				appendHmac(target, buffer, application, (ConnectionImpl) application.getConnection(ctx.channel()));
			}
		}
	}

	private void writeToBuffer(@NotNull ByteBuf buffer, @NotNull DataContainer data, int packetId) {
		InternalUtil.writeInt(application, buffer, packetId);
		for (DataComponent<?> component : data.getData()) {
			component.getType().writeUnchecked(application, buffer, component.getData());
		}
	}

	private void appendHmac(@NotNull ByteBuf src, @NotNull ByteBuf dst, @NotNull Application application, @NotNull ConnectionImpl connection) {
		final byte[] data = ByteBufUtilExtension.getBytes(src);
		src.release();
		try {
			final byte[] hash = HashUtil.calculateHMAC(data, connection.getHmacKey(), application.getEncryptionSetting().getHmacSetting().getHashingAlgorithm());
			final int buffer = application.getBuffer();
			InternalUtil.writeInt(application, dst, 0x4);
			ByteBufUtilExtension.correctSize(dst, 6, buffer);
			final int dataLength = data.length;
			final short hashLength = (short) hash.length;
			dst.writeInt(dataLength);
			dst.writeShort(hashLength);
			ByteBufUtilExtension.correctSize(dst, dataLength, buffer);
			dst.writeBytes(data);
			ByteBufUtilExtension.correctSize(dst, hashLength, buffer);
			dst.writeBytes(hash);
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			e.printStackTrace(); // TODO: Handle this better!
		}
	}

}
