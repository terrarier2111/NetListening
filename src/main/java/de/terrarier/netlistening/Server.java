package de.terrarier.netlistening;

import de.terrarier.netlistening.api.PacketCaching;
import de.terrarier.netlistening.api.compression.CompressionSetting;
import de.terrarier.netlistening.api.compression.CompressionSettingWrapper;
import de.terrarier.netlistening.api.encryption.EncryptionSetting;
import de.terrarier.netlistening.api.encryption.EncryptionSettingWrapper;
import de.terrarier.netlistening.api.serialization.SerializationProvider;
import de.terrarier.netlistening.impl.ServerImpl;
import de.terrarier.netlistening.internals.AssumeNotNull;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;

import static de.terrarier.netlistening.utils.ObjectUtilFallback.checkPositive;
import static de.terrarier.netlistening.utils.ObjectUtilFallback.checkPositiveOrZero;

/**
 * @author Terrarier2111
 * @since 1.0
 */
public interface Server extends Application {

    /**
     * Maps a specific id to the connection being identified with this id.
     *
     * @param id the id of the requested connection.
     * @return the connection which is identified with the passed id and
     * if not available, null.
     */
    Connection getConnection(int id);

    /**
     * Maps a specific channel to the connection wrapping the specified channel.
     *
     * @param channel the channel which underlies the connection.
     * @return the connection which wraps the passed channel and
     * if not available, null.
     */
    Connection getConnection(@NotNull Channel channel);

    /**
     * Creates a new builder with the passed arguments.
     *
     * @param port the port the server should bind/listen to.
     * @return the new builder.
     */
    @AssumeNotNull
    static Builder builder(int port) {
        return new Builder(port);
    }

    final class Builder extends Application.Builder<Server, Builder> {

        private final ServerImpl.Builder impl;

        public Builder(int port) {
            impl = new ServerImpl.Builder(checkPositive(port, "port"));
        }

        /**
         * Sets the caching rule which should be used to cache packets.
         *
         * @param caching the caching mode.
         * @return the local reference.
         */
        @AssumeNotNull
        public Builder caching(@NotNull PacketCaching caching) {
            impl.caching(caching);
            return this;
        }

        /**
         * @see Application.Builder#timeout(long)
         */
        @AssumeNotNull
        @Override
        public Builder timeout(long timeout) {
            impl.timeout(checkPositiveOrZero(timeout, "timeout"));
            return this;
        }

        /**
         * @see Application.Builder#buffer(int)
         */
        @AssumeNotNull
        @Override
        public Builder buffer(int buffer) {
            impl.buffer(checkPositiveOrZero(buffer, "buffer"));
            return this;
        }

        /**
         * Sets the encoding which should be used to encode/decode strings.
         *
         * @param charset the charset which should be used to encode/decode strings.
         * @return the local reference.
         */
        @AssumeNotNull
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
        @AssumeNotNull
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
        @AssumeNotNull
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
        @AssumeNotNull
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
        @AssumeNotNull
        public EncryptionSettingWrapper encryption() {
            return new EncryptionSettingWrapper(this);
        }

        /**
         * @see Application.Builder#option(ChannelOption, Object)
         */
        @AssumeNotNull
        @Override
        public <T> Builder option(@NotNull ChannelOption<T> option, T value) {
            impl.option(option, value);
            return this;
        }

        /**
         * @see Application.Builder#serialization(SerializationProvider)
         */
        @AssumeNotNull
        @Override
        public Builder serialization(@NotNull SerializationProvider serializationProvider) {
            impl.serialization(serializationProvider);
            return this;
        }

        /**
         * @see Application.Builder#build()
         */
        @AssumeNotNull
        @Override
        public Server build() {
            return impl.build();
        }

    }

}
