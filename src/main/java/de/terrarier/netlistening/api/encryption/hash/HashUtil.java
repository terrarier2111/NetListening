package de.terrarier.netlistening.api.encryption.hash;

import de.terrarier.netlistening.internals.AssumeNotNull;
import de.terrarier.netlistening.utils.ByteBufUtilExtension;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.ApiStatus;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * @author Terrarier2111
 * @since 1.0
 */
@ApiStatus.Internal
public final class HashUtil {

    private HashUtil() {
        throw new UnsupportedOperationException("This class may not be instantiated!");
    }

    public static byte[] hash(@AssumeNotNull HashingAlgorithm hashingAlgorithm, @AssumeNotNull byte[] data)
            throws NoSuchAlgorithmException {
        return hash(hashingAlgorithm, data, 0);
    }

    private static byte[] hash(@AssumeNotNull HashingAlgorithm hashingAlgorithm, @AssumeNotNull byte[] data, int salt)
            throws NoSuchAlgorithmException {
        final MessageDigest digest = MessageDigest.getInstance(hashingAlgorithm.getRawName());
        if (salt > 0) {
            final ByteBuf dataBuffer = Unpooled.buffer(data.length + salt);
            dataBuffer.writeBytes(data);
            dataBuffer.writeBytes(generateSalt(salt));
            data = ByteBufUtilExtension.getBytes(dataBuffer);
            dataBuffer.release();
        }
        return digest.digest(data);
    }

    @AssumeNotNull
    private static byte[] generateSalt(int length) {
        final byte[] salt = new byte[length];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    @AssumeNotNull
    public static byte[] calculateHMAC(@AssumeNotNull byte[] data, @AssumeNotNull byte[] key,
                                       @AssumeNotNull HashingAlgorithm algorithm)
            throws NoSuchAlgorithmException, InvalidKeyException {
        final String macName = algorithm.getMacName();
        final SecretKeySpec secretKeySpec = new SecretKeySpec(key, macName);
        final Mac mac = Mac.getInstance(macName);
        mac.init(secretKeySpec);
        return mac.doFinal(data);
    }

}
