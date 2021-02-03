package de.terrarier.netlistening.api.serialization;

import de.terrarier.netlistening.api.event.EventManager;
import de.terrarier.netlistening.api.event.ExceptionTrowEvent;
import de.terrarier.netlistening.api.event.ListenerType;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.01
 * @author Terrarier2111
 */
public abstract class SerializationProvider {

    public static final JavaIoSerializationProvider DEFAULT_SERIALIZATION_PROVIDER = new JavaIoSerializationProvider();
    private EventManager eventManager;

    /**
     * @return a fallback serialization provider to use when an
     * object is not serializable by the underlying implementation
     * if there is no fallback, return null.
     */
    protected SerializationProvider getFallback() {
        return DEFAULT_SERIALIZATION_PROVIDER;
    }

    /**
     * @param obj the object to check.
     * @return whether or not the underlying implementation can serialize the
     * passed object.
     */
    protected abstract boolean isSerializable(Object obj);

    /**
     * @param data the byte array to check.
     * @return whether or not the underlying implementation can deserialize the
     * passed byte array.
     */
    protected abstract boolean isDeserializable(byte[] data);

    /**
     * Serializes an object and return a byte array that represents that
     * object which can later be deserialized into the object.
     *
     * @param obj the object to be serialized.
     * @return the byte array that represents the passed object.
     * @throws Exception if something unexpected happens.
     */
    protected abstract byte[] serialize(Object obj) throws Exception;

    /**
     * Deserializes an object from a byte array that represents
     * this object.
     *
     * @param data the data to be deserialized to an object.
     * @return the object that was deserialized from the passed byte array.
     * @throws Exception if something unexpected happens.
     */
    protected abstract Object deserialize(byte[] data) throws Exception;

    public final void setEventManager(@NotNull EventManager eventManager) {
        if(this.eventManager == null) {
            this.eventManager = eventManager;
        }else {
            throw new IllegalStateException("The event manager was already set!");
        }
    }

    protected final void handleException(@NotNull Exception exception) {
        final ExceptionTrowEvent event = new ExceptionTrowEvent(new SerializationException(exception));
        eventManager.callEvent(ListenerType.EXCEPTION_THROW, event);
        if (event.isPrint()) {
            event.getException().printStackTrace();
        }
    }

    public static final class SerializationException extends Exception {

        private transient final Exception cause;

        private SerializationException(@NotNull Exception cause) {
            this.cause = cause;
        }

        @NotNull
        public Exception getCause() {
            return cause;
        }

        @NotNull
        @Override
        public Throwable initCause(Throwable cause) {
            return this;
        }

        @NotNull
        @Override
        public Throwable fillInStackTrace() {
            return this;
        }

        @Override
        public StackTraceElement[] getStackTrace() {
            try {
                final StackTraceElement[] stackTraceElements = cause.getStackTrace();
                if(stackTraceElements == null) {
                    cause.fillInStackTrace();
                    return cause.getStackTrace();
                }
                return stackTraceElements;
            }finally {
                cause.fillInStackTrace();
            }
        }
    }

}
