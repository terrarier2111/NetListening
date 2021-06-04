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

import de.terrarier.netlistening.Client;
import de.terrarier.netlistening.api.encryption.hash.HashUtil;
import de.terrarier.netlistening.api.encryption.hash.HashingAlgorithm;
import de.terrarier.netlistening.impl.ClientImpl;
import de.terrarier.netlistening.internals.AssumeNotNull;
import de.terrarier.netlistening.utils.ByteBufUtilExtension;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.NotNull;

import java.security.NoSuchAlgorithmException;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author Terrarier2111
 * @since 1.0
 */
public final class ServerKey {

    private final byte[] key;
    private final byte[] keyHash;
    private final HashingAlgorithm hashingAlgorithm;

    public ServerKey(byte @NotNull [] bytes) {
        final ByteBuf buffer = Unpooled.wrappedBuffer(bytes);
        final byte hashingAlgorithmLength = buffer.readByte();
        final byte[] hashData = ByteBufUtilExtension.readBytes(buffer, hashingAlgorithmLength);
        key = null;
        hashingAlgorithm = HashingAlgorithm.valueOf(new String(hashData, UTF_8));
        keyHash = ByteBufUtilExtension.getBytes(buffer);
        buffer.release();
    }

    public ServerKey(byte @NotNull [] key, @NotNull HashingAlgorithm hashingAlgorithm) throws NoSuchAlgorithmException {
        this(key, HashUtil.hash(hashingAlgorithm, key), hashingAlgorithm);
    }

    public ServerKey(byte[] key, byte @NotNull [] keyHash, @NotNull HashingAlgorithm hashingAlgorithm) {
        this.key = key;
        this.keyHash = keyHash;
        this.hashingAlgorithm = hashingAlgorithm;
    }

    /**
     * @return the public key provided by the server.
     */
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
    public static ServerKey fromHash(@AssumeNotNull byte[] keyHash, @NotNull Client client) {
        return fromHash(keyHash, ((ClientImpl) client).getServerKeyHashing());
    }

    /**
     * @param keyHash          the hash of a former ServerKey.
     * @param hashingAlgorithm the HashingAlgorithm used to hash the ServerKey.
     * @return the ServerKey generated from the hash.
     */
    @AssumeNotNull
    public static ServerKey fromHash(byte @NotNull [] keyHash, @NotNull HashingAlgorithm hashingAlgorithm) {
        return new ServerKey(null, keyHash, hashingAlgorithm);
    }

}
