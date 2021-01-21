package de.terrarier.netlistening.api.encryption;

import org.jetbrains.annotations.NotNull;

import javax.crypto.SecretKey;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class SymmetricEncryptionData extends EncryptionData {

    private final SecretKey secretKey;

    public SymmetricEncryptionData(@NotNull EncryptionOptions encryptionOptions, @NotNull SecretKey secretKey) {
        super(encryptionOptions);
        this.secretKey = secretKey;
    }

    /**
     * @return the key used to encrypt and decrypt data.
     */
    @NotNull
    public SecretKey getSecretKey() {
        return secretKey;
    }

}
