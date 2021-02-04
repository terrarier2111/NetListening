package de.terrarier.netlistening.api.serialization;

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
        // No special conditions to check here so always return true.
        // TODO: Look for possible conditions which could be checked.
        return true;
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
