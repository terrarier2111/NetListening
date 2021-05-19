package de.terrarier.netlistening;

import de.terrarier.netlistening.api.encryption.ServerKey;
import de.terrarier.netlistening.api.encryption.hash.HashingAlgorithm;
import de.terrarier.netlistening.api.proxy.ProxyType;
import de.terrarier.netlistening.api.serialization.SerializationProvider;
import de.terrarier.netlistening.impl.ClientImpl;
import de.terrarier.netlistening.internals.AssumeNotNull;
import de.terrarier.netlistening.internals.CheckNotNull;
import io.netty.channel.ChannelOption;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import static de.terrarier.netlistening.utils.ObjectUtilFallback.*;

/**
 * @author Terrarier2111
 * @since 1.0
 */
public interface Client extends Application {

    String PROXY_HANDLER = "nl_proxy_handler";

    /**
     * @return the connection of the Client and if not available, null.
     */
    Connection getConnection();

    /**
     * @return the key used by the server to encrypt the symmetric key.
     */
    ServerKey getServerKey();

    /**
     * Sets the expected key used by the server to encrypt the symmetric key.
     * If the key set is different to the key received by the server,
     * a KeyChangeEvent with a {@code HASH_CHANGED} action is getting called,
     * which can be used to detect MITM attacks.
     *
     * @param data the data representing the expected key.
     * @return whether or not setting the key was successful.
     */
    boolean setServerKey(@CheckNotNull byte[] data);

    /**
     * Creates a new builder with the passed arguments.
     *
     * @param host       the host the client should connect to.
     * @param remotePort the port the client should connect to.
     * @return the new builder.
     */
    @AssumeNotNull
    static Builder builder(String host, int remotePort) {
        return new Builder(host, remotePort);
    }

    /**
     * Creates a new builder with the passed arguments.
     *
     * @param remoteAddress the address to which the client should connect to.
     * @return the new builder.
     */
    @AssumeNotNull
    static Builder builder(SocketAddress remoteAddress) {
        return new Builder(remoteAddress);
    }

    /**
     * Creates a new builder with the passed arguments.
     *
     * @param filePath the filePath the client should write to.
     * @return the new builder.
     */
    @AssumeNotNull
    static Builder builder(String filePath) {
        return new Builder(filePath);
    }

    final class Builder extends Application.Builder<Client, Builder> {

        private final ClientImpl.Builder impl;

        /**
         * Constructs a builder for a default multi-use socket.
         *
         * @param host the host the socket should connect to.
         * @param remotePort the port the socket should connect to.
         */
        public Builder(@NotNull String host, int remotePort) {
            this(new InetSocketAddress(host, checkPositive(remotePort, "remotePort")));
        }

        /**
         * Constructs a builder for a default multi-use socket.
         *
         * @param remoteAddress the host the socket should connect to.
         */
        public Builder(@NotNull SocketAddress remoteAddress) {
            impl = new ClientImpl.Builder(remoteAddress);
        }

        /**
         * Constructs a builder for an UDS (UnixDomainSocket), works only locally.
         *
         * @param filePath the filePath the Server should write to.
         */
        public Builder(@NotNull String filePath) {
            impl = new ClientImpl.Builder(filePath);
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
         * Sets the source port of the client.
         *
         * @param localPort the port the client is getting bound to.
         * @return the local reference.
         */
        @AssumeNotNull
        public Builder localPort(int localPort) {
            impl.localPort(checkPositiveOrZero(localPort, "localPort"));
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
         * Sets the key hashing algorithm used to hash the public
         * key provided by the server.
         *
         * @param hashingAlgorithm the hashing algorithm used to hash the key provided by the server.
         * @return the local reference.
         */
        @AssumeNotNull
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
        @AssumeNotNull
        public Builder serverKeyHash(@CheckNotNull byte[] bytes) {
            impl.serverKeyHash(checkNotNull(bytes, "bytes"));
            return this;
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
         * Sets a proxy to which the client connects to in order
         * to hide its identity.
         *
         * @param address   the address of the proxy server to which the client should connect to.
         * @param proxyType the type of the proxy server that the client should connect to.
         * @return the local reference.
         */
        @AssumeNotNull
        public Builder proxy(@NotNull SocketAddress address, @NotNull ProxyType proxyType) {
            impl.proxy(address, proxyType);
            return this;
        }

        /**
         * @see Application.Builder#build()
         */
        @AssumeNotNull
        @Override
        public Client build() {
            return impl.build();
        }

    }

}
