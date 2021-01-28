package de.terrarier.netlistening.impl;

import de.terrarier.netlistening.Application;
import de.terrarier.netlistening.Connection;
import de.terrarier.netlistening.api.DataComponent;
import de.terrarier.netlistening.api.DataContainer;
import de.terrarier.netlistening.api.PacketCaching;
import de.terrarier.netlistening.api.encryption.EncryptionOptions;
import de.terrarier.netlistening.api.encryption.SymmetricEncryptionContext;
import de.terrarier.netlistening.api.encryption.SymmetricEncryptionUtil;
import de.terrarier.netlistening.api.type.DataType;
import de.terrarier.netlistening.internals.*;
import de.terrarier.netlistening.network.PacketCache;
import de.terrarier.netlistening.network.PacketSkeleton;
import de.terrarier.netlistening.network.PacketSynchronization;
import de.terrarier.netlistening.utils.ByteBufUtilExtension;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;

import javax.crypto.SecretKey;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class ConnectionImpl implements Connection {
	
	private final Application application;
	private final Channel channel;
	private final int id;
	private final PacketCache cache;
	private volatile boolean receivedPacket;
	private volatile boolean sentCachedData;
	private volatile boolean finishedSendCachedData;
	private ByteBuf preConnectBuffer;
	private Queue<DataContainer> preConnectSendQueue;
	private ByteBuf finalBuffer;
	private SymmetricEncryptionContext encryptionContext;
	private byte[] hmacKey;
	
	public ConnectionImpl(@NotNull Application application, @NotNull Channel channel, int id) {
		this.application = application;
		this.channel = channel;
		this.id = id;
		if(application.getCaching() != PacketCaching.INDIVIDUAL) {
			cache = application.getCache();
		}else {
			cache = new PacketCache();
		}
	}

	/**
	 * @see Connection
	 */
	@Override
	public void sendData(@NotNull DataContainer data) {
		if(application.isClient()) {
			application.sendData(data);
			return;
		}
		final boolean connected = isConnected();
		checkReceived();
		if(connected && sentCachedData) {
			if(finishedSendCachedData) {
				channel.writeAndFlush(data);
			}else {
				// TODO: Handle stuff incoming before!
				// TODO: And check if this is really needed!
			}
		}else {
			if(preConnectSendQueue == null) {
				preConnectSendQueue = new ConcurrentLinkedQueue<>();
			}
			preConnectSendQueue.add(data);
		}
	}

	/**
	 * @see Connection
	 */
	@Deprecated
	@Override
	public void sendData(@NotNull DataComponent<?> data) {
		final DataContainer container = new DataContainer();
		container.addComponent(data);
		sendData(container);
	}
	
	private void checkReceived() {
		if (!receivedPacket) {
			receivedPacket = true;
			final boolean connected = isConnected();
			
			if(!connected && preConnectBuffer == null) {
				preConnectBuffer = Unpooled.buffer();
			}
			
			final ByteBuf buffer = connected ? Unpooled.buffer() : preConnectBuffer;
			buffer.writeInt(0x0);
			final DataType<InternalPayload> dtcp = DataType.getDTIP();
			((DataTypeInternalPayload) dtcp).write(application, buffer, InternalPayload.HANDSHAKE);
			if (application.getCaching() == PacketCaching.GLOBAL) {

				final Map<Integer, PacketSkeleton> outPackets = cache.getOutPackets();
				final Map<Integer, PacketSkeleton> inPackets = cache.getInPackets();
				final int outPacketsSize = outPackets.size();
				final int inPacketsSize = inPackets.size();
				if (outPacketsSize > 1 || inPacketsSize > 2) {
					final boolean simpleSynchronization = application.getPacketSynchronization() == PacketSynchronization.SIMPLE;

					if(outPacketsSize > 1) {
						for (int out = 5; out < outPacketsSize + 4; out++) {
							final DataType<?>[] data = outPackets.get(out).getData();
							dtcp.write0(application, buffer, simpleSynchronization ?
									new InternalPayLoad_RegisterInPacket(out, data) : new InternalPayLoad_RegisterInPacket(data));
						}
					}

					if(inPacketsSize > 2) {
						for (int in = 5; in < inPacketsSize + 3; in++) {
							final DataType<?>[] data = inPackets.get(in).getData();
							dtcp.write0(application, buffer, simpleSynchronization ?
									new InternalPayLoad_RegisterOutPacket(in, data) : new InternalPayLoad_RegisterOutPacket(data));
						}
					}
				}
			}
			if(connected) {
				channel.writeAndFlush(buffer);
			}
		}
	}

	/**
	 * @see Connection
	 */
	@Override
	public void disconnect() {
		application.disconnect(this);
	}
	
	protected void disconnect0() {
		if(application.getCaching() != PacketCaching.GLOBAL) {
			cache.clear();
		}
		channel.close();
	}

	/**
	 * @see Connection
	 */
	@Override
	public boolean isConnected() {
		return channel.isActive() || channel.isOpen();
	}

	@NotNull
	public Application getApplication() {
		return application;
	}

	/**
	 * @see Connection
	 */
	@NotNull
	@Override
	public Channel getChannel() {
		return channel;
	}

	public SymmetricEncryptionContext getEncryptionContext() {
		return encryptionContext;
	}

	/**
	 * Sets an internal symmetric key of the connection!
	 *
	 * @param options the options which should be used to interpret the key data.
	 * @param symmetricKey the data which should be used to generate the key.
	 */
	public void setSymmetricKey(@NotNull EncryptionOptions options, byte[] symmetricKey) {
		final SecretKey secretKey = SymmetricEncryptionUtil.readSecretKey(symmetricKey, options);
		encryptionContext = new SymmetricEncryptionContext(options, secretKey);
	}

	/**
	 * Sets an internal symmetric key of the connection!
	 *
	 * @param application the application to which this connection is referring to.
	 * @param secretKey the SecretKey which should be used to encrypt data.
	 */
	public void setSymmetricKey(@NotNull Application application, @NotNull SecretKey secretKey) {
		final EncryptionOptions options = application.getEncryptionSetting().getSymmetricSetting();
		encryptionContext = new SymmetricEncryptionContext(options, secretKey);
	}

	public byte[] getHmacKey() {
		return hmacKey;
	}

	/**
	 * Sets an internal symmetric key of the connection!
	 *
	 * @param key the key which should be used to hash data.
	 */
	public void setHmacKey(byte[] key) {
		hmacKey = key;
	}

	/**
	 * Sets an internal symmetric key of the connection!
	 *
	 * @param secretKey the SecretKey which should be used to generate hmacs for data.
	 */
	public void setHmacKey(@NotNull SecretKey secretKey) {
		setHmacKey(secretKey.getEncoded());
	}

	/**
	 * @see Connection
	 */
	@Override
	public int getId() {
		return id;
	}

	public boolean isStable() {
		return sentCachedData && receivedPacket;
	}

	@NotNull
	public PacketCache getCache() {
		return cache;
	}
	
	public void check() {
		if(!sentCachedData) {
			sentCachedData = true;

			if (preConnectBuffer != null && preConnectBuffer.writerIndex() > 0) {
				channel.writeAndFlush(preConnectBuffer);
			} else {
				checkReceived();
			}

			if(application.getEncryptionSetting() == null) {
				prepare();
			}
		}
	}
	
	public void writeToInitialBuffer(@NotNull ByteBuf buffer) {
		if (!sentCachedData) {
			final boolean receivedBefore = receivedPacket;
			checkReceived();
			if (receivedBefore) {
				final int readable = buffer.readableBytes();
				ByteBufUtilExtension.correctSize(preConnectBuffer, readable,
						application.getBuffer());
				preConnectBuffer.writeBytes(ByteBufUtilExtension.getBytes(buffer, readable));
			}
			buffer.release();
		} else {
			if(finishedSendCachedData) {
				channel.writeAndFlush(buffer);
			}else {
				// TODO: Test logic to send data delayed!
				if(finalBuffer == null) {
					finalBuffer = Unpooled.buffer();
				}
				final int readable = buffer.readableBytes();
				ByteBufUtilExtension.correctSize(finalBuffer, readable, application.getBuffer());
				finalBuffer.writeBytes(ByteBufUtilExtension.readBytes(buffer, readable));
			}
		}
	}

	public void prepare() {
		if(preConnectSendQueue != null) {
			for (DataContainer data : preConnectSendQueue) {
				channel.writeAndFlush(data);
			}
			preConnectSendQueue.clear();
		}

		final ByteBuf buffer = Unpooled.buffer(application.getCompressionSetting().isVarIntCompression() ? 1 : 4);
		InternalUtil.writeInt(application, buffer, 0x2);
		channel.writeAndFlush(buffer);
		finishedSendCachedData = true;
		if (finalBuffer != null) {
			channel.writeAndFlush(finalBuffer);
			finalBuffer = null;
		}
	}
	
}
