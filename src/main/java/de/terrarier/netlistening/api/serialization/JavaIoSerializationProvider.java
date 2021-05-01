package de.terrarier.netlistening.api.serialization;

import de.terrarier.netlistening.internals.AssumeNotNull;
import de.terrarier.netlistening.utils.ConversionUtil;

import java.io.*;

import static java.io.ObjectStreamConstants.*;

/**
 * @since 1.01
 * @author Terrarier2111
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
    protected boolean isDeserializable(byte[] data) {
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
    protected Object deserialize(byte[] data) throws Exception {
        try (ByteArrayInputStream in = new ByteArrayInputStream(data)) {
            final ObjectInputStream is = new ObjectInputStream(in);
            return is.readObject();
        }
    }

}
