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
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.NotNull;

import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

import static de.terrarier.netlistening.api.encryption.AsymmetricEncryptionUtil.readPrivateKey;
import static de.terrarier.netlistening.api.encryption.AsymmetricEncryptionUtil.readPublicKey;
import static de.terrarier.netlistening.util.ByteBufUtilExtension.*;

/**
 * @author Terrarier2111
 * @since 1.0
 */
public final class AsymmetricEncryptionData extends EncryptionData {

    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    public AsymmetricEncryptionData(@NotNull EncryptionOptions encryptionOptions, @NotNull PrivateKey privateKey,
                                    @NotNull PublicKey publicKey) {
        super(encryptionOptions);
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    public AsymmetricEncryptionData(@NotNull EncryptionOptions encryptionOptions, byte @NotNull [] encryptionData)
            throws InvalidKeySpecException, NoSuchAlgorithmException {
        super(encryptionOptions);
        final ByteBuf buffer = Unpooled.wrappedBuffer(encryptionData);
        privateKey = readPrivateKey(readBytes(buffer, buffer.readInt()), encryptionOptions);
        publicKey = readPublicKey(getBytes(buffer, buffer.readInt()), encryptionOptions);
        buffer.release();
    }

    /**
     * @return the asymmetric private key used to decrypt the asymmetric public key sent by the client.
     */
    @AssumeNotNull
    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    /**
     * @return the asymmetric public key used to encrypt the asymmetric key sent by the client.
     */
    @AssumeNotNull
    public PublicKey getPublicKey() {
        return publicKey;
    }

    /**
     * Transforms the key pair into a byte array which is returned.
     * This should only be used to save the keys used for encryption
     * on the server side. The keys are not protected in any way.
     * The user is responsible to store them securely.
     *
     * @return the keys represented as a byte array.
     */
    @AssumeNotNull
    public byte[] keysToBytes() {
        final byte[] privateKeyData = privateKey.getEncoded();
        final int privateKeyLength = privateKeyData.length;
        final byte[] publicKeyData = publicKey.getEncoded();
        final int publicKeyLength = publicKeyData.length;
        final ByteBuf buffer = Unpooled.buffer(4 + privateKeyLength + 4 + publicKeyLength);
        buffer.writeInt(privateKeyLength);
        buffer.writeBytes(privateKeyData);
        buffer.writeInt(publicKeyLength);
        buffer.writeBytes(publicKeyData);
        return getBytesAndRelease(buffer);
    }

}
