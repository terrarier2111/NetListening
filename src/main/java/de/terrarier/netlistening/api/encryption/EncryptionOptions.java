package de.terrarier.netlistening.api.encryption;

import de.terrarier.netlistening.internals.AssumeNotNull;
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
    @AssumeNotNull
    public EncryptionOptions type(@NotNull CipherEncryptionAlgorithm type) {
        checkModifiable();
        this.type = type;
        return this;
    }

    /**
     * Sets the size of the key which should be used to en-/decrypt data.
     *
     * @param keySize the size of the key.
     * @return the local reference.
     */
    @AssumeNotNull
    public EncryptionOptions keySize(int keySize) {
        checkModifiable();
        this.keySize = keySize & 0x7FFFFFF8; // this magic number is validating the key size such that it is a multiple of 8.
        return this;
    }

    /**
     * Sets the algorithm mode which should be used for the en-/decryption of data.
     *
     * @param mode the algorithm mode which should be used for the en-/decryption of data.
     * @return the local reference.
     */
    @AssumeNotNull
    public EncryptionOptions mode(@NotNull CipherAlgorithmMode mode) {
        checkModifiable();
        this.mode = mode;
        return this;
    }

    /**
     * Sets the padding which should be used for the en-/decryption of data.
     *
     * @param padding the padding which is used for the en-/decryption of data.
     * @return the local reference.
     */
    @AssumeNotNull
    public EncryptionOptions padding(@NotNull CipherAlgorithmPadding padding) {
        checkModifiable();
        this.padding = padding;
        return this;
    }

    /**
     * @return a string representing all available options for the creation of an instance of cipher.
     */
    @AssumeNotNull
    public String build() {
        checkBuilt();
        return type.name() + "/" + mode.name() + "/" + padding.getPaddingName();
    }

    private void checkModifiable() {
        if(built) {
            throw new IllegalStateException("The options were already built, you can't modify them anymore!");
        }
    }

    private void checkBuilt() {
        if(!built) {
            if(type == null) {
                throw new IllegalStateException("Please set the encryption algorithm!");
            }
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
    }

    /**
     * @return the encryption algorithm which is used to en-/decrypt data.
     */
    @AssumeNotNull
    public CipherEncryptionAlgorithm getType() {
        checkBuilt();
        return type;
    }

    /**
     * @return the size of the key which is used to en-/decrypt data.
     */
    public int getKeySize() {
        checkBuilt();
        return keySize;
    }

    /**
     * @return the algorithm mode which is used for the en-/decryption of data.
     */
    @AssumeNotNull
    public CipherAlgorithmMode getMode() {
        checkBuilt();
        return mode;
    }

    /**
     * @return the padding which is used for the en-/decryption of data.
     */
    @AssumeNotNull
    public CipherAlgorithmPadding getPadding() {
        checkBuilt();
        return padding;
    }

}
