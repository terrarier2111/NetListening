package de.terrarier.netlistening.network;

import de.terrarier.netlistening.Connection;
import de.terrarier.netlistening.api.DataComponent;
import de.terrarier.netlistening.api.DataContainer;
import de.terrarier.netlistening.api.PacketCaching;
import de.terrarier.netlistening.api.encryption.EncryptionSetting;
import de.terrarier.netlistening.api.encryption.hash.HashUtil;
import de.terrarier.netlistening.api.encryption.hash.HmacSetting;
import de.terrarier.netlistening.api.encryption.hash.HmacUseCase;
import de.terrarier.netlistening.api.event.ExceptionTrowEvent;
import de.terrarier.netlistening.api.type.DataType;
import de.terrarier.netlistening.impl.ApplicationImpl;
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
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class PacketDataEncoder extends MessageToByteEncoder<DataContainer> {

	private final ApplicationImpl application;
	private final ExecutorService delayedExecutor;

	public PacketDataEncoder(@NotNull ApplicationImpl application, ExecutorService delayedExecutor) {
		this.application = application;
		this.delayedExecutor = delayedExecutor;
	}

	@Override
	protected void encode(@NotNull ChannelHandlerContext ctx, @NotNull DataContainer data, @NotNull ByteBuf buffer) {
		final List<DataComponent<?>> containedData = data.getData();
		final int dataSize = containedData.size();

		if (dataSize > 0) {
			final DataType<?>[] types = new DataType<?>[dataSize];
			for (int i = 0; i < dataSize; i++) {
				types[i] = containedData.get(i).getType();
			}

			final PacketCache cache = application.getCache();
			PacketSkeleton packet = cache.getOutPacket(types);
			if (packet == null) {
				// System.out.println("regchannel: " + ctx.channel());
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
							final Channel conChannel = connection.getChannel();
							if (!conChannel.equals(channel)) {
								registerBuffer.retain();
								if (connection.isConnected()) {
									conChannel.writeAndFlush(registerBuffer);
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

			if (!application.isClient() && !packet.isRegistered()) { // here occurs a race condition
				if(delayedExecutor.isShutdown()) {
					return;
				}
				final PacketSkeleton finalPacket = packet;
				// Sending data delayed, awaiting the packet's registration to finish
				final Channel channel = ctx.channel();
				delayedExecutor.execute(() -> {
					while (!finalPacket.isRegistered());
					channel.writeAndFlush(data);
				});
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

			final ByteBuf dst = hmac ? Unpooled.buffer() : buffer;
			writeToBuffer(dst, data, packet.getId());
			if(hmac) {
				appendHmac(dst, buffer, (ConnectionImpl) application.getConnection(ctx.channel()));
			}
		}
	}

	@SuppressWarnings("ForLoopReplaceableByForEach")
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
			application.getEventManager().handleExceptionThrown(new ExceptionTrowEvent(e));
		}
	}

}
