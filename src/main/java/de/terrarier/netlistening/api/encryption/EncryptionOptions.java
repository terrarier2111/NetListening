/*
Copyright 2021 Terrarier2111

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package de.terrarier.netlistening.api.encryption;

import de.terrarier.netlistening.internal.AssumeNotNull;
import org.jetbrains.annotations.NotNull;

/**
 * @author Terrarier2111
 * @since 1.0
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
        final int sanitizedKeySize = keySize & 0x7FFFFFF8; // This magic number is validating the key size such that it is a multiple of 8.
        if (sanitizedKeySize < 8) {
            throw new IllegalArgumentException("The key size may not be smaller than 8.");
        }
        this.keySize = sanitizedKeySize;
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
        return type.name() + '/' + mode.name() + '/' + padding.getPaddingName();
    }

    private void checkModifiable() {
        if (built) {
            throw new IllegalStateException("The options were already built, you can't modify them anymore!");
        }
    }

    private void checkBuilt() {
        if (!built) {
            if (type == null) {
                throw new IllegalStateException("Please set the encryption algorithm!");
            }
            built = true;
            if (keySize < 8) {
                keySize = type.getDefaultSize();
            }
            if (mode == null) {
                mode = type.getDefaultMode();
            }
            if (padding == null) {
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
