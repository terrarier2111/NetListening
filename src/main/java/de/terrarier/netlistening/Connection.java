package de.terrarier.netlistening;

import de.terrarier.netlistening.api.DataComponent;
import de.terrarier.netlistening.api.DataContainer;
import de.terrarier.netlistening.internals.AssumeNotNull;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;

/**
 * @author Terrarier2111
 * @since 1.0
 */
public interface Connection {

    /**
     * Sends data to the connection.
     *
     * @param data the data to be sent to the connection.
     */
    void sendData(@NotNull DataContainer data);

    /**
     * Sends data to the connection.
     *
     * @param data the data to be sent to the connection.
     */
    @Deprecated
    default void sendData(@NotNull DataComponent<?> data) {
        final DataContainer container = new DataContainer();
        container.addComponent(data);
        sendData(container);
    }

    /**
     * Sends data to the connection.
     *
     * @param data the data which gets sent.
     */
    default void sendData(@NotNull Object... data) {
        sendData(false, data);
    }

    /**
     * Sends data to the connection.
     *
     * @param encrypted whether the traffic is to be encrypted.
     * @param data      the data which gets sent.
     * @deprecated passing a boolean as the first part of data might unexpectedly
     * be compiled to this method.
     */
    @Deprecated
    void sendData(boolean encrypted, @NotNull Object... data);

    /**
     * Disconnects the connection.
     *
     * @throws IllegalStateException if {@code Connection#isConnected()} returns {@code false}.
     */
    void disconnect();

    /**
     * @return if the connection is connected.
     */
    boolean isConnected();

    /**
     * @return the channel underlying the connection.
     */
    @AssumeNotNull
    Channel getChannel();

    /**
     * @return the remote address of the connection.
     */
    @AssumeNotNull
    InetSocketAddress getRemoteAddress();

    /**
     * @return the id of the connection.
     */
    int getId();

}
