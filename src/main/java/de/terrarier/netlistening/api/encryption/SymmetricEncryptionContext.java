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
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import javax.crypto.SecretKey;

/**
 * @author Terrarier2111
 * @since 1.0
 */
public final class SymmetricEncryptionContext {

    private final SecretKey secretKey;
    private final SymmetricEncryptionData symmetricEncryptionData;

    @Deprecated
    public SymmetricEncryptionContext(@NotNull EncryptionOptions symmetricEncryptionOptions,
                                      @NotNull SecretKey secretKey) {
        this(secretKey, symmetricEncryptionOptions);
    }

    public SymmetricEncryptionContext(@NotNull SecretKey secretKey,
                                      @NotNull EncryptionOptions symmetricEncryptionOptions) {
        this.secretKey = secretKey;
        this.symmetricEncryptionData = new SymmetricEncryptionData(symmetricEncryptionOptions, secretKey);
    }

    /**
     * @return the key which is used to en-/decrypt data.
     */
    @AssumeNotNull
    public SecretKey getSecretKey() {
        return secretKey;
    }

    /**
     * @param data the data to be encrypted.
     * @return the data encrypted with the internal encryption key.
     */
    @ApiStatus.Internal
    @AssumeNotNull
    public byte[] encrypt(@AssumeNotNull byte[] data) {
        return SymmetricEncryptionUtil.encrypt(data, symmetricEncryptionData);
    }

    /**
     * @param data the data to be decrypted.
     * @return the data decrypted with the internal encryption key.
     */
    @ApiStatus.Internal
    @AssumeNotNull
    public byte[] decrypt(@AssumeNotNull byte[] data) {
        return SymmetricEncryptionUtil.decrypt(data, symmetricEncryptionData);
    }

}
