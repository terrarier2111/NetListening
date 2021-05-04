package de.terrarier.netlistening.impl;

import de.terrarier.netlistening.Application;
import de.terrarier.netlistening.api.PacketCaching;
import de.terrarier.netlistening.api.compression.CompressionSetting;
import de.terrarier.netlistening.api.encryption.EncryptionSetting;
import de.terrarier.netlistening.api.event.DataHandler;
import de.terrarier.netlistening.api.event.EventManager;
import de.terrarier.netlistening.api.event.Listener;
import de.terrarier.netlistening.api.event.ListenerType;
import de.terrarier.netlistening.api.serialization.JavaIoSerializationProvider;
import de.terrarier.netlistening.api.serialization.SerializationProvider;
import de.terrarier.netlistening.internals.AssumeNotNull;
import de.terrarier.netlistening.network.PacketCache;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static de.terrarier.netlistening.utils.ObjectUtilFallback.checkPositiveOrZero;

/**
 * @since 1.02
 * @author Terrarier2111
 */
public abstract class ApplicationImpl implements Application {

    final PacketCache cache = new PacketCache();
    final DataHandler handler = new DataHandler();
    final EventManager eventManager = new EventManager(handler);
    Charset stringEncoding = StandardCharsets.UTF_8;
    int buffer = 256;
    EncryptionSetting encryptionSetting;
    CompressionSetting compressionSetting;
    SerializationProvider serializationProvider = new JavaIoSerializationProvider();
    Thread worker;
    EventLoopGroup group;

    /**
     * @see Application#getStringEncoding()
     */
    @ApiStatus.Internal
    @AssumeNotNull
    @Override
    public final Charset getStringEncoding() {
        return stringEncoding;
    }

    /**
     * @return the packet cache is used by the application to map packet ids
     * to packet content.
     */
    @ApiStatus.Internal
    @AssumeNotNull
    public final PacketCache getCache() {
        return cache;
    }

    /**
     * @return the caching mode is used to cache packets.
     */
    @AssumeNotNull
    public abstract PacketCaching getCaching();

    /**
     * @return the buffer size which is added on top of the required space,
     * every time a buffer is expanded.
     */
    public final int getBuffer() {
        return buffer;
    }

    /**
     * @return the encryption settings which should be used to encrypt traffic
     * which was marked to get encrypted.
     */
    public final EncryptionSetting getEncryptionSetting() {
        return encryptionSetting;
    }

    /**
     * @return the compression setting containing information
     * about which compression techniques should be applied on
     * specific data.
     */
    @AssumeNotNull
    public final CompressionSetting getCompressionSetting() {
        return compressionSetting;
    }

    /**
     * @return the serialization provider which handles the serialization
     * of specific data.
     */
    @AssumeNotNull
    public final SerializationProvider getSerializationProvider() {
        return serializationProvider;
    }

    /**
     * @see Application#registerListener(Listener)
     */
    @Override
    public final long registerListener(@NotNull Listener<?> listener) {
        return eventManager.registerListener(listener);
    }

    /**
     * @see Application#unregisterListeners(ListenerType) 
     */
    @Deprecated
    @Override
    public final void unregisterListeners(@NotNull ListenerType listenerType) {
        eventManager.unregisterListeners(listenerType);
    }

    /**
     * @see Application#unregisterListener(long)
     */
    @Override
    public final void unregisterListener(long listenerId) {
        eventManager.unregisterListener(listenerId);
    }

    @ApiStatus.Internal
    @AssumeNotNull
    public final EventManager getEventManager() {
        return eventManager;
    }

    @ApiStatus.Internal
    static abstract class Builder<A extends ApplicationImpl, B extends Builder<A, B>> extends Application.Builder<A, B> {

        final A application;
        final Map<ChannelOption<?>, Object> options = new HashMap<>();
        long timeout;
        private boolean built;

        Builder(@AssumeNotNull A application) {
            this.application = application;
            // https://en.wikipedia.org/wiki/Type_of_service
            options.put(ChannelOption.IP_TOS, 0x18);
        }

        /**
         * @see Application.Builder#timeout(long)
         */
        @SuppressWarnings("unchecked")
        @AssumeNotNull
        public final B timeout(long timeout) {
            validate();
            checkPositiveOrZero(timeout, "timeout");
            this.timeout = timeout;
            return (B) this;
        }

        /**
         * @see Application.Builder#buffer(int)
         */
        @SuppressWarnings("unchecked")
        @AssumeNotNull
        public final B buffer(int buffer) {
            validate();
            checkPositiveOrZero(buffer, "buffer");
            application.buffer = buffer;
            return (B) this;
        }

        /**
         * @see Application.Builder#option(ChannelOption, Object)
         */
        @SuppressWarnings("unchecked")
        @AssumeNotNull
        public final <T> B option(@NotNull ChannelOption<T> option, T value) {
            validate();
            options.put(option, value);
            return (B) this;
        }

        /**
         * @see Application.Builder#serialization(SerializationProvider)
         */
        @SuppressWarnings("unchecked")
        @AssumeNotNull
        public final B serialization(@NotNull SerializationProvider serializationProvider) {
            validate();
            application.serializationProvider = serializationProvider;
            return (B) this;
        }

        /**
         * @see Application.Builder#build()
         */
        @AssumeNotNull
        public final A build() {
            validate();
            built = true;
            build0();
            return application;
        }

        abstract void build0();

        final void validate() {
            if(built) {
                throw new ApplicationAlreadyBuiltException();
            }
        }

        private static final class ApplicationAlreadyBuiltException extends IllegalStateException {

            private ApplicationAlreadyBuiltException() {
                super("The builder can't be used anymore because the application was already built!");
            }

        }

    }

}
