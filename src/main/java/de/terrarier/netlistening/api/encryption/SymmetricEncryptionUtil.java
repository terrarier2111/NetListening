package de.terrarier.netlistening.api.encryption;

import de.terrarier.netlistening.internals.AssumeNotNull;
import org.jetbrains.annotations.ApiStatus;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * @since 1.0
 * @author Terrarier2111
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
        return performCipher(input, encryptionData, Cipher.ENCRYPT_MODE);
    }

    @AssumeNotNull
    static byte[] decrypt(@AssumeNotNull byte[] input, @AssumeNotNull SymmetricEncryptionData encryptionData) {
        return performCipher(input, encryptionData, Cipher.DECRYPT_MODE);
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
