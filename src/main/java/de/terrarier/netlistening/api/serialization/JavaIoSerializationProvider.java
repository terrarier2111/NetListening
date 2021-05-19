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

import de.terrarier.netlistening.internals.AssumeNotNull;
import de.terrarier.netlistening.utils.ConversionUtil;

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
     * @see SerializationProvider#isDeserializable(byte[])
     */
    @Override
    protected boolean isDeserializable(@AssumeNotNull byte[] data) {
        // Common stream header hex: ACED0005
        final int dataLength = data.length;
        return (dataLength > 5 || (dataLength == 5 && data[4] != TC_NULL))
                && ConversionUtil.getShortFromByteArray(data, 0) == STREAM_MAGIC
                && ConversionUtil.getShortFromByteArray(data, 2) == STREAM_VERSION;
    }

    /**
     * @see SerializationProvider#serialize(Object)
     */
    @Override
    protected byte[] serialize(@AssumeNotNull Object obj) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            final ObjectOutputStream os = new ObjectOutputStream(out);
            os.writeObject(obj);
            return out.toByteArray();
        }
    }

    /**
     * @see SerializationProvider#deserialize(byte[])
     */
    @Override
    protected Object deserialize(@AssumeNotNull byte[] data) throws Exception {
        try (ByteArrayInputStream in = new ByteArrayInputStream(data)) {
            final ObjectInputStream is = new ObjectInputStream(in);
            return is.readObject();
        }
    }

}
