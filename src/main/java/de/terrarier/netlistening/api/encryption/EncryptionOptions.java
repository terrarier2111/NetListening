package de.terrarier.netlistening.api.encryption;

import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class EncryptionOptions {

    private CipherEncryptionAlgorithm type;
    private int keySize;
    private CipherAlgorithmMode mode;
    private CipherAlgorithmPadding padding;
    private boolean built;

    /**
     * Sets the encryption algorithm which should be used to en-/decrypt data.
     *
     * @param type the encryption algorithm which should be used.
     * @return the local reference.
     */
    @NotNull
    public EncryptionOptions type(@NotNull CipherEncryptionAlgorithm type) {
        if(built) { // TODO: Throw exception here!
            return this;
        }
        this.type = type;
        return this;
    }

    /**
     * Sets the size of the key which should be used to en-/decrypt data.
     *
     * @param keySize the size of the key.
     * @return the local reference.
     */
    @NotNull
    public EncryptionOptions keySize(int keySize) {
        if(built) {
            return this;
        }
        this.keySize = keySize & 0x7FFFFFF8; // this magic number is validating the keySize
        return this;
    }

    /**
     * Sets the algorithm mode which should be used for the en-/decryption of data.
     *
     * @param mode the algorithm mode which should be used for the en-/decryption of data.
     * @return the local reference.
     */
    @NotNull
    public EncryptionOptions mode(@NotNull CipherAlgorithmMode mode) {
        if(built) {
            return this;
        }
        this.mode = mode;
        return this;
    }

    /**
     * Sets the padding which should be used for the en-/decryption of data.
     *
     * @param padding the padding which is used for the en-/decryption of data.
     * @return the local reference.
     */
    @NotNull
    public EncryptionOptions padding(@NotNull CipherAlgorithmPadding padding) {
        if(built) {
            return this;
        }
        this.padding = padding;
        return this;
    }

    /**
     * @return a string representing all available options for the creation of an instance of cipher.
     */
    @NotNull
    public String build() {
        if(type == null) {
            throw new IllegalStateException("Please set the encryption algorithm!");
        }
        if(!built) {
            built = true;
            if (keySize < 8) {
                keySize = type.getDefaultSize();
            }
            if(mode == null) {
                mode = type.getDefaultMode();
            }
            if(padding == null) {
                padding = type.getDefaultPadding();
            }
        }
        return type.name() + "/" + mode.name() + "/" + padding.getPaddingName();
    }

    /**
     * @return the encryption algorithm which gets used to en-/decrypt data.
     */
    @NotNull
    public CipherEncryptionAlgorithm getType() {
        if(!built) {
            build();
        }
        return type;
    }

    /**
     * @return the size of the key which is used to en-/decrypt data.
     */
    public int getKeySize() {
        if(!built) {
            build();
        }
        return keySize;
    }

    /**
     * @return the algorithm mode which is used for the en-/decryption of data.
     */
    @NotNull
    public CipherAlgorithmMode getMode() {
        if(!built) {
            build();
        }
        return mode;
    }

    /**
     * @return the padding which is used for the en-/decryption of data.
     */
    @NotNull
    public CipherAlgorithmPadding getPadding() {
        if(!built) {
            build();
        }
        return padding;
    }

}
