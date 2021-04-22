package de.terrarier.netlistening.api.encryption;

import de.terrarier.netlistening.internals.AssumeNotNull;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public abstract class EncryptionData {

    private final EncryptionOptions options;

    EncryptionData(@NotNull EncryptionOptions encryptionOptions) {
        this.options = encryptionOptions;
    }

    /**
     * @return the options applied on the key specified in the constructor.
     */
    @AssumeNotNull
    public final EncryptionOptions getOptions() {
        return options;
    }

}
