package de.terrarier.netlistening.api.encryption;

import de.terrarier.netlistening.Client;
import de.terrarier.netlistening.api.encryption.hash.HashUtil;
import de.terrarier.netlistening.api.encryption.hash.HashingAlgorithm;
import de.terrarier.netlistening.impl.ClientImpl;
import de.terrarier.netlistening.internals.AssumeNotNull;
import de.terrarier.netlistening.internals.CheckNotNull;
import de.terrarier.netlistening.utils.ByteBufUtilExtension;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.NotNull;

import java.security.NoSuchAlgorithmException;

import static de.terrarier.netlistening.utils.ObjectUtilFallback.checkNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author Terrarier2111
 * @since 1.0
 */
public final class ServerKey {

    private byte[] key;
    private final byte[] keyHash;
    private final HashingAlgorithm hashingAlgorithm;

    public ServerKey(@CheckNotNull byte[] bytes) {
        final ByteBuf buffer = Unpooled.wrappedBuffer(checkNotNull(bytes, "bytes"));
        final byte hashingAlgorithmLength = buffer.readByte();
        final byte[] hashData = ByteBufUtilExtension.readBytes(buffer, hashingAlgorithmLength);
        this.hashingAlgorithm = HashingAlgorithm.valueOf(new String(hashData, UTF_8));
        this.keyHash = ByteBufUtilExtension.getBytes(buffer);
        buffer.release();
    }

    public ServerKey(@CheckNotNull byte[] key, @NotNull HashingAlgorithm hashingAlgorithm) throws NoSuchAlgorithmException {
        this(checkNotNull(key, "key"), HashUtil.hash(hashingAlgorithm, key), hashingAlgorithm);
    }

    public ServerKey(byte[] key, @CheckNotNull byte[] keyHash, @NotNull HashingAlgorithm hashingAlgorithm) {
        this.key = key;
        this.keyHash = checkNotNull(keyHash, "keyHash");
        this.hashingAlgorithm = hashingAlgorithm;
    }

    /**
     * @return the public key provided by the server.
     */
    @AssumeNotNull
    public byte[] getKey() {
        return key;
    }

    /**
     * @return the hash of the public key provided by the server.
     */
    @AssumeNotNull
    public byte[] getKeyHash() {
        return keyHash;
    }

    /**
     * @return the hashing algorithm used to hash the public key provided by the server.
     */
    @AssumeNotNull
    public HashingAlgorithm getHashingAlgorithm() {
        return hashingAlgorithm;
    }

    /**
     * Transforms the ServerKey into a byte array which can be used to store the key hash
     * which can be compared to the public key provided by the server in order to prevent
     * MITM attacks.
     *
     * @return a byte array which can be transformed back into a ServerKey.
     */
    @AssumeNotNull
    public byte[] toByteArray() {
        final byte[] hashingAlgorithmBytes = hashingAlgorithm.name().getBytes(UTF_8);
        final int habLength = hashingAlgorithmBytes.length;
        final ByteBuf buffer = Unpooled.buffer(1 + habLength + keyHash.length);
        buffer.writeByte(habLength);
        buffer.writeBytes(hashingAlgorithmBytes);
        buffer.writeBytes(keyHash);

        final byte[] ret = ByteBufUtilExtension.getBytes(buffer);
        buffer.release();
        return ret;
    }

    /**
     * @param keyHash the hash of a former ServerKey.
     * @param client  the Client the ServerKey gets created for.
     * @return the ServerKey generated from the hash.
     */
    @AssumeNotNull
    public static ServerKey fromHash(byte[] keyHash, @NotNull Client client) {
        return fromHash(keyHash, ((ClientImpl) client).getServerKeyHashing());
    }

    /**
     * @param keyHash          the hash of a former ServerKey.
     * @param hashingAlgorithm the HashingAlgorithm used to hash the ServerKey.
     * @return the ServerKey generated from the hash.
     */
    @AssumeNotNull
    public static ServerKey fromHash(@CheckNotNull byte[] keyHash, @NotNull HashingAlgorithm hashingAlgorithm) {
        return new ServerKey(null, checkNotNull(keyHash, "keyHash"), hashingAlgorithm);
    }

}
