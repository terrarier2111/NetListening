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
	private volatile DataSendState dataSendState = DataSendState.IDLE;
	private ByteBuf preConnectBuffer;
	private Queue<DataContainer> preConnectSendQueue;
	private SymmetricEncryptionContext encryptionContext;
	private byte[] hmacKey;
	// TODO: Improve and test delayed data sending mechanics.
	
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
		if(connected && dataSendState.isAtLeast(DataSendState.SENDING)) {
			if(dataSendState.isAtLeast(DataSendState.FINISHED)) {
				channel.writeAndFlush(data);
			}else {
				// TODO: Handle stuff incoming before!
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
		return dataSendState.isAtLeast(DataSendState.SENDING) && receivedPacket;
	}

	@NotNull
	public PacketCache getCache() {
		return cache;
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
			final DataType<InternalPayload> dtip = DataType.getDTIP();
			((DataTypeInternalPayload) dtip).write(application, buffer, InternalPayload.HANDSHAKE);
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
							dtip.write0(application, buffer, simpleSynchronization ?
									new InternalPayLoad_RegisterInPacket(out, data) : new InternalPayLoad_RegisterInPacket(data));
						}
					}

					if(inPacketsSize > 2) {
						for (int in = 5; in < inPacketsSize + 3; in++) {
							final DataType<?>[] data = inPackets.get(in).getData();
							dtip.write0(application, buffer, simpleSynchronization ?
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

	public void check() {
		if(!dataSendState.isAtLeast(DataSendState.SENDING)) {
			dataSendState = DataSendState.SENDING;

			if (preConnectBuffer != null && preConnectBuffer.writerIndex() > 0) {
				channel.writeAndFlush(preConnectBuffer);
				preConnectBuffer = null;
			} else {
				// writing the init data to the channel (without hitting the pre connect buffer)
				checkReceived();
			}

			if(application.getEncryptionSetting() == null) {
				prepare();
			}else {
				dataSendState = DataSendState.WAITING_FOR_FINISH;
			}
		}
	}

	public void writeToInitialBuffer(@NotNull ByteBuf buffer) {
		final DataSendState dataSendState = this.dataSendState; // caching volatile field get result
		if (!dataSendState.isAtLeast(DataSendState.SENDING)) {
			// TODO: Test if we have to discard the first pre connect buffer.
			checkReceived();
			transferData(buffer);
		} else {
			if(!trySend(buffer)) {
				// TODO: Test logic to send data delayed!
				if(dataSendState != DataSendState.WAITING_FOR_FINISH) { // check if it's not waiting for a response from the other end of the connection
					while(true) {
						if(this.dataSendState == DataSendState.WAITING_FOR_FINISH) {
							break;
						}else if(trySend(buffer)) {
							return;
						}
					}
				}
				if(preConnectBuffer == null) {
					preConnectBuffer = Unpooled.buffer();
				}
				transferData(buffer);
			}
		}
	}

	private boolean trySend(@NotNull ByteBuf buffer) {
		final DataSendState dataSendState = this.dataSendState; // caching volatile field get result
		if(dataSendState.isAtLeast(DataSendState.FINISHING)) {
			if(dataSendState == DataSendState.FINISHING) {
				while(this.dataSendState != DataSendState.FINISHED); // we are waiting until the execution of the prepare method has finished
			}
			channel.writeAndFlush(buffer);
			return true;
		}
		return false;
	}

	private void transferData(@NotNull ByteBuf buffer) {
		final int readable = buffer.readableBytes();
		ByteBufUtilExtension.correctSize(preConnectBuffer, readable, application.getBuffer());
		preConnectBuffer.writeBytes(ByteBufUtilExtension.readBytes(buffer, readable));
		buffer.release();
	}

	public void prepare() {
		dataSendState = DataSendState.FINISHING;
		if(preConnectSendQueue != null) {
			final Queue<DataContainer> sendQueue = preConnectSendQueue;
			preConnectSendQueue = null;
			for (DataContainer data : sendQueue) {
				channel.writeAndFlush(data);
			}
			sendQueue.clear();
		}

		final ByteBuf buffer = Unpooled.buffer(application.getCompressionSetting().isVarIntCompression() ? 1 : 4);
		InternalUtil.writeIntUnchecked(application, buffer, 0x2);
		channel.writeAndFlush(buffer);
		if (preConnectBuffer != null) {
			channel.writeAndFlush(preConnectBuffer);
			preConnectBuffer = null;
		}
		dataSendState = DataSendState.FINISHED;
	}

	private enum DataSendState {

		IDLE, SENDING, WAITING_FOR_FINISH, FINISHING, FINISHED;

		boolean isAtLeast(@NotNull DataSendState state) {
			return ordinal() >= state.ordinal();
		}

	}
	
}
