package de.terrarier.netlistening.api.encryption;

import org.jetbrains.annotations.NotNull;

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
public final class SymmetricEncryptionUtil {

    private SymmetricEncryptionUtil() {}

    @NotNull
    public static SymmetricEncryptionData generate(@NotNull EncryptionOptions encryptionOptions) throws NoSuchAlgorithmException {
        final KeyGenerator generator = KeyGenerator.getInstance(encryptionOptions.getType().name());
        final SecureRandom random = new SecureRandom();
        generator.init(encryptionOptions.getKeySize(), random);
        final SecretKey key = generator.generateKey();
        return new SymmetricEncryptionData(encryptionOptions, key);
    }

    public static byte[] encrypt(byte[] input, @NotNull SymmetricEncryptionData encryptionData) {
        return performCipher(input, encryptionData, Cipher.ENCRYPT_MODE);
    }

    public static byte[] decrypt(byte[] input, @NotNull SymmetricEncryptionData encryptionData) {
        return performCipher(input, encryptionData, Cipher.DECRYPT_MODE);
    }

    private static byte[] performCipher(byte[] input, @NotNull SymmetricEncryptionData encryptionData, int mode) {
        final SecretKey secretKey = encryptionData.getSecretKey();
        return AsymmetricEncryptionUtil.performCipher(input, encryptionData.getOptions(), secretKey, mode);
    }

    @NotNull
    public static SecretKey readSecretKey(byte[] secretKey, @NotNull EncryptionOptions encryptionOptions) {
        return new SecretKeySpec(secretKey, encryptionOptions.getType().name());
    }

}
