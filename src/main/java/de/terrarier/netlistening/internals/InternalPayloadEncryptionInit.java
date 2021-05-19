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
package de.terrarier.netlistening.internals;

import de.terrarier.netlistening.Client;
import de.terrarier.netlistening.Server;
import de.terrarier.netlistening.api.encryption.*;
import de.terrarier.netlistening.api.encryption.hash.HashingAlgorithm;
import de.terrarier.netlistening.api.encryption.hash.HmacApplicationPolicy;
import de.terrarier.netlistening.api.encryption.hash.HmacSetting;
import de.terrarier.netlistening.api.type.DataType;
import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ClientImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.utils.ByteBufUtilExtension;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.ApiStatus;

import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

/**
 * @author Terrarier2111
 * @since 1.0
 */
@ApiStatus.Internal
public final class InternalPayloadEncryptionInit extends InternalPayload {

    private final SymmetricEncryptionData symmetricEncryptionData;
    private final PublicKey publicKey;
    private final byte[] hmacKey;

    private InternalPayloadEncryptionInit(@AssumeNotNull SymmetricEncryptionData symmetricEncryptionData,
                                          @AssumeNotNull PublicKey publicKey, byte[] hmacKey) {
        super((byte) 0x3);
        this.symmetricEncryptionData = symmetricEncryptionData;
        this.publicKey = publicKey;
        this.hmacKey = hmacKey;
    }

    InternalPayloadEncryptionInit() {
        super((byte) 0x3);
        symmetricEncryptionData = null;
        publicKey = null;
        hmacKey = null;
    }

    @Override
    void write(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ByteBuf buffer) {
        final EncryptionSetting encryptionSetting = application.getEncryptionSetting();
        if (application instanceof Server) {
            final EncryptionOptions asymmetricSetting = encryptionSetting.getAsymmetricSetting();
            final byte[] key = symmetricEncryptionData.getSecretKey().getEncoded();
            final byte[] secretKey = AsymmetricEncryptionUtil.encrypt(key, asymmetricSetting, publicKey);
            writeOptions(symmetricEncryptionData.getOptions(), secretKey, buffer, application);
            final boolean hmac = hmacKey != null;
            checkWriteable(application, buffer, 1);
            buffer.writeBoolean(hmac);
            if (hmac) {
                final HmacSetting hmacSetting = encryptionSetting.getHmacSetting();
                final EncryptionOptions hmacOptions = hmacSetting.getEncryptionSetting();
                writeOptions(hmacOptions, hmacKey, buffer, application);
                checkWriteable(application, buffer, 1 + 1);
                buffer.writeByte(hmacSetting.getApplicationPolicy().ordinal());
                buffer.writeByte(hmacSetting.getHashingAlgorithm().ordinal());
            }
            return;
        }
        writeKey(encryptionSetting.getEncryptionData().getPublicKey().getEncoded(), buffer, application);
    }

    @Override
    public void read(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ConnectionImpl connection,
                     @AssumeNotNull ByteBuf buffer) throws CancelReadSignal {
        final byte[] key = readKey(buffer);
        if (application instanceof Client) {
            checkReadable(buffer, 7 + 1);
            final EncryptionOptions symmetricOptions = readOptions(buffer);
            final EncryptionSetting encryptionSetting = application.getEncryptionSetting();
            if (buffer.readBoolean()) {
                final byte[] hmacKey = readKey(buffer);
                checkReadable(buffer, 7 + 1 + 1);
                final EncryptionOptions hmacOptions = readOptions(buffer);
                final byte useCase = buffer.readByte();
                final byte hashingAlgorithm = buffer.readByte();
                final HmacSetting hmacSetting = new HmacSetting()
                        .applicationPolicy(HmacApplicationPolicy.fromId(useCase))
                        .hashingAlgorithm(HashingAlgorithm.fromId(hashingAlgorithm))
                        .encryptionOptions(hmacOptions);

                encryptionSetting.hmac(hmacSetting);
                connection.setHmacKey(hmacKey);
            }
            connection.setSymmetricKey(symmetricOptions,
                    AsymmetricEncryptionUtil.decrypt(key, encryptionSetting.getEncryptionData()));
            final ByteBuf finishBuffer = Unpooled.buffer(application.getCompressionSetting().isVarIntCompression() ? 2 : 5);
            DataType.getDTIP().write0(application, finishBuffer, ENCRYPTION_FINISH);
            ((ClientImpl) application).sendRawData(finishBuffer);
        } else {
            final EncryptionSetting encryptionSetting = application.getEncryptionSetting();
            try {
                final PublicKey publicKey = AsymmetricEncryptionUtil.readPublicKey(key,
                        encryptionSetting.getAsymmetricSetting());
                final ByteBuf initBuffer = Unpooled.buffer();

                DataType.getDTIP().write0(application, initBuffer,
                        new InternalPayloadEncryptionInit(
                                new SymmetricEncryptionData(encryptionSetting.getSymmetricSetting(),
                                        connection.getEncryptionContext().getSecretKey()), publicKey,
                                connection.getHmacKey()));
                connection.getChannel().writeAndFlush(initBuffer);
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                e.printStackTrace();
            }
        }
    }

    @AssumeNotNull
    private static EncryptionOptions readOptions(@AssumeNotNull ByteBuf buffer) {
        final byte type = buffer.readByte();
        final int keySize = buffer.readInt();
        final byte mode = buffer.readByte();
        final byte padding = buffer.readByte();
        return new EncryptionOptions()
                .type(CipherEncryptionAlgorithm.fromId(type))
                .keySize(keySize)
                .mode(CipherAlgorithmMode.fromId(mode))
                .padding(CipherAlgorithmPadding.fromId(padding));
    }

    private static void writeOptions(@AssumeNotNull EncryptionOptions options, @AssumeNotNull byte[] key,
                                     @AssumeNotNull ByteBuf buffer, @AssumeNotNull ApplicationImpl application) {
        writeKey(key, buffer, application);
        checkWriteable(application, buffer, 1 + 4 + 1 + 1);
        buffer.writeByte(options.getType().ordinal());
        buffer.writeInt(options.getKeySize());
        buffer.writeByte(options.getMode().ordinal());
        buffer.writeByte(options.getPadding().ordinal());
    }

    private static void writeKey(@AssumeNotNull byte[] key, @AssumeNotNull ByteBuf buffer,
                                 @AssumeNotNull ApplicationImpl application) {
        final int keyLength = key.length;
        checkWriteable(application, buffer, 4 + keyLength);
        buffer.writeInt(keyLength);
        buffer.writeBytes(key);
    }

    @AssumeNotNull
    private static byte[] readKey(@AssumeNotNull ByteBuf buffer) throws CancelReadSignal {
        checkReadable(buffer, 4 + 1);
        final int keyLength = buffer.readInt();
        checkReadable(buffer, keyLength);
        return ByteBufUtilExtension.readBytes(buffer, keyLength);
    }

}
