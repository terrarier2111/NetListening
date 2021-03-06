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
package de.terrarier.netlistening.internal;

import de.terrarier.netlistening.Server;
import de.terrarier.netlistening.api.compression.CompressionSetting;
import de.terrarier.netlistening.api.encryption.*;
import de.terrarier.netlistening.api.event.EventManager;
import de.terrarier.netlistening.api.event.InvalidDataEvent;
import de.terrarier.netlistening.api.event.ListenerType;
import de.terrarier.netlistening.api.type.DataType;
import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ClientImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.ApiStatus;

import java.nio.charset.Charset;

import static de.terrarier.netlistening.util.ByteBufUtilExtension.readBytes;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author Terrarier2111
 * @since 1.0
 */
@ApiStatus.Internal
public final class InternalPayloadHandshake extends InternalPayload {

    InternalPayloadHandshake() {
        super((byte) 0x2, false);
    }

    @Override
    void write(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ByteBuf buffer) {
        final CompressionSetting compressionSetting = application.getCompressionSetting();
        final Charset charset = application.getStringEncoding();
        final boolean utf8 = charset.equals(UTF_8);
        final EncryptionSetting encryptionSetting = application.getEncryptionSetting();
        final boolean encryption = encryptionSetting != null;
        byte mask = (byte) (compressionSetting.isVarIntCompression() ? 1 : 0);
        if (compressionSetting.isNibbleCompression())
            mask |= 1 << 1;
        if (!utf8)
            mask |= 1 << 2;
        if (encryption)
            mask |= 1 << 3;
        checkWriteable(application, buffer, 1);
        buffer.writeByte(mask);
        if (!utf8) {
            final String charsetName = charset.name();
            final byte[] bytes = charsetName.getBytes(UTF_8);
            final int length = bytes.length;
            checkWriteable(application, buffer, 1 + length);
            buffer.writeByte(length);
            buffer.writeBytes(bytes);
        }
        if (encryption) {
            final EncryptionOptions options = encryptionSetting.getAsymmetricSetting();
            final byte[] serverKey = encryptionSetting.getEncryptionData().getPublicKey().getEncoded();
            final int serverKeyLength = serverKey.length;
            checkWriteable(buffer, 1 + 4 + 1 + 1 + 4 + serverKeyLength);
            buffer.writeByte(options.getType().ordinal());
            buffer.writeInt(options.getKeySize());
            buffer.writeByte(options.getMode().ordinal());
            buffer.writeByte(options.getPadding().ordinal());
            buffer.writeInt(serverKeyLength);
            buffer.writeBytes(serverKey);
        }
    }

    @Override
    void read(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ConnectionImpl connection,
              @AssumeNotNull ByteBuf buffer) throws CancelReadSignal {
        if (application instanceof Server) {
            final InvalidDataEvent event = new InvalidDataEvent(connection,
                    InvalidDataEvent.DataInvalidReason.INVALID_HANDSHAKE);

            if (application.getEventManager().callEvent(ListenerType.INVALID_DATA, EventManager.CancelAction.IGNORE,
                    event)) {
                return;
            }

            throw new IllegalStateException("The connection " + connection.getChannel() + " has sent invalid data!");
        }

        checkReadable(buffer, 1);
        final byte mask = buffer.readByte();
        Charset charset = null;
        if ((mask & 1 << 2) != 0) {
            checkReadable(buffer, 1 + 1);
            final byte length = buffer.readByte();
            checkReadable(buffer, length);
            final byte[] bytes = readBytes(buffer, length);
            charset = Charset.forName(new String(bytes, UTF_8));
        }
        EncryptionSetting encryptionSetting = null;
        byte[] serverKey = null;
        if ((mask & 1 << 3) != 0) {
            checkReadable(buffer, 1 + 4 + 1 + 1 + 4 + 1);
            final byte type = buffer.readByte();
            final int keySize = buffer.readInt();
            final byte mode = buffer.readByte();
            final byte padding = buffer.readByte();
            final EncryptionOptions asymmetricEncryptionOptions = new EncryptionOptions()
                    .type(CipherEncryptionAlgorithm.fromId(type))
                    .keySize(keySize)
                    .mode(CipherAlgorithmMode.fromId(mode))
                    .padding(CipherAlgorithmPadding.fromId(padding));
            final int serverKeyLength = buffer.readInt();
            checkReadable(buffer, serverKeyLength);
            serverKey = readBytes(buffer, serverKeyLength);
            encryptionSetting = new EncryptionSetting()
                    .asymmetricEncryptionOptions(asymmetricEncryptionOptions);
        }
        final ClientImpl client = (ClientImpl) application;
        final CompressionSetting compressionSetting = new CompressionSetting()
                .varIntCompression((mask & 1) != 0)
                .nibbleCompression((mask & 1 << 1) != 0);
        client.receiveHandshake(compressionSetting, charset, encryptionSetting, serverKey);

        if (encryptionSetting != null) {
            final ByteBuf initBuffer = Unpooled.buffer();
            DataType.getDTIP().write(application, initBuffer, ENCRYPTION_INIT);
            client.sendRawData(initBuffer);
        }
    }

}
