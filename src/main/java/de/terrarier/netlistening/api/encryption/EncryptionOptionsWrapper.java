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

import de.terrarier.netlistening.internals.AssumeNotNull;
import org.jetbrains.annotations.NotNull;

/**
 * @param <T> the encryption setting wrapper which is to be configured.
 * @author Terrarier2111
 * @since 1.0
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
