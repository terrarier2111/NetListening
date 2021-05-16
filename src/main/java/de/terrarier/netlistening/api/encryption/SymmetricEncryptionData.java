package de.terrarier.netlistening.api.encryption;

import de.terrarier.netlistening.internals.AssumeNotNull;
import org.jetbrains.annotations.NotNull;

import javax.crypto.SecretKey;

/**
 * @author Terrarier2111
 * @since 1.0
 */
public final class SymmetricEncryptionData extends EncryptionData {

    private final SecretKey secretKey;

    public SymmetricEncryptionData(@NotNull EncryptionOptions encryptionOptions, @NotNull SecretKey secretKey) {
        super(encryptionOptions);
        this.secretKey = secretKey;
    }

    /**
     * @return the key used to en-/decrypt data.
     */
    @AssumeNotNull
    public SecretKey getSecretKey() {
        return secretKey;
    }

}
