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

import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.internals.AssumeNotNull;
import de.terrarier.netlistening.utils.ByteBufUtilExtension;
import io.netty.buffer.ByteBuf;

/**
 * @author Terrarier2111
 * @since 1.10
 */
public final class WritableByteAccumulation {

    private final ApplicationImpl application;
    private final ByteBuf buffer;
    private final int initialWriterIdx;

    public WritableByteAccumulation(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ByteBuf buffer) {
        this.application = application;
        this.buffer = buffer;
        ByteBufUtilExtension.correctSize(buffer, 4, application.getBuffer());
        buffer.writeInt(0);
        initialWriterIdx = buffer.writerIndex();
    }

    /**
     * @return the buffer which poses as the actual container which the data gets written to.
     */
    @AssumeNotNull
    public ByteBuf getBuffer() {
        return buffer;
    }

    /**
     * Sets the array which is regarded as the entire data which is added to this accumulation.
     *
     * @param bytes the bytes which are added to this accumulation.
     */
    public void setArray(@AssumeNotNull byte[] bytes) {
        final int additionalSize = bytes.length - (buffer.writerIndex() - initialWriterIdx);
        buffer.writerIndex(initialWriterIdx);
        if(additionalSize > 0) {
            ensureWritable(additionalSize);
        }
        buffer.writeBytes(bytes);
    }

    /**
     * Corrects the size of the buffer such that it has space for at least {@code bytes}.
     *
     * @param bytes the bytes which should be writable to the buffer.
     */
    public void ensureWritable(int bytes) {
        ByteBufUtilExtension.correctSize(buffer, bytes, application.getBuffer());
    }

    void rollback() {
        buffer.setInt(initialWriterIdx - 4, 0);
        buffer.writerIndex(initialWriterIdx);
    }

    void updateLength() {
        buffer.setInt(initialWriterIdx - 4, buffer.writerIndex() - initialWriterIdx);
    }

}
