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

import de.terrarier.netlistening.api.event.EventManager;
import de.terrarier.netlistening.internals.AssumeNotNull;
import de.terrarier.netlistening.utils.ByteBufUtilExtension;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.ApiStatus;

/**
 * @author Terrarier2111
 * @since 1.01
 */
public abstract class SerializationProvider {

    public static final Object SERIALIZATION_ERROR = new Object();
    protected SerializationProvider fallback;
    private EventManager eventManager;

    /**
     * @return a fallback serialization provider to use when an
     * object is not serializable by the underlying implementation
     * if there is no fallback, return null.
     */
    protected SerializationProvider getFallback() {
        if (fallback == null) {
            fallback = new JavaIoSerializationProvider();
            fallback.setEventManager(eventManager);
        }
        return fallback;
    }

    /**
     * @param obj the object to check.
     * @return whether or not the underlying implementation can serialize the
     * passed object.
     */
    protected abstract boolean isSerializable(@AssumeNotNull Object obj);

    /**
     * @param data the byte array to check.
     * @return whether or not the underlying implementation can deserialize the
     * passed byte array.
     */
    protected abstract boolean isDeserializable(@AssumeNotNull ReadableByteAccumulation data);

    /**
     * Serializes an object and return a byte array that represents that
     * object which can later be deserialized into the object.
     *
     * @param obj the object to be serialized.
     * @throws Exception if something unexpected happens.
     */
    protected abstract void serialize(@AssumeNotNull WritableByteAccumulation data, @AssumeNotNull Object obj) throws Exception;

    /**
     * Deserializes an object from a byte array that represents
     * that object.
     *
     * @param data the data to be deserialized to an object.
     * @return the object that was deserialized from the passed byte array.
     * @throws Exception if something unexpected happens.
     */
    protected abstract Object deserialize(@AssumeNotNull ReadableByteAccumulation data) throws Exception;

    /**
     * Reads {@code length} bytes from the {@code buffer} and returns them as a byte array.
     *
     * @param buffer the buffer from which the bytes should get read.
     * @param length the number of bytes which should be read from the buffer.
     * @return the bytes which were read from the buffer.
     */
    @AssumeNotNull
    protected static byte[] readBytes(@AssumeNotNull ByteBuf buffer, int length) {
        return ByteBufUtilExtension.readBytes(buffer, length);
    }

    /**
     * Writes {@code data} to {@code buffer}.
     *
     * @param buffer the buffer to which {@code data} gets written to.
     * @param data   the data which gets written to {@code buffer}.
     */
    protected static void writeBytes(@AssumeNotNull ByteBuf buffer, @AssumeNotNull byte[] data) {
        ensureWritable(buffer, data.length);
        buffer.writeBytes(data);
    }

    /**
     * Corrects the size of the buffer such that it has space for at least {@code bytes}.
     *
     * @param bytes the bytes which should be writable to the buffer.
     */
    protected static void ensureWritable(@AssumeNotNull ByteBuf buffer, int bytes) {
        ByteBufUtilExtension.correctSize(buffer, bytes, 0);
    }

    @ApiStatus.Internal
    public final void setEventManager(@AssumeNotNull EventManager eventManager) {
        if (this.eventManager != null) {
            throw new IllegalStateException("The event manager was already set!");
        }
        this.eventManager = eventManager;
    }

    @ApiStatus.Internal
    protected final void handleException(@AssumeNotNull Exception exception) {
        eventManager.handleExceptionThrown(new SerializationException(exception));
    }

    @ApiStatus.Internal
    public static final class SerializationException extends Exception {

        private transient final Exception cause;

        private SerializationException(@AssumeNotNull Exception cause) {
            this.cause = cause;
        }

        @AssumeNotNull
        @Override
        public Exception getCause() {
            return cause;
        }

        @AssumeNotNull
        @Override
        public Throwable initCause(Throwable cause) {
            return this;
        }

        @AssumeNotNull
        @Override
        public Throwable fillInStackTrace() {
            return this;
        }

        @Override
        public StackTraceElement[] getStackTrace() {
            try {
                final StackTraceElement[] stackTraceElements = cause.getStackTrace();
                if (stackTraceElements == null) {
                    return cause.fillInStackTrace().getStackTrace();
                }
                return stackTraceElements;
            } finally {
                cause.fillInStackTrace();
            }
        }
    }

}
