package de.terrarier.netlistening.api.serialization;

import de.terrarier.netlistening.utils.ConversionUtil;
import org.jetbrains.annotations.NotNull;

import java.io.*;

/**
 * @since 1.01
 * @author Terrarier2111
 */
public final class JavaIoSerializationProvider extends SerializationProvider {

    /**
     * @see SerializationProvider
     */
    @Override
    public SerializationProvider getFallback() {
        return null;
    }

    /**
     * @see SerializationProvider
     */
    @Override
    public boolean isSerializable(@NotNull Object obj) {
        return obj instanceof Serializable;
    }

    /**
     * @see SerializationProvider
     */
    @Override
    public boolean isDeserializable(byte[] data) {
        // Common stream header hex: ACED0005
        final int dataLength = data.length;
        return (dataLength > 5 || (dataLength == 5 && data[4] != ObjectStreamConstants.TC_NULL))
                && ConversionUtil.getShortFromByteArray(data, 0) == ObjectStreamConstants.STREAM_MAGIC
                && ConversionUtil.getShortFromByteArray(data, 2) == ObjectStreamConstants.STREAM_VERSION;
    }

    /**
     * @see SerializationProvider
     */
    @Override
    public byte[] serialize(@NotNull Object obj) throws Exception {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            final ObjectOutputStream os = new ObjectOutputStream(out);
            os.writeObject(obj);
        }finally {
            out.close();
        }
        return out.toByteArray();
    }

    /**
     * @see SerializationProvider
     */
    @Override
    public Object deserialize(byte[] data) throws Exception {
        try (ByteArrayInputStream in = new ByteArrayInputStream(data)) {
            final ObjectInputStream is = new ObjectInputStream(in);
            return is.readObject();
        }
    }
}
