package de.terrarier.netlistening.api.encryption;

import de.terrarier.netlistening.utils.ByteBufUtilExtension;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.NotNull;

import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class AsymmetricEncryptionData extends EncryptionData {

    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    public AsymmetricEncryptionData(@NotNull EncryptionOptions encryptionOptions, @NotNull PrivateKey privateKey, @NotNull PublicKey publicKey) {
        super(encryptionOptions);
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    public AsymmetricEncryptionData(@NotNull EncryptionOptions encryptionOptions, byte[] encryptionData) throws InvalidKeySpecException, NoSuchAlgorithmException {
        super(encryptionOptions);
        final ByteBuf buffer = Unpooled.wrappedBuffer(encryptionData);
        privateKey = AsymmetricEncryptionUtil.readPrivateKey(ByteBufUtilExtension.readBytes(buffer, buffer.readInt()), encryptionOptions);
        publicKey = AsymmetricEncryptionUtil.readPublicKey(ByteBufUtilExtension.getBytes(buffer, buffer.readInt()), encryptionOptions);
        buffer.release();
    }

    /**
     * @return the asymmetric private key used to decrypt the asymmetric public key sent by the client.
     */
    @NotNull
    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    /**
     * @return the asymmetric public key used to encrypt the asymmetric key sent by the client.
     */
    @NotNull
    public PublicKey getPublicKey() {
        return publicKey;
    }

    /**
     * Transforms the key pair into a byte array which is returned.
     * This should ONLY be used to save the keys used for encryption
     * on the server side. The keys are NOT protected in any way.
     * The user is responsible to store them securely.
     *
     * @return the keys represented as a byte array.
     */
    public byte[] keysToByteArray() {
        final byte[] privateKeyData = privateKey.getEncoded();
        final int privateKeyLength = privateKeyData.length;
        final byte[] publicKeyData = publicKey.getEncoded();
        final int publicKeyLength = publicKeyData.length;
        final ByteBuf buffer = Unpooled.buffer(8 + privateKeyLength + publicKeyLength);
        buffer.writeInt(privateKeyLength);
        buffer.writeBytes(privateKeyData);
        buffer.writeInt(publicKeyLength);
        buffer.writeBytes(publicKeyData);
        final byte[] ret = ByteBufUtilExtension.getBytes(buffer);
        buffer.release();
        return ret;
    }

}
