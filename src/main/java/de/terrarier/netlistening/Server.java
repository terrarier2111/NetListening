package de.terrarier.netlistening;

import de.terrarier.netlistening.api.DataComponent;
import de.terrarier.netlistening.api.DataContainer;
import de.terrarier.netlistening.api.PacketCaching;
import de.terrarier.netlistening.api.compression.CompressionSetting;
import de.terrarier.netlistening.api.compression.CompressionSettingWrapper;
import de.terrarier.netlistening.api.encryption.EncryptionSetting;
import de.terrarier.netlistening.api.encryption.EncryptionSettingWrapper;
import de.terrarier.netlistening.api.serialization.SerializationProvider;
import de.terrarier.netlistening.impl.ServerImpl;
import de.terrarier.netlistening.network.PacketSynchronization;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public interface Server extends Application {

    /**
     * @see Application
     */
    @Override
    Connection getConnection(@NotNull Channel channel);

    /**
     * @see Application
     */
    @Override
    void sendData(@NotNull DataContainer data, @NotNull Connection connection);

    /**
     * @see Application
     */
    @Deprecated
    @Override
    void sendData(@NotNull DataComponent<?> data, @NotNull Connection connection);

    /**
     * @see Application
     */
    @Override
    void disconnect(@NotNull Connection connection);

    /**
     * @see Application
     */
    @Override
    default boolean isClient() {
        return false;
    }

    class Builder {

        private final ServerImpl.Builder impl;

        public Builder(int port) {
            this.impl = new ServerImpl.Builder(port);
        }

        /**
         * Sets the caching rule which should be used to cache packets.
         *
         * @param caching the caching mode.
         * @return the local reference.
         */
        @NotNull
        public Builder caching(@NotNull PacketCaching caching) {
            impl.caching(caching);
            return this;
        }

        /**
         * Sets a specific read timeout for the connection, and automatically writes
         * data to all connections ever timeout / 2 milliseconds
         *
         * @param timeout the amount of milliseconds in which any data should be received.
         * @return the local reference.
         */
        @NotNull
        public Builder timeout(long timeout) {
            impl.timeout(timeout);
            return this;
        }

        /**
         * Sets the buffer size which is added on top of the required space,
         * every time a buffer is expanded.
         *
         * @param buffer the additional size added to the buffer.
         * @return the local reference.
         */
        @NotNull
        public Builder buffer(int buffer) {
            impl.buffer(buffer);
            return this;
        }

        /**
         * Sets the packet synchronization which should be used to synchronize
         * packets between different connections.
         *
         * @param packetSynchronization the synchronization mode.
         * @return the local reference.
         */
        @NotNull
        public Builder packetSynchronization(@NotNull PacketSynchronization packetSynchronization) {
            impl.packetSynchronization(packetSynchronization);
            return this;
        }

        /**
         * Sets the encoding which should be used to encode/decode Strings.
         *
         * @param charset the charset which should be used to encode/decode Strings.
         * @return the local reference.
         */
        @NotNull
        public Builder stringEncoding(@NotNull Charset charset) {
            impl.stringEncoding(charset);
            return this;
        }

        /**
         * Sets the compression setting which should be used to compress
         * specific data.
         *
         * @param compressionSetting the compression setting which should be used.
         * @return the local reference.
         */
        @NotNull
        public Builder compression(@NotNull CompressionSetting compressionSetting) {
            impl.compression(compressionSetting);
            return this;
        }

        /**
         * Creates a compression setting wrapper which can be used to
         * adjust the compression settings.
         *
         * @return a compression setting wrapper.
         */
        @NotNull
        public CompressionSettingWrapper compression() {
            return new CompressionSettingWrapper(this);
        }

        /**
         * Sets the encryption setting which should be used to encrypt traffic
         * which was marked to encrypt.
         *
         * @param encryptionSetting the setting which should be used to encrypt specific traffic.
         * @return the local reference.
         */
        @NotNull
        public Builder encryption(@NotNull EncryptionSetting encryptionSetting) {
            impl.encryption(encryptionSetting);
            return this;
        }

        /**
         * Creates an encryption setting wrapper which can be used to
         * adjust the encryption settings.
         *
         * @return an encryption setting wrapper.
         */
        @NotNull
        public EncryptionSettingWrapper encryption() {
            return new EncryptionSettingWrapper(this);
        }

        /**
         * Sets a specific option of the channel to a specific
         * value when the channel gets opened!
         *
         * @param option the option to be set.
         * @param value the value to be assigned to the option.
         * @param <T> the type of the option.
         * @return the local reference.
         */
        @NotNull
        public <T> Builder option(@NotNull ChannelOption<T> option, T value) {
            impl.option(option, value);
            return this;
        }

        /**
         * Sets the serialization provider which is to be used to
         * perform serialization operations.
         *
         * @param serializationProvider the serialization provider which provides
         * an implementation for serialization operations.
         * @return the local reference.
         */
        @NotNull
        public Builder serialization(@NotNull SerializationProvider serializationProvider) {
            impl.serialization(serializationProvider);
            return this;
        }

        /**
         * Builds the server, sets its default values and starts it.
         *
         * @return the started server.
         */
        @NotNull
        public Server build() {
            return impl.build();
        }

    }

}
