package de.terrarier.netlistening.api.encryption;

import de.terrarier.netlistening.internals.AssumeNotNull;

/**
 * @author Terrarier2111
 * @since 1.0
 */
public abstract class EncryptionData {

    private final EncryptionOptions options;

    EncryptionData(@AssumeNotNull EncryptionOptions encryptionOptions) {
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
