package de.terrarier.netlistening.internals;

import de.terrarier.netlistening.Server;
import de.terrarier.netlistening.api.compression.CompressionSetting;
import de.terrarier.netlistening.api.encryption.*;
import de.terrarier.netlistening.api.type.DataType;
import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ClientImpl;
import de.terrarier.netlistening.utils.ByteBufUtilExtension;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @since 1.0
 * @author Terrarier2111
 */
@ApiStatus.Internal
public final class InternalPayloadHandshake extends InternalPayload {

	InternalPayloadHandshake() {
        super((byte) 0x2);
    }

	@Override
	void write(@NotNull ApplicationImpl application, @NotNull ByteBuf buffer) {
		final CompressionSetting compressionSetting = application.getCompressionSetting();
		final Charset charset = application.getStringEncoding();
		final boolean utf8 = charset.equals(UTF_8);
		final EncryptionSetting encryptionSetting = application.getEncryptionSetting();
		final boolean encryption = encryptionSetting != null;
		byte mask = (byte) (compressionSetting.isVarIntCompression() ? 1 : 0);
		if(compressionSetting.isNibbleCompression())
			mask |= 1 << 1;
		if(!utf8)
			mask |= 1 << 2;
		if(encryption)
			mask |= 1 << 3;
		checkWriteable(application, buffer, 1);
		buffer.writeByte(mask);
		if(!utf8) {
			final String charsetName = charset.name();
			final byte[] bytes = charsetName.getBytes(UTF_8);
			final byte length = (byte) bytes.length;
			checkWriteable(application, buffer, 1 + length);
			buffer.writeByte(length);
			buffer.writeBytes(bytes);
		}
		if(encryption) {
			final EncryptionOptions options = encryptionSetting.getAsymmetricSetting();
			final byte[] serverKey = encryptionSetting.getEncryptionData().getPublicKey().getEncoded();
			final int serverKeyLength = serverKey.length;
			checkWriteable(application, buffer, 1 + 4 + 1 + 1 + 4 + serverKeyLength);
			buffer.writeByte(options.getType().ordinal());
			buffer.writeInt(options.getKeySize());
			buffer.writeByte(options.getMode().ordinal());
			buffer.writeByte(options.getPadding().ordinal());
			buffer.writeInt(serverKeyLength);
			buffer.writeBytes(serverKey);
		}
	}

	@Override
	public void read(@NotNull ApplicationImpl application, @NotNull Channel channel, @NotNull ByteBuf buffer)
			throws CancelReadSignal {
		if(application instanceof Server) {
			throw new IllegalStateException("The connection " + channel.toString() + " has sent invalid data!");
		}

		checkReadable(buffer, 1);
		final byte mask = buffer.readByte();
		Charset charset = null;
		if((mask & 1 << 2) != 0) {
			checkReadable(buffer, 1 + 1);
			final byte length = buffer.readByte();
			checkReadable(buffer, length);
			final byte[] bytes = ByteBufUtilExtension.readBytes(buffer, length);
			charset = Charset.forName(new String(bytes, UTF_8));
		}
		EncryptionSetting encryptionSetting = null;
		byte[] serverKey = null;
		if((mask & 1 << 3) != 0) {
			checkReadable(buffer, 1 + 4 + 1 + 1 + 4 + 1);
			final byte type = buffer.readByte();
			final int keySize = buffer.readInt();
			final byte mode = buffer.readByte();
			final byte padding = buffer.readByte();
			final EncryptionOptions asymmetricEncryptionOptions = new EncryptionOptions();
			asymmetricEncryptionOptions.type(CipherEncryptionAlgorithm.fromId(type));
			asymmetricEncryptionOptions.keySize(keySize);
			asymmetricEncryptionOptions.mode(CipherAlgorithmMode.fromId(mode));
			asymmetricEncryptionOptions.padding(CipherAlgorithmPadding.fromId(padding));
			final int serverKeyLength = buffer.readInt();
			checkReadable(buffer, serverKeyLength);
			serverKey = ByteBufUtilExtension.readBytes(buffer, serverKeyLength);
			encryptionSetting = new EncryptionSetting();
			encryptionSetting.asymmetricEncryptionOptions(asymmetricEncryptionOptions);
		}
		final ClientImpl client = (ClientImpl) application;
		final CompressionSetting compressionSetting = new CompressionSetting().varIntCompression((mask & 1) != 0)
				.nibbleCompression((mask & 1 << 1) != 0);
		client.receiveHandshake(compressionSetting, charset, encryptionSetting, serverKey);

		if(encryptionSetting != null) {
			final ByteBuf initBuffer = Unpooled.buffer();
			DataType.getDTIP().write0(application, initBuffer, ENCRYPTION_INIT);
			client.sendRawData(initBuffer);
		}
	}

}
