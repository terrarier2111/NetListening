package de.terrarier.netlistening.api.encryption;

import de.terrarier.netlistening.internals.AssumeNotNull;
import de.terrarier.netlistening.internals.CheckNotNull;
import de.terrarier.netlistening.utils.ByteBufUtilExtension;
import de.terrarier.netlistening.utils.ObjectUtilFallback;
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

    public AsymmetricEncryptionData(@NotNull EncryptionOptions encryptionOptions, @NotNull PrivateKey privateKey,
                                    @NotNull PublicKey publicKey) {
        super(encryptionOptions);
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    public AsymmetricEncryptionData(@NotNull EncryptionOptions encryptionOptions, @CheckNotNull byte[] encryptionData)
            throws InvalidKeySpecException, NoSuchAlgorithmException {
        super(encryptionOptions);
        final ByteBuf buffer = Unpooled.wrappedBuffer(ObjectUtilFallback.checkNotNull(encryptionData, "encryptionData"));
        privateKey = AsymmetricEncryptionUtil.readPrivateKey(ByteBufUtilExtension.readBytes(buffer, buffer.readInt()),
                encryptionOptions);
        publicKey = AsymmetricEncryptionUtil.readPublicKey(ByteBufUtilExtension.getBytes(buffer, buffer.readInt()),
                encryptionOptions);
        buffer.release();
    }

    /**
     * @return the asymmetric private key used to decrypt the asymmetric public key sent by the client.
     */
    @AssumeNotNull
    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    /**
     * @return the asymmetric public key used to encrypt the asymmetric key sent by the client.
     */
    @AssumeNotNull
    public PublicKey getPublicKey() {
        return publicKey;
    }

    /**
     * Transforms the key pair into a byte array which is returned.
     * This should only be used to save the keys used for encryption
     * on the server side. The keys are not protected in any way.
     * The user is responsible to store them securely.
     *
     * @return the keys represented as a byte array.
     */
    @AssumeNotNull
    public byte[] keysToBytes() {
        final byte[] privateKeyData = privateKey.getEncoded();
        final int privateKeyLength = privateKeyData.length;
        final byte[] publicKeyData = publicKey.getEncoded();
        final int publicKeyLength = publicKeyData.length;
        final ByteBuf buffer = Unpooled.buffer(4 + privateKeyLength + 4 + publicKeyLength);
        buffer.writeInt(privateKeyLength);
        buffer.writeBytes(privateKeyData);
        buffer.writeInt(publicKeyLength);
        buffer.writeBytes(publicKeyData);
        final byte[] ret = ByteBufUtilExtension.getBytes(buffer);
        buffer.release();
        return ret;
    }

    /**
     * @deprecated use keysToBytes instead!
     */
    @AssumeNotNull
    @Deprecated
    public byte[] keysToByteArray() {
        return keysToBytes();
    }

}
