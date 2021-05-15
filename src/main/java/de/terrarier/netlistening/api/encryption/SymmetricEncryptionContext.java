package de.terrarier.netlistening.api.encryption;

import de.terrarier.netlistening.internals.AssumeNotNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import javax.crypto.SecretKey;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class SymmetricEncryptionContext {

    private final SecretKey secretKey;
    private final SymmetricEncryptionData symmetricEncryptionData;

    @Deprecated
    public SymmetricEncryptionContext(@NotNull EncryptionOptions symmetricEncryptionOptions,
                                      @NotNull SecretKey secretKey) {
        this(secretKey, symmetricEncryptionOptions);
    }

    public SymmetricEncryptionContext(@NotNull SecretKey secretKey,
                                      @NotNull EncryptionOptions symmetricEncryptionOptions) {
        this.secretKey = secretKey;
        this.symmetricEncryptionData = new SymmetricEncryptionData(symmetricEncryptionOptions, secretKey);
    }

    /**
     * @return the key which is used to en-/decrypt data.
     */
    @AssumeNotNull
    public SecretKey getSecretKey() {
        return secretKey;
    }

    /**
     * @param data the data to be encrypted.
     * @return the data encrypted with the internal encryption key.
     */
    @ApiStatus.Internal
    @AssumeNotNull
    public byte[] encrypt(@AssumeNotNull byte[] data) {
        return SymmetricEncryptionUtil.encrypt(data, symmetricEncryptionData);
    }

    /**
     * @param data the data to be decrypted.
     * @return the data decrypted with the internal encryption key.
     */
    @ApiStatus.Internal
    @AssumeNotNull
    public byte[] decrypt(@AssumeNotNull byte[] data) {
        return SymmetricEncryptionUtil.decrypt(data, symmetricEncryptionData);
    }

}
