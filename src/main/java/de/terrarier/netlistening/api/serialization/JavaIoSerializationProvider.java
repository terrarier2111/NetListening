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
package de.terrarier.netlistening.api.serialization;

import de.terrarier.netlistening.internal.AssumeNotNull;
import de.terrarier.netlistening.util.ByteBufUtilExtension;
import io.netty.buffer.ByteBuf;

import java.io.*;

import static java.io.ObjectStreamConstants.*;

/**
 * @author Terrarier2111
 * @since 1.01
 */
public final class JavaIoSerializationProvider extends SerializationProvider {

    /**
     * @see SerializationProvider#getFallback()
     */
    @Override
    protected SerializationProvider getFallback() {
        return null;
    }

    /**
     * @see SerializationProvider#isSerializable(Object)
     */
    @Override
    protected boolean isSerializable(@AssumeNotNull Object obj) {
        return obj instanceof Serializable;
    }

    /**
     * @see SerializationProvider#isDeserializable(ReadableByteAccumulation)
     */
    @Override
    protected boolean isDeserializable(@AssumeNotNull ReadableByteAccumulation ba) {
        // Common stream header hex: ACED0005
        final ByteBuf buffer = ba.getBuffer();
        final int dataLength = buffer.readableBytes();
        return (dataLength > 5 || (dataLength == 5 && buffer.getByte(buffer.readerIndex() + 4) != TC_NULL))
                && buffer.getShort(buffer.readerIndex()) == STREAM_MAGIC
                && buffer.getShort(buffer.readerIndex() + 2) == STREAM_VERSION;
    }

    /**
     * @see SerializationProvider#serialize(WritableByteAccumulation, Object)
     */
    @Override
    protected void serialize(@AssumeNotNull WritableByteAccumulation ba, @AssumeNotNull Object obj) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            final ObjectOutputStream os = new ObjectOutputStream(out);
            os.writeObject(obj);
            final byte[] bytes = out.toByteArray();
            final ByteBuf buffer = ba.getBuffer();
            ByteBufUtilExtension.correctSize(buffer, bytes.length, 0);
            buffer.writeBytes(bytes);
        }
    }

    /**
     * @see SerializationProvider#deserialize(ReadableByteAccumulation)
     */
    @Override
    protected Object deserialize(@AssumeNotNull ReadableByteAccumulation ba) throws Exception {
        final ByteBuf buffer = ba.getBuffer();
        try (ByteArrayInputStream in = new ByteArrayInputStream(ByteBufUtilExtension.readBytes(buffer,
                buffer.readableBytes()))) {
            final ObjectInputStream is = new ObjectInputStream(in);
            return is.readObject();
        }
    }

}
