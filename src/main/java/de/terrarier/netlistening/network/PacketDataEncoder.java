package de.terrarier.netlistening.network;

import de.terrarier.netlistening.Application;
import de.terrarier.netlistening.Connection;
import de.terrarier.netlistening.api.DataComponent;
import de.terrarier.netlistening.api.DataContainer;
import de.terrarier.netlistening.api.PacketCaching;
import de.terrarier.netlistening.api.encryption.EncryptionSetting;
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
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.MessageToByteEncoder;
import org.jetbrains.annotations.NotNull;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
		final int dataSize = containedData.size();

		if (dataSize > 0) {
			final DataComponent<?>[] rawData = containedData.toArray(EMPTY_DATA_COMPONENTS); // TODO: Check if directly accessing the list would be faster
			final DataType<?>[] types = new DataType<?>[dataSize];
			for (int index = 0; index < dataSize; index++) {
				types[index] = rawData[index].getType();
			}

			final PacketCache cache = application.getCache();
			PacketSkeleton packet = cache.getOutPacket(types);
			if (packet == null) {
				packet = cache.registerOutPacket(types);
				final InternalPayLoad_RegisterInPacket register = new InternalPayLoad_RegisterInPacket(
						application.getPacketSynchronization() == PacketSynchronization.SIMPLE ? packet.getId() : 0, types);
				final ByteBuf registerBuffer = Unpooled.buffer(5 + dataSize);
				DataType.getDTIP().write0(application, registerBuffer, register);
				buffer.writeBytes(ByteBufUtilExtension.getBytes(registerBuffer));
				if (application.getCaching() == PacketCaching.GLOBAL) {
					final Collection<Connection> connections = application.getConnections();
					if (connections.size() > 1) {
						final Channel channel = ctx.channel();
						for (Connection connection : connections) {
							if (!connection.getChannel().equals(channel)) {
								registerBuffer.retain();
								if (connection.isConnected()) {
									connection.getChannel().writeAndFlush(registerBuffer);
								} else {
									((ConnectionImpl) connection).writeToInitialBuffer(registerBuffer);
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
				if(delayedExecutor == null || delayedExecutor.isShutdown()) {
					return;
				}
				delayedExecutor.execute(() -> channel.writeAndFlush(data));
				return;
			}

			final EncryptionSetting encryptionSetting = application.getEncryptionSetting();
			final HmacSetting hmacSetting = encryptionSetting == null ? null : encryptionSetting.getHmacSetting();
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
					appendHmac(target, buffer, connection);
				}
				return;
			}
			if(hmac) {
				hmac = hmacSetting.getUseCase() == HmacUseCase.ALL;
			}

			final ByteBuf target = hmac ? Unpooled.buffer() : buffer;
			writeToBuffer(target, data, packet.getId());
			if(hmac) {
				appendHmac(target, buffer, (ConnectionImpl) application.getConnection(ctx.channel()));
			}
		}
	}

	private void writeToBuffer(@NotNull ByteBuf buffer, @NotNull DataContainer data, int packetId) {
		InternalUtil.writeInt(application, buffer, packetId);
		final List<DataComponent<?>> dataComponentList = data.getData();
		final int dataSize = dataComponentList.size();
		for (int i = 0; i < dataSize; i++) {
			final DataComponent<?> component = dataComponentList.get(i);
			component.getType().writeUnchecked(application, buffer, component.getData());
		}
	}

	private void appendHmac(@NotNull ByteBuf src, @NotNull ByteBuf dst, @NotNull ConnectionImpl connection) {
		final byte[] data = ByteBufUtilExtension.getBytes(src);
		src.release();
		try {
			final byte[] hash = HashUtil.calculateHMAC(data, connection.getHmacKey(),
					application.getEncryptionSetting().getHmacSetting().getHashingAlgorithm());
			final int buffer = application.getBuffer();
			final int dataLength = data.length;
			final short hashLength = (short) hash.length;
			InternalUtil.writeInt(application, dst, 0x4);
			ByteBufUtilExtension.correctSize(dst, 6 + dataLength + hashLength, buffer);
			dst.writeInt(dataLength);
			dst.writeShort(hashLength);
			dst.writeBytes(data);
			dst.writeBytes(hash);
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			e.printStackTrace(); // TODO: Handle this better!
		}
	}

	@Override
	public void handlerRemoved(@NotNull ChannelHandlerContext ctx) throws Exception {
		shutdown();
		super.handlerRemoved(ctx);
	}

	@Override
	public void close(@NotNull ChannelHandlerContext ctx, @NotNull ChannelPromise promise) throws Exception {
		shutdown();
		super.close(ctx, promise);
	}

	private void shutdown() {
		if(delayedExecutor != null && !delayedExecutor.isShutdown()) {
			delayedExecutor.shutdown();
			try {
				if (!delayedExecutor.awaitTermination(250, TimeUnit.MILLISECONDS)) {
					delayedExecutor.shutdownNow();
				}
			} catch (InterruptedException e) {
				delayedExecutor.shutdownNow();
			}
			delayedExecutor = null;
		}
	}

}
