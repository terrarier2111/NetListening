package de.terrarier.netlistening.api.serialization;

import de.terrarier.netlistening.api.event.EventManager;
import de.terrarier.netlistening.api.event.ExceptionTrowEvent;
import de.terrarier.netlistening.internals.AssumeNotNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * @since 1.01
 * @author Terrarier2111
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
        if(fallback == null) {
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
    protected abstract boolean isDeserializable(@AssumeNotNull byte[] data);

    /**
     * Serializes an object and return a byte array that represents that
     * object which can later be deserialized into the object.
     *
     * @param obj the object to be serialized.
     * @return the byte array that represents the passed object.
     * @throws Exception if something unexpected happens.
     */
    protected abstract byte[] serialize(@AssumeNotNull Object obj) throws Exception;

    /**
     * Deserializes an object from a byte array that represents
     * that object.
     *
     * @param data the data to be deserialized to an object.
     * @return the object that was deserialized from the passed byte array.
     * @throws Exception if something unexpected happens.
     */
    protected abstract Object deserialize(@AssumeNotNull byte[] data) throws Exception;

    @ApiStatus.Internal
    public final void setEventManager(@AssumeNotNull EventManager eventManager) {
        if(this.eventManager == null) {
            this.eventManager = eventManager;
        }else {
            throw new IllegalStateException("The event manager was already set!");
        }
    }

    @ApiStatus.Internal
    protected final void handleException(@AssumeNotNull Exception exception) {
        final ExceptionTrowEvent event = new ExceptionTrowEvent(new SerializationException(exception));
        eventManager.handleExceptionThrown(event);
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
                if(stackTraceElements == null) {
                    return cause.fillInStackTrace().getStackTrace();
                }
                return stackTraceElements;
            }finally {
                cause.fillInStackTrace();
            }
        }
    }

}
