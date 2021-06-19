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
import io.netty.util.internal.EmptyArrays;

/**
 * @author Terrarier2111
 * @since 1.10
 */
public final class ReadableByteAccumulation {

    private final ByteBuf buffer;
    private final int length;
    private byte[] bytes;

    public ReadableByteAccumulation(@AssumeNotNull ByteBuf buffer, int length) {
        this.buffer = buffer;
        this.length = length;
    }

    /**
     * @return the buffer which poses as the actual container which the data gets read from.
     */
    @AssumeNotNull
    public ByteBuf getBuffer() {
        return buffer;
    }

    /**
     * @return the length of the bytes which should be read.
     */
    public int getLength() {
        return length;
    }

    /**
     * @return a byte array of length {@code length}.
     */
    @AssumeNotNull
    public byte[] getArray() {
        if (bytes == null) {
            if (length == 0) {
                bytes = EmptyArrays.EMPTY_BYTES;
            } else {
                bytes = ByteBufUtilExtension.readBytes(buffer, length);
            }
        }
        return bytes;
    }

}
