package de.terrarier.netlistening.api.encryption;

import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 * @param <T>
 */
public final class EncryptionOptionsWrapper<T extends EncryptionOptionsSuperBuilder<T>> {

    private final EncryptionOptionsSuperBuilder<T> encryptionBuilder;
    private final EncryptionOptions encryptionOptions;

    public EncryptionOptionsWrapper(@NotNull EncryptionOptionsSuperBuilder<T> encryptionBuilder) {
        this.encryptionBuilder = encryptionBuilder;
        this.encryptionOptions = new EncryptionOptions();
    }

    public EncryptionOptionsWrapper(@NotNull EncryptionOptionsSuperBuilder<T> encryptionBuilder, @NotNull EncryptionOptions encryptionOptions) {
        this.encryptionBuilder = encryptionBuilder;
        this.encryptionOptions = encryptionOptions;
    }

    /**
     * @see EncryptionOptions
     */
    @NotNull
    public EncryptionOptionsWrapper<T> type(@NotNull CipherEncryptionAlgorithm type) {
        encryptionOptions.type(type);
        return this;
    }

    /**
     * @see EncryptionOptions
     */
    @NotNull
    public EncryptionOptionsWrapper<T> keySize(int keySize) {
        encryptionOptions.keySize(keySize);
        return this;
    }

    /**
     * @see EncryptionOptions
     */
    @NotNull
    public EncryptionOptionsWrapper<T> mode(@NotNull CipherAlgorithmMode mode) {
        encryptionOptions.mode(mode);
        return this;
    }

    /**
     * @see EncryptionOptions
     */
    @NotNull
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
    @NotNull
    public T build() {
        encryptionBuilder.encryptionOptions(encryptionOptions);
        return (T) encryptionBuilder;
    }

}
