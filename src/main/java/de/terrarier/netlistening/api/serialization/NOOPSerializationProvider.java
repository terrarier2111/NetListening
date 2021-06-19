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

/**
 * @author Terrarier2111
 * @since 1.10
 */
public class NOOPSerializationProvider extends SerializationProvider {

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
    protected final boolean isSerializable(@AssumeNotNull Object obj) {
        return false;
    }

    /**
     * @see SerializationProvider#isDeserializable(ReadableByteAccumulation)
     */
    @Override
    protected final boolean isDeserializable(@AssumeNotNull ReadableByteAccumulation data) {
        return false;
    }

    /**
     * @see SerializationProvider#serialize(WritableByteAccumulation, Object)
     */
    @Override
    protected final void serialize(@AssumeNotNull WritableByteAccumulation data, @AssumeNotNull Object obj) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see SerializationProvider#deserialize(ReadableByteAccumulation)
     */
    @Override
    protected final Object deserialize(@AssumeNotNull ReadableByteAccumulation data) {
        throw new UnsupportedOperationException();
    }

}
