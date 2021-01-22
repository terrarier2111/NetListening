package de.terrarier.netlistening.internals;

import de.terrarier.netlistening.Application;
import de.terrarier.netlistening.api.compression.CompressionSetting;
import de.terrarier.netlistening.api.encryption.*;
import de.terrarier.netlistening.api.type.DataType;
import de.terrarier.netlistening.impl.ClientImpl;
import de.terrarier.netlistening.network.PacketSynchronization;
import de.terrarier.netlistening.utils.ByteBufUtilExtension;
import de.terrarier.netlistening.utils.IntContainer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class InternalPayLoad_Handshake extends InternalPayload {

	protected InternalPayLoad_Handshake() {
        super((byte) 0x3);
    }

	@Override
	protected void write(@NotNull Application application, @NotNull ByteBuf buffer) {
		checkWriteable(application, buffer, 1 + 1 + 1 + 1 + 1);
		buffer.writeBoolean(application.getCompressionSetting().isVarIntCompression());
		buffer.writeBoolean(application.getCompressionSetting().isNibbleCompression());
		buffer.writeByte(application.getPacketSynchronization().ordinal());
		final Charset charset = application.getStringEncoding();
		final boolean utf8 = charset.equals(UTF_8);
		buffer.writeBoolean(!utf8);
		if(!utf8) {
			final String charsetName = charset.name();
			final byte[] bytes = charsetName.getBytes(UTF_8);
			final byte length = (byte) bytes.length;
			checkWriteable(application, buffer, 1 + length);
			buffer.writeByte(length);
			buffer.writeBytes(bytes);
		}
		checkWriteable(application, buffer, 1);
		final EncryptionSetting encryptionSetting = application.getEncryptionSetting();
		final boolean encryption = encryptionSetting != null;
		buffer.writeBoolean(encryption);
		if(encryption) {
			final EncryptionOptions options = encryptionSetting.getAsymmetricSetting();
			checkWriteable(application, buffer, 1 + 4 + 1 + 1);
			buffer.writeByte(options.getType().ordinal());
			buffer.writeInt(options.getKeySize());
			buffer.writeByte(options.getMode().ordinal());
			buffer.writeByte(options.getPadding().ordinal());
			final byte[] serverKey = encryptionSetting.getEncryptionData().getPublicKey().getEncoded();
			final int serverKeyLength = serverKey.length;
			checkWriteable(application, buffer, 4 + serverKeyLength);
			buffer.writeInt(serverKeyLength);
			buffer.writeBytes(serverKey);
		}
	}

	@Override
	public void read(@NotNull Application application, @NotNull Channel channel, @NotNull ByteBuf buffer) throws CancelReadingSignal {
		if(!application.isClient()) {
			throw new IllegalStateException("The connection " + channel.toString() + " has sent invalid data!");
		}
		final IntContainer required = new IntContainer();
		checkReadable(buffer, required.getAndAdd(4), 1 + 1 + 1 + 1 + 1);
		final boolean varIntCompression = buffer.readBoolean();
		final boolean nibbleCompression = buffer.readBoolean();
		final byte packetSyncId = buffer.readByte();
		Charset charset = null;
		if(buffer.readBoolean()) {
			checkReadable(buffer, required.getAndAdd(1), 1 + 1);
			final byte length = buffer.readByte();
			checkReadable(buffer, required.getAndAdd(1 + length), 1 + length);
			final byte[] bytes = ByteBufUtilExtension.readBytes(buffer, length);
			charset = Charset.forName(new String(bytes, UTF_8));
		}
		EncryptionSetting encryptionSetting = null;
		byte[] serverKey = null;
		if(buffer.readBoolean()) {
			final byte type = buffer.readByte();
			checkReadable(buffer, required.getAndAdd(4), 4);
			final int keySize = buffer.readInt();
			final byte mode = buffer.readByte();
			final byte padding = buffer.readByte();
			final EncryptionOptions asymmetricEncryptionOptions = new EncryptionOptions();
			asymmetricEncryptionOptions.type(CipherEncryptionAlgorithm.fromId(type));
			asymmetricEncryptionOptions.keySize(keySize);
			asymmetricEncryptionOptions.mode(CipherAlgorithmMode.fromId(mode));
			asymmetricEncryptionOptions.padding(CipherAlgorithmPadding.fromId(padding));
			checkReadable(buffer, required.getAndAdd(4), 4);
			final int serverKeyLength = buffer.readInt();
			checkReadable(buffer, required.getAndAdd(serverKeyLength), serverKeyLength);
			serverKey = ByteBufUtilExtension.readBytes(buffer, serverKeyLength);
			encryptionSetting = new EncryptionSetting();
			encryptionSetting.asymmetricEncryptionOptions(asymmetricEncryptionOptions);
		}
		final ClientImpl client = (ClientImpl) application;
		final PacketSynchronization packetSynchronization = PacketSynchronization.fromId(packetSyncId);
		final CompressionSetting compressionSetting = new CompressionSetting().varIntCompression(varIntCompression).nibbleCompression(nibbleCompression);
		client.receiveHandshake(compressionSetting, packetSynchronization, charset, encryptionSetting, serverKey);
		final ByteBuf initBuffer = Unpooled.buffer();
		DataType.getDTCP().write0(application, initBuffer, ENCRYPTION_INIT);
		client.sendRawData(initBuffer);
	}

}
