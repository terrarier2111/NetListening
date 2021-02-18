package de.terrarier.netlistening.impl;

import de.terrarier.netlistening.Application;
import de.terrarier.netlistening.api.compression.CompressionSetting;
import de.terrarier.netlistening.api.encryption.EncryptionSetting;
import de.terrarier.netlistening.api.event.DataHandler;
import de.terrarier.netlistening.api.event.EventManager;
import de.terrarier.netlistening.api.event.Listener;
import de.terrarier.netlistening.api.event.ListenerType;
import de.terrarier.netlistening.api.serialization.JavaIoSerializationProvider;
import de.terrarier.netlistening.api.serialization.SerializationProvider;
import de.terrarier.netlistening.network.PacketCache;
import de.terrarier.netlistening.network.PacketSynchronization;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @since 1.02
 * @author Terrarier2111
 */
public abstract class ApplicationImpl implements Application {

    protected final PacketCache cache = new PacketCache();
    protected final DataHandler handler = new DataHandler(this);
    protected final EventManager eventManager = new EventManager(handler);
    protected PacketSynchronization packetSynchronization = PacketSynchronization.NONE;
    protected Charset stringEncoding = StandardCharsets.UTF_8;
    protected int buffer = 256;
    protected EncryptionSetting encryptionSetting;
    protected CompressionSetting compressionSetting;
    protected SerializationProvider serializationProvider = new JavaIoSerializationProvider();
    protected Thread worker;
    protected EventLoopGroup group;

    /**
     * @see Application
     */
    @NotNull
    @Override
    public final PacketSynchronization getPacketSynchronization() {
        return packetSynchronization;
    }

    /**
     * @see Application
     */
    @NotNull
    @Override
    public final Charset getStringEncoding() {
        return stringEncoding;
    }

    /**
     * @see Application
     */
    @NotNull
    @Override
    public final PacketCache getCache() {
        return cache;
    }

    /**
     * @see Application
     */
    @Override
    public final int getBuffer() {
        return buffer;
    }

    /**
     * @see Application
     */
    @Override
    public final EncryptionSetting getEncryptionSetting() {
        return encryptionSetting;
    }

    /**
     * @see Application
     */
    @NotNull
    @Override
    public final CompressionSetting getCompressionSetting() {
        return compressionSetting;
    }

    /**
     * @see Application
     */
    @NotNull
    @Override
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

        protected final A application;
        protected final Map<ChannelOption<?>, Object> options = new HashMap<>();
        protected long timeout;
        private boolean built;

        public Builder(@NotNull A application) {
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

        protected abstract void build0();

        protected final void validate() {
            if(built)
                fail();
        }

        protected abstract void fail();

    }

}
