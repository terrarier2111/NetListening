package de.terrarier.netlistening.api.encryption;

import org.jetbrains.annotations.NotNull;

import javax.crypto.SecretKey;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class SymmetricEncryptionContext {

    private final SecretKey secretKey;
    private final SymmetricEncryptionData symmetricEncryptionData;

    public SymmetricEncryptionContext(@NotNull EncryptionOptions symmetricEncryptionOptions, @NotNull SecretKey secretKey) {
        this.symmetricEncryptionData = new SymmetricEncryptionData(symmetricEncryptionOptions, secretKey);
        this.secretKey = secretKey;
    }

    /**
     * @return the key which is used to en-/decrypt data.
     */
    @NotNull
    public SecretKey getSecretKey() {
        return secretKey;
    }

    /**
     * @param data the data to be encrypted.
     * @return the data encrypted with the internal encryption key.
     */
    public byte[] encrypt(byte[] data) {
        return SymmetricEncryptionUtil.encrypt(data, symmetricEncryptionData);
    }

    /**
     * @param data the data to be decrypted.
     * @return the data decrypted with the internal encryption key.
     */
    public byte[] decrypt(byte[] data) {
        return SymmetricEncryptionUtil.decrypt(data, symmetricEncryptionData);
    }

}
