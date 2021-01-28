package de.terrarier.netlistening;

import de.terrarier.netlistening.api.encryption.ServerKey;
import de.terrarier.netlistening.api.encryption.hash.HashingAlgorithm;
import de.terrarier.netlistening.api.proxy.ProxyType;
import de.terrarier.netlistening.impl.ClientImpl;
import io.netty.channel.ChannelOption;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public interface Client extends Application {

    /**
     * @return the key used by the server to encrypt the symmetric key.
     */
    ServerKey getServerKey();

    /**
     * Sets the expected key used by the server to encrypt the symmetric key.
     * If the key set is different to the key received by the server,
     * a KeyChangeEvent with an HASH_CHANGED action is getting called,
     * which can be used to detect MITM attacks.
     *
     * @param data the data representing the expected key.
     * @return whether or not setting the key was successful.
     */
    boolean setServerKey(byte[] data);

    /**
     * @see Application
     */
    @Override
    default boolean isClient() {
        return true;
    }

    class Builder {

        private final ClientImpl.Builder impl;

        public Builder(@NotNull String host, int remotePort) {
            this(new InetSocketAddress(host, remotePort));
        }

        public Builder(@NotNull SocketAddress remoteAddress) {
            this.impl = new ClientImpl.Builder(remoteAddress);
        }

        /**
         * Sets a specific read timeout for the connection, and automatically writes
         * data to the server ever timeout / 2 milliseconds
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
         * Sets the source port of the client.
         *
         * @param localPort the port the client is getting bound to.
         * @return the local reference.
         */
        @NotNull
        public Builder localPort(int localPort) {
            impl.localPort(localPort);
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
         * Sets the key hashing algorithm used to hash the public
         * key provided by the server.
         *
         * @param hashingAlgorithm the hashing algorithm used to hash the key provided by the server.
         * @return the local reference.
         */
        @NotNull
        public Builder serverKeyHashingAlgorithm(@NotNull HashingAlgorithm hashingAlgorithm) {
            impl.serverKeyHashingAlgorithm(hashingAlgorithm);
            return this;
        }

        /**
         * Sets the server key hash which gets compared with the key
         * provided by the server.
         *
         * @param bytes the key represented as a byte array.
         * @return the local reference.
         */
        @NotNull
        public Builder serverKeyHash(byte[] bytes) {
            impl.serverKeyHash(bytes);
            return this;
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
         * Sets a proxy to which the client connects to in order
         * to hide its identity.
         *
         * @param address the address of the proxy server to which the client should connect to.
         * @param proxyType the type of the proxy server that the client should connect to.
         * @return the local reference.
         */
        @NotNull
        public Builder proxy(@NotNull SocketAddress address, @NotNull ProxyType proxyType) {
            impl.proxy(address, proxyType);
            return this;
        }

        /**
         * Builds the client, sets its default values and starts it.
         *
         * @return the started Client.
         */
        @NotNull
        public Client build() {
            return impl.build();
        }

    }

}
