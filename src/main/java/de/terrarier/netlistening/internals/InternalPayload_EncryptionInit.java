package de.terrarier.netlistening.internals;

import de.terrarier.netlistening.Application;
import de.terrarier.netlistening.api.encryption.*;
import de.terrarier.netlistening.api.encryption.hash.HashingAlgorithm;
import de.terrarier.netlistening.api.encryption.hash.HmacSetting;
import de.terrarier.netlistening.api.encryption.hash.HmacUseCase;
import de.terrarier.netlistening.api.type.DataType;
import de.terrarier.netlistening.impl.ClientImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.utils.ByteBufUtilExtension;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;

import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class InternalPayload_EncryptionInit extends InternalPayload {

    private final SymmetricEncryptionData symmetricEncryptionData;
    private final PublicKey publicKey;
    private final byte[] hmacKey;

    private InternalPayload_EncryptionInit(@NotNull SymmetricEncryptionData symmetricEncryptionData,
                                           @NotNull PublicKey publicKey, byte[] hmacKey) {
        super((byte) 0x3);
        this.symmetricEncryptionData = symmetricEncryptionData;
        this.publicKey = publicKey;
        this.hmacKey = hmacKey;
    }

    public InternalPayload_EncryptionInit() {
        super((byte) 0x3);
        symmetricEncryptionData = null;
        publicKey = null;
        hmacKey = null;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void write(@NotNull Application application, @NotNull ByteBuf buffer) {
        if (!application.isClient()) {
            final EncryptionOptions asymmetricSetting = application.getEncryptionSetting().getAsymmetricSetting();
            final byte[] key = symmetricEncryptionData.getSecretKey().getEncoded();
            final byte[] secretKey = AsymmetricEncryptionUtil.encrypt(key, asymmetricSetting, publicKey);
            writeOptions(symmetricEncryptionData.getOptions(), secretKey, buffer, application);
            final HmacSetting hmacSetting = application.getEncryptionSetting().getHmacSetting();
            final boolean hmac = hmacSetting != null;
            checkWriteable(application, buffer, 1);
            buffer.writeBoolean(hmac);
            if(hmac) {
                final EncryptionOptions hmacOptions = hmacSetting.getEncryptionSetting();
                writeOptions(hmacOptions, hmacKey, buffer, application);
                checkWriteable(application, buffer, 1 + 1);
                buffer.writeByte(hmacSetting.getUseCase().ordinal());
                buffer.writeByte(hmacSetting.getHashingAlgorithm().ordinal());
            }
            return;
        }
        writeKey(application.getEncryptionSetting().getEncryptionData().getPublicKey().getEncoded(), buffer, application);
    }

    @Override
    public void read(@NotNull Application application, @NotNull Channel channel, @NotNull ByteBuf buffer)
            throws CancelReadingSignal {
        final byte[] key = readKey(buffer);
        if (application.isClient()) {
            checkReadable(buffer, 7 + 1);
            final EncryptionOptions symmetricOptions = readOptions(buffer);
            final ConnectionImpl connection = (ConnectionImpl) application.getConnection(null);
            final EncryptionSetting encryptionSetting = application.getEncryptionSetting();
            if(buffer.readBoolean()) {
                final byte[] hmacKey = readKey(buffer);
                checkReadable(buffer, 7 + 1 + 1);
                final EncryptionOptions hmacOptions = readOptions(buffer);
                final byte useCase = buffer.readByte();
                final byte hashingAlgorithm = buffer.readByte();
                final HmacSetting hmacSetting = new HmacSetting();
                hmacSetting.useCase(HmacUseCase.fromId(useCase));
                hmacSetting.hashingAlgorithm(HashingAlgorithm.fromId(hashingAlgorithm));
                hmacSetting.encryptionOptions(hmacOptions);

                encryptionSetting.hmac(hmacSetting);
                connection.setHmacKey(hmacKey);
            }
            connection.setSymmetricKey(symmetricOptions,
                    AsymmetricEncryptionUtil.decrypt(key, encryptionSetting.getEncryptionData()));
            final ByteBuf finishBuffer = Unpooled.buffer(application.getCompressionSetting().isVarIntCompression() ? 2 : 5);
            DataType.getDTIP().write0(application, finishBuffer, ENCRYPTION_FINISH);
            ((ClientImpl) application).sendRawData(finishBuffer);
        } else {
            try {
                final EncryptionSetting encryptionSetting = application.getEncryptionSetting();
                final PublicKey publicKey = AsymmetricEncryptionUtil.readPublicKey(key, encryptionSetting.getAsymmetricSetting());
                final ByteBuf initBuffer = Unpooled.buffer();
                final ConnectionImpl connection = (ConnectionImpl) application.getConnection(channel);
                DataType.getDTIP().write0(application, initBuffer,
                        new InternalPayload_EncryptionInit(
                                new SymmetricEncryptionData(encryptionSetting.getSymmetricSetting(),
                                        connection.getEncryptionContext().getSecretKey()), publicKey, connection.getHmacKey()));
                channel.writeAndFlush(initBuffer);
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                e.printStackTrace();
            }
        }
    }

    @NotNull
    private static EncryptionOptions readOptions(@NotNull ByteBuf buffer) {
        final byte type = buffer.readByte();
        final int keySize = buffer.readInt();
        final byte mode = buffer.readByte();
        final byte padding = buffer.readByte();
        final EncryptionOptions encryptionOptions = new EncryptionOptions();
        encryptionOptions.type(CipherEncryptionAlgorithm.fromId(type));
        encryptionOptions.keySize(keySize);
        encryptionOptions.mode(CipherAlgorithmMode.fromId(mode));
        encryptionOptions.padding(CipherAlgorithmPadding.fromId(padding));
        return encryptionOptions;
    }

    private static void writeOptions(@NotNull EncryptionOptions options, byte[] key, @NotNull ByteBuf buffer,
                              @NotNull Application application) {
        writeKey(key, buffer, application);
        checkWriteable(application, buffer, 1 + 4 + 1 + 1);
        buffer.writeByte(options.getType().ordinal());
        buffer.writeInt(options.getKeySize());
        buffer.writeByte(options.getMode().ordinal());
        buffer.writeByte(options.getPadding().ordinal());
    }

    private static void writeKey(byte[] key, @NotNull ByteBuf buffer, @NotNull Application application) {
        final int keyLength = key.length;
        checkWriteable(application, buffer, 4 + keyLength);
        buffer.writeInt(keyLength);
        buffer.writeBytes(key);
    }

    private static byte[] readKey(@NotNull ByteBuf buffer) throws CancelReadingSignal {
        checkReadable(buffer, 4 + 1);
        final int keyLength = buffer.readInt();
        checkReadable(buffer, keyLength);
        return ByteBufUtilExtension.readBytes(buffer, keyLength);
    }

}
