package de.terrarier.netlistening.api.encryption;

import de.terrarier.netlistening.internals.AssumeNotNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * @since 1.0
 * @author Terrarier2111
 */
@ApiStatus.Internal
public final class AsymmetricEncryptionUtil {

    private AsymmetricEncryptionUtil() {
        throw new UnsupportedOperationException("This class may not be instantiated!");
    }

    @NotNull
    static AsymmetricEncryptionData generate(@AssumeNotNull EncryptionOptions encryptionOptions)
            throws NoSuchAlgorithmException {
        final KeyPairGenerator generator = KeyPairGenerator.getInstance(encryptionOptions.getType().name());
        final SecureRandom random = new SecureRandom();
        generator.initialize(encryptionOptions.getKeySize(), random);
        final KeyPair key = generator.generateKeyPair();
        return new AsymmetricEncryptionData(encryptionOptions, key.getPrivate(), key.getPublic());
    }

    public static byte[] encrypt(byte[] input, @AssumeNotNull EncryptionOptions encryptionOptions, @AssumeNotNull PublicKey key) {
        return performCipher(input, encryptionOptions, key, Cipher.ENCRYPT_MODE);
    }

    public static byte[] decrypt(byte[] input, @AssumeNotNull AsymmetricEncryptionData encryptionData) {
        return performCipher(input, encryptionData.getOptions(), encryptionData.getPrivateKey(), Cipher.DECRYPT_MODE);
    }

    static byte[] performCipher(byte[] input, @AssumeNotNull EncryptionOptions encryptionOptions, @AssumeNotNull Key key,
                                          int mode) {

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

    @NotNull
    public static PublicKey readPublicKey(byte[] publicKey, @AssumeNotNull EncryptionOptions encryptionOptions)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        final X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKey);
        final KeyFactory factory = KeyFactory.getInstance(encryptionOptions.getType().name());
        return factory.generatePublic(spec);
    }

    @NotNull
    static PrivateKey readPrivateKey(byte[] bytes, @AssumeNotNull EncryptionOptions encryptionOptions)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        final PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
        return KeyFactory.getInstance(encryptionOptions.getType().name()).generatePrivate(spec);
    }

}
