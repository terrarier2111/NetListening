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

import de.terrarier.netlistening.api.type.DataType;
import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.network.PacketDataDecoder;
import de.terrarier.netlistening.util.ByteBufUtilExtension;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

import static de.terrarier.netlistening.internal.InternalUtil.writeInt;
import static de.terrarier.netlistening.util.ByteBufUtilExtension.correctSize;
import static de.terrarier.netlistening.util.ByteBufUtilExtension.getBytes;

/**
 * @author Terrarier2111
 * @since 1.0
 */
@ApiStatus.Internal
public final class DataTypeEncrypt extends DataType<Void> {

    public DataTypeEncrypt() {
        super((byte) 0xD, (byte) 4, false);
    }

    @Override
    public Void read0(@AssumeNotNull PacketDataDecoder.DecoderContext decoderContext, @AssumeNotNull List<Object> out,
                      @AssumeNotNull ByteBuf buffer) throws Exception {
        checkReadable(buffer, 4);
        final int size = buffer.readInt();
        checkReadable(buffer, size);
        final byte[] decrypted = decoderContext.getConnection().getEncryptionContext().decrypt(
                ByteBufUtilExtension.readBytes(buffer, size));
        final PacketDataDecoder decoder = decoderContext.getDecoder();
        final ByteBuf dataBuffer = Unpooled.wrappedBuffer(decrypted);
        try {
            decoder.decode(decoderContext.getHandlerContext(), dataBuffer, out);
        } finally {
            dataBuffer.release();
        }
        return null;
    }

    @Override
    public void write0(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ConnectionImpl connection,
                       @AssumeNotNull ByteBuf buffer, Void data) {
        final byte[] encryptedData = connection.getEncryptionContext().encrypt(
                getBytes(buffer, buffer.readableBytes()));
        // We reset the reader index because we want to reuse the old buffer space which is still filled with the unencrypted data
        buffer.resetWriterIndex();
        writeInt(application, buffer, 0x3);
        final int size = encryptedData.length;
        correctSize(buffer, 4 + size, application.getBuffer());
        buffer.writeInt(size);
        buffer.writeBytes(encryptedData);
    }

    @Override
    protected Void read(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ConnectionImpl connection,
                        @AssumeNotNull ByteBuf buffer) {
        return null;
    }

    @Override
    protected void write(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ByteBuf buffer, Void empty) {
    }

}
