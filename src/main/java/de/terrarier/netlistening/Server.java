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

    final class Builder extends Application.Builder<Server, Builder> {

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
         * @see Application.Builder
         */
        @NotNull
        public Builder timeout(long timeout) {
            impl.timeout(timeout);
            return this;
        }

        /**
         * @see Application.Builder
         */
        @NotNull
        public Builder buffer(int buffer) {
            impl.buffer(buffer);
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
         * @see Application.Builder
         */
        @NotNull
        public <T> Builder option(@NotNull ChannelOption<T> option, T value) {
            impl.option(option, value);
            return this;
        }

        /**
         * @see Application.Builder
         */
        @NotNull
        public Builder serialization(@NotNull SerializationProvider serializationProvider) {
            impl.serialization(serializationProvider);
            return this;
        }

        /**
         * @see Application.Builder
         */
        @NotNull
        public Server build() {
            return impl.build();
        }

    }

}
