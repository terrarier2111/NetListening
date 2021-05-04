package de.terrarier.netlistening.api.encryption;

import de.terrarier.netlistening.internals.AssumeNotNull;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 * @param <T> the encryption setting wrapper which is to be configured.
 */
public final class EncryptionOptionsWrapper<T extends EncryptionOptionsSuperBuilder<T>> {

    private final EncryptionOptionsSuperBuilder<T> encryptionBuilder;
    private final EncryptionOptions encryptionOptions;

    public EncryptionOptionsWrapper(@NotNull EncryptionOptionsSuperBuilder<T> encryptionBuilder) {
        this(encryptionBuilder, new EncryptionOptions());
    }

    public EncryptionOptionsWrapper(@NotNull EncryptionOptionsSuperBuilder<T> encryptionBuilder,
                                    @NotNull EncryptionOptions encryptionOptions) {
        this.encryptionBuilder = encryptionBuilder;
        this.encryptionOptions = encryptionOptions;
    }

    /**
     * @see EncryptionOptions#type(CipherEncryptionAlgorithm) 
     */
    @AssumeNotNull
    public EncryptionOptionsWrapper<T> type(@NotNull CipherEncryptionAlgorithm type) {
        encryptionOptions.type(type);
        return this;
    }

    /**
     * @see EncryptionOptions#keySize(int) 
     */
    @AssumeNotNull
    public EncryptionOptionsWrapper<T> keySize(int keySize) {
        encryptionOptions.keySize(keySize);
        return this;
    }

    /**
     * @see EncryptionOptions#mode(CipherAlgorithmMode) 
     */
    @AssumeNotNull
    public EncryptionOptionsWrapper<T> mode(@NotNull CipherAlgorithmMode mode) {
        encryptionOptions.mode(mode);
        return this;
    }

    /**
     * @see EncryptionOptions#padding(CipherAlgorithmPadding) 
     */
    @AssumeNotNull
    public EncryptionOptionsWrapper<T> padding(@NotNull CipherAlgorithmPadding padding) {
        encryptionOptions.padding(padding);
        return this;
    }

    /**
     * Sets the encryption options for the
     * encryption setting wrapper and returns
     * this wrapper.
     *
     * @return an encryption setting wrapper.
     */
    @SuppressWarnings("unchecked")
    @AssumeNotNull
    public T build() {
        encryptionBuilder.encryptionOptions(encryptionOptions);
        return (T) encryptionBuilder;
    }

}
