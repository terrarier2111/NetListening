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
import de.terrarier.netlistening.network.PacketCache;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @since 1.02
 * @author Terrarier2111
 */
@ApiStatus.Internal
public abstract class ApplicationImpl implements Application {

    final PacketCache cache = new PacketCache();
    final DataHandler handler = new DataHandler(this);
    final EventManager eventManager = new EventManager(handler);
    Charset stringEncoding = StandardCharsets.UTF_8;
    int buffer = 256;
    EncryptionSetting encryptionSetting;
    CompressionSetting compressionSetting;
    SerializationProvider serializationProvider = new JavaIoSerializationProvider();
    Thread worker;
    EventLoopGroup group;

    /**
     * @see Application
     */
    @NotNull
    @Override
    public final Charset getStringEncoding() {
        return stringEncoding;
    }

    /**
     * @return the packet cache used by the application to map packet ids
     * to packet content.
     */
    @NotNull
    public final PacketCache getCache() {
        return cache;
    }

    /**
     * @return the caching mode used to cache packets.
     */
    @NotNull
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
     * which was marked to encrypt.
     */
    public final EncryptionSetting getEncryptionSetting() {
        return encryptionSetting;
    }

    /**
     * @return the compression setting containing information
     * about which compression techniques should be applied on
     * specific data.
     */
    @NotNull
    public final CompressionSetting getCompressionSetting() {
        return compressionSetting;
    }

    /**
     * @return the serialization provider which handles the serialization
     * of specific data.
     */
    @NotNull
    public final SerializationProvider getSerializationProvider() {
        return serializationProvider;
    }

    /**
     * @see Application
     */
    @Override
    public final void registerListener(@NotNull Listener<?> listener) {
        eventManager.registerListener(listener);
    }

    /**
     * @see Application
     */
    @Override
    public final void unregisterListeners(@NotNull ListenerType listenerType) {
        eventManager.unregisterListeners(listenerType);
    }

    @NotNull
    public final EventManager getEventManager() {
        return eventManager;
    }

    public static abstract class Builder<A extends ApplicationImpl, B extends Builder<A, B>> extends Application.Builder<A, B> {

        final A application;
        final Map<ChannelOption<?>, Object> options = new HashMap<>();
        long timeout;
        private boolean built;

        Builder(@NotNull A application) {
            this.application = application;
            // https://en.wikipedia.org/wiki/Type_of_service
            options.put(ChannelOption.IP_TOS, 0x18);
        }

        @SuppressWarnings("unchecked")
        @NotNull
        public final B timeout(long timeout) {
            validate();
            this.timeout = timeout;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        @NotNull
        public final B buffer(int buffer) {
            validate();
            application.buffer = buffer;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        @NotNull
        public final <T> B option(@NotNull ChannelOption<T> option, T value) {
            validate();
            options.put(option, value);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        @NotNull
        public final B serialization(@NotNull SerializationProvider serializationProvider) {
            validate();
            application.serializationProvider = serializationProvider;
            return (B) this;
        }

        @NotNull
        public final A build() {
            validate();
            built = true;
            build0();
            return application;
        }

        abstract void build0();

        final void validate() {
            if(built)
                fail();
        }

        abstract void fail();

    }

}
