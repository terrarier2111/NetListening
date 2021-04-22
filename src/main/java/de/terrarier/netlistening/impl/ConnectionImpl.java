package de.terrarier.netlistening.impl;

import de.terrarier.netlistening.Application;
import de.terrarier.netlistening.Client;
import de.terrarier.netlistening.Connection;
import de.terrarier.netlistening.Server;
import de.terrarier.netlistening.api.DataContainer;
import de.terrarier.netlistening.api.PacketCaching;
import de.terrarier.netlistening.api.encryption.EncryptionOptions;
import de.terrarier.netlistening.api.encryption.SymmetricEncryptionContext;
import de.terrarier.netlistening.api.encryption.SymmetricEncryptionUtil;
import de.terrarier.netlistening.api.type.DataType;
import de.terrarier.netlistening.internals.DataTypeInternalPayload;
import de.terrarier.netlistening.internals.InternalPayload;
import de.terrarier.netlistening.internals.InternalPayloadRegisterPacket;
import de.terrarier.netlistening.internals.InternalUtil;
import de.terrarier.netlistening.network.PacketCache;
import de.terrarier.netlistening.network.PacketSkeleton;
import de.terrarier.netlistening.utils.ByteBufUtilExtension;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import javax.crypto.SecretKey;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class ConnectionImpl implements Connection {

	static final AtomicInteger ID = new AtomicInteger();
	private final ApplicationImpl application;
	private final Channel channel;
	private final int id = ID.getAndIncrement();
	private final PacketCache cache;
	private volatile boolean receivedPacket;
	private volatile DataSendState dataSendState = DataSendState.IDLE;
	private ByteBuf preConnectBuffer;
	private List<DataContainer> preConnectSendQueue;
	private SymmetricEncryptionContext encryptionContext;
	private byte[] hmacKey;
	// TODO: Improve and test delayed data sending mechanics.

	@ApiStatus.Internal
	ConnectionImpl(@NotNull ApplicationImpl application, @NotNull Channel channel) {
		this.application = application;
		this.channel = channel;
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
		if(application instanceof Client) {
			application.sendData(data);
			return;
		}
		final boolean connected = isConnected();
		checkReceived();
		if(connected && dataSendState.isAtLeast(DataSendState.SENDING)) {
			if(dataSendState == DataSendState.FINISHED) {
				channel.writeAndFlush(data);
			}else {
				// TODO: Handle stuff incoming before!
			}
		}else {
			synchronized (this) {
				if (preConnectSendQueue == null) {
					preConnectSendQueue = new ArrayList<>();
				}

				preConnectSendQueue.add(data);
			}
		}
	}

	/**
	 * @see Connection
	 */
	@Override
	public void sendData(boolean encrypted, @NotNull Object... data) {
		final DataContainer dataContainer = new DataContainer();
		dataContainer.setEncrypted(encrypted);
		dataContainer.addAll(data);
		sendData(dataContainer);
	}

	/**
	 * @see Connection
	 */
	@Override
	public void disconnect() {
		if (!isConnected()) {
			throw new IllegalStateException(
					"The connection " + Integer.toHexString(id) + " is not connected!");
		}
		if(application instanceof Server) {
			((ServerImpl) application).disconnect0(this);
		}
		disconnect0();
	}

	void disconnect0() {
		if(application.getCaching() != PacketCaching.GLOBAL) {
			cache.getPackets().clear();
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
	@Deprecated
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

	/**
	 * @see Connection
	 */
	@NotNull
	@Override
	public InetSocketAddress getRemoteAddress() {
		return (InetSocketAddress) channel.remoteAddress();
	}

	// TODO: Add any sort of documentation to this method!
	public SymmetricEncryptionContext getEncryptionContext() {
		return encryptionContext;
	}

	/**
	 * Defines the encryption context for this connection which determines
	 * how data sent to/received from this connection gets en-/decrypted.
	 *
	 * @param options the options which should be used to interpret the key data.
	 * @param symmetricKey the data which should be used to generate the key.
	 */
	public void setSymmetricKey(@NotNull EncryptionOptions options, byte[] symmetricKey) {
		final SecretKey secretKey = SymmetricEncryptionUtil.readSecretKey(symmetricKey, options);
		encryptionContext = new SymmetricEncryptionContext(options, secretKey);
	}

	/**
	 * Defines the encryption context for this connection which determines
	 * how data sent to/received from this connection gets en-/decrypted.
	 *
	 * @param application the application to which this connection is related to.
	 * @param secretKey the secret key which should be used to encrypt data.
	 */
	public void setSymmetricKey(@NotNull ApplicationImpl application, @NotNull SecretKey secretKey) {
		final EncryptionOptions options = application.getEncryptionSetting().getSymmetricSetting();
		encryptionContext = new SymmetricEncryptionContext(options, secretKey);
	}

	// TODO: Add any sort of documentation to this method!
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

	@ApiStatus.Internal
	public boolean isStable() {
		return dataSendState.isAtLeast(DataSendState.SENDING) && receivedPacket;
	}

	@ApiStatus.Internal
	@NotNull
	public PacketCache getCache() {
		return cache;
	}

	private void checkReceived() {
		if (!receivedPacket) {
			receivedPacket = true;
			final boolean connected = isConnected();

			synchronized (this) {
				if (!connected && preConnectBuffer == null) {
					preConnectBuffer = Unpooled.buffer();
				}

				final ByteBuf buffer = connected ? Unpooled.buffer() : preConnectBuffer;
				buffer.writeInt(0x0);
				final DataTypeInternalPayload dtip = DataType.getDTIP();
				dtip.write(application, buffer, InternalPayload.HANDSHAKE);
				if (application.getCaching() == PacketCaching.GLOBAL) {

					final Map<Integer, PacketSkeleton> packets = cache.getPackets();
					final int packetsSize = packets.size();
					if (packetsSize > 3) {

						for (int id = 5; id < packetsSize + 2; id++) {
							final DataType<?>[] data = packets.get(id).getData();
							dtip.write0(application, buffer, new InternalPayloadRegisterPacket(id, data));
						}
					}
				}
				if (connected) {
					channel.writeAndFlush(buffer);
				}
			}
		}
	}

	@ApiStatus.Internal
	public void check() {
		if(!dataSendState.isAtLeast(DataSendState.SENDING)) {
			dataSendState = DataSendState.SENDING;

			synchronized (this) {
				if (preConnectBuffer != null && preConnectBuffer.writerIndex() > 0) {
					channel.writeAndFlush(preConnectBuffer);
					preConnectBuffer = null;
				} else {
					// writing the init data to the channel (without hitting the pre connect buffer).
					checkReceived();
				}
			}

			if(application.getEncryptionSetting() == null) {
				prepare();
			}else {
				dataSendState = DataSendState.WAITING_FOR_FINISH;
			}
		}
	}

	@ApiStatus.Internal
	public void writeToInitialBuffer(@NotNull ByteBuf buffer) {
		final DataSendState dataSendState = this.dataSendState; // caching volatile field get result
		if (!dataSendState.isAtLeast(DataSendState.SENDING)) {
			checkReceived();
			transferData(buffer);
		} else {
			if(!trySend(buffer)) {
				// TODO: Test logic to send data delayed!
				if(dataSendState != DataSendState.WAITING_FOR_FINISH) { // check if the connection isn't in the waiting state and therefore
																		// currently it isn't waiting for a response from the other end of the connection.
					while(true) {
						if(this.dataSendState == DataSendState.WAITING_FOR_FINISH) {
							break;
						}else if(trySend(buffer)) {
							return;
						}
					}
				}
				synchronized (this) {
					if (preConnectBuffer == null) {
						preConnectBuffer = Unpooled.buffer();
					}
				}
				transferData(buffer);
			}
		}
	}

	private boolean trySend(@NotNull ByteBuf buffer) {
		final DataSendState dataSendState = this.dataSendState; // caching volatile field get result
		if(dataSendState.isAtLeast(DataSendState.FINISHING)) {
			if(dataSendState == DataSendState.FINISHING) {
				// we are waiting until the execution of the prepare method has finished.
				while(this.dataSendState != DataSendState.FINISHED);
			}
			channel.writeAndFlush(buffer);
			return true;
		}
		return false;
	}

	private void transferData(@NotNull ByteBuf buffer) {
		final int readable = buffer.readableBytes();
		synchronized (this) {
			ByteBufUtilExtension.correctSize(preConnectBuffer, readable, application.getBuffer());
			preConnectBuffer.writeBytes(ByteBufUtilExtension.getBytes(buffer, readable));
		}
		buffer.release();
	}

	@ApiStatus.Internal
	public void prepare() {
		dataSendState = DataSendState.FINISHING;
		synchronized (this) {
			if (preConnectSendQueue != null) {
				final List<DataContainer> sendQueue = preConnectSendQueue;
				preConnectSendQueue = null;
				for (DataContainer data : sendQueue) {
					channel.writeAndFlush(data);
				}
				sendQueue.clear();
			}
		}

		final ByteBuf buffer = Unpooled.buffer(application.getCompressionSetting().isVarIntCompression() ? 1 : 4);
		InternalUtil.writeIntUnchecked(application, buffer, 0x2);
		channel.writeAndFlush(buffer);
		synchronized (this) {
			if (preConnectBuffer != null) {
				channel.writeAndFlush(preConnectBuffer);
				preConnectBuffer = null;
			}
		}
		dataSendState = DataSendState.FINISHED;
	}

	private enum DataSendState {

		IDLE, SENDING, WAITING_FOR_FINISH, FINISHING, FINISHED;

		private boolean isAtLeast(@NotNull DataSendState state) {
			return ordinal() >= state.ordinal();
		}

	}
	
}
