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

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import static javax.crypto.Cipher.DECRYPT_MODE;
import static javax.crypto.Cipher.ENCRYPT_MODE;

/**
 * @author Terrarier2111
 * @since 1.0
 */
@ApiStatus.Internal
public final class SymmetricEncryptionUtil {

    private SymmetricEncryptionUtil() {
        throw new UnsupportedOperationException("This class may not be instantiated!");
    }

    @AssumeNotNull
    public static SymmetricEncryptionData generate(@AssumeNotNull EncryptionOptions encryptionOptions)
            throws NoSuchAlgorithmException {
        final KeyGenerator generator = KeyGenerator.getInstance(encryptionOptions.getType().name());
        generator.init(encryptionOptions.getKeySize(), new SecureRandom());
        return new SymmetricEncryptionData(encryptionOptions, generator.generateKey());
    }

    @AssumeNotNull
    static byte[] encrypt(@AssumeNotNull byte[] input, @AssumeNotNull SymmetricEncryptionData encryptionData) {
        return performCipher(input, encryptionData, ENCRYPT_MODE);
    }

    @AssumeNotNull
    static byte[] decrypt(@AssumeNotNull byte[] input, @AssumeNotNull SymmetricEncryptionData encryptionData) {
        return performCipher(input, encryptionData, DECRYPT_MODE);
    }

    @AssumeNotNull
    private static byte[] performCipher(@AssumeNotNull byte[] input,
                                        @AssumeNotNull SymmetricEncryptionData encryptionData, int mode) {
        final SecretKey secretKey = encryptionData.getSecretKey();
        return AsymmetricEncryptionUtil.performCipher(input, encryptionData.getOptions(), secretKey, mode);
    }

    @AssumeNotNull
    public static SecretKey readSecretKey(@AssumeNotNull byte[] secretKey,
                                          @AssumeNotNull EncryptionOptions encryptionOptions) {
        return new SecretKeySpec(secretKey, encryptionOptions.getType().name());
    }

}
