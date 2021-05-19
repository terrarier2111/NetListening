package de.terrarier.netlistening.api.serialization;

import de.terrarier.netlistening.internals.AssumeNotNull;

/**
 * @author Terrarier2111
 * @since 1.10
 */
public final class NOOPSerializationProvider extends SerializationProvider {

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
    protected boolean isSerializable(Object obj) {
        return false;
    }

    /**
     * @see SerializationProvider#isDeserializable(byte[])
     */
    @Override
    protected boolean isDeserializable(@AssumeNotNull byte[] data) {
        return false;
    }

    /**
     * @see SerializationProvider#serialize(Object)
     */
    @Override
    protected byte[] serialize(Object obj) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see SerializationProvider#deserialize(byte[])
     */
    @Override
    protected Object deserialize(@AssumeNotNull byte[] data) {
        throw new UnsupportedOperationException();
    }

}
