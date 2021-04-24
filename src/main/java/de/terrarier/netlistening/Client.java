package de.terrarier.netlistening;

import de.terrarier.netlistening.api.DataContainer;
import de.terrarier.netlistening.api.encryption.ServerKey;
import de.terrarier.netlistening.api.encryption.hash.HashingAlgorithm;
import de.terrarier.netlistening.api.proxy.ProxyType;
import de.terrarier.netlistening.api.serialization.SerializationProvider;
import de.terrarier.netlistening.impl.ClientImpl;
import de.terrarier.netlistening.internals.AssumeNotNull;
import io.netty.channel.ChannelOption;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import static de.terrarier.netlistening.utils.ObjectUtilFallback.checkPositive;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public interface Client extends Application {

    String PROXY_HANDLER = "nl_proxy_handler";

    /**
     * Interrupts the connection to the server if there is one.
     */
    @Deprecated
    void disconnect();

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
    boolean setServerKey(byte[] data);

    /**
     * @see Application#sendData(Connection, DataContainer)
     * @deprecated use {@link Connection#sendData(DataContainer)} instead.
     */
    @Deprecated
    @Override
    default void sendData(Connection connection, @NotNull DataContainer data) {
        sendData(data);
    }

    /**
     * Creates a new builder with the passed arguments.
     *
     * @param host the host the client should connect to.
     * @param remotePort the port the client should connect to.
     * @return the new builder.
     */
    @AssumeNotNull
    static Builder builder(@NotNull String host, int remotePort) {
        checkPositive(remotePort, "remotePort");
        return new Builder(host, remotePort);
    }

    /**
     * Creates a new builder with the passed arguments.
     *
     * @param remoteAddress the address to which the client should connect to.
     * @return the new builder.
     */
    @AssumeNotNull
    static Builder builder(@NotNull SocketAddress remoteAddress) {
        return new Builder(remoteAddress);
    }

    @ApiStatus.Internal
    final class Builder extends Application.Builder<Client, Builder> {

        private final ClientImpl.Builder impl;

        public Builder(@NotNull String host, int remotePort) {
            this(new InetSocketAddress(host, remotePort));
        }

        public Builder(@NotNull SocketAddress remoteAddress) {
            this.impl = new ClientImpl.Builder(remoteAddress);
        }

        /**
         * @see Application.Builder#timeout(long)
         */
        @AssumeNotNull
        @Override
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
        @AssumeNotNull
        public Builder localPort(int localPort) {
            impl.localPort(localPort);
            return this;
        }

        /**
         * @see Application.Builder#buffer(int) 
         */
        @AssumeNotNull
        @Override
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
        public Builder serverKeyHash(byte[] bytes) {
            impl.serverKeyHash(bytes);
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
         * @param address the address of the proxy server to which the client should connect to.
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
