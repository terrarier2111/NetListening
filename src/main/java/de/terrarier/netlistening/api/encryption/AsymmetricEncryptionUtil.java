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

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * @author Terrarier2111
 * @since 1.0
 */
@ApiStatus.Internal
public final class AsymmetricEncryptionUtil {

    private AsymmetricEncryptionUtil() {
        throw new UnsupportedOperationException("This class may not be instantiated!");
    }

    @AssumeNotNull
    static AsymmetricEncryptionData generate(@AssumeNotNull EncryptionOptions encryptionOptions)
            throws NoSuchAlgorithmException {
        final KeyPairGenerator generator = KeyPairGenerator.getInstance(encryptionOptions.getType().name());
        final SecureRandom random = new SecureRandom();
        generator.initialize(encryptionOptions.getKeySize(), random);
        final KeyPair key = generator.generateKeyPair();
        return new AsymmetricEncryptionData(encryptionOptions, key.getPrivate(), key.getPublic());
    }

    @AssumeNotNull
    public static byte[] encrypt(@AssumeNotNull byte[] input, @AssumeNotNull EncryptionOptions encryptionOptions,
                                 @AssumeNotNull PublicKey key) {
        return performCipher(input, encryptionOptions, key, Cipher.ENCRYPT_MODE);
    }

    @AssumeNotNull
    public static byte[] decrypt(@AssumeNotNull byte[] input, @AssumeNotNull AsymmetricEncryptionData encryptionData) {
        return performCipher(input, encryptionData.getOptions(), encryptionData.getPrivateKey(), Cipher.DECRYPT_MODE);
    }

    @AssumeNotNull
    static byte[] performCipher(@AssumeNotNull byte[] input, @AssumeNotNull EncryptionOptions encryptionOptions,
                                @AssumeNotNull Key key, int mode) {
        try {
            final Cipher cipher = Cipher.getInstance(encryptionOptions.build());
            cipher.init(mode, key);
            return cipher.doFinal(input);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException |
                InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }

    @AssumeNotNull
    public static PublicKey readPublicKey(@AssumeNotNull byte[] publicKey,
                                          @AssumeNotNull EncryptionOptions encryptionOptions)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        final X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKey);
        final KeyFactory factory = KeyFactory.getInstance(encryptionOptions.getType().name());
        return factory.generatePublic(spec);
    }

    @AssumeNotNull
    static PrivateKey readPrivateKey(@AssumeNotNull byte[] bytes, @AssumeNotNull EncryptionOptions encryptionOptions)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        final PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
        final KeyFactory factory = KeyFactory.getInstance(encryptionOptions.getType().name());
        return factory.generatePrivate(spec);
    }

}
