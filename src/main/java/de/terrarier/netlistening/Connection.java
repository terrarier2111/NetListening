package de.terrarier.netlistening;

import de.terrarier.netlistening.api.DataComponent;
import de.terrarier.netlistening.api.DataContainer;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;

/**
 * @since 1.0
 * @author Terrarier2111
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
     * @param data the data which gets sent.
     */
    void sendData(boolean encrypted, @NotNull Object... data);

    /**
     * Disconnects the connection.
     */
    void disconnect();

    /**
     * @return if the connection is connected.
     */
    boolean isConnected();

    /**
     * @return the channel underlying the connection.
     */
    @NotNull
    Channel getChannel();

    /**
     * @return the remote address of the connection.
     */
    @NotNull
    InetSocketAddress getRemoteAddress();

    /**
     * @return the id of the connection.
     */
    int getId();

}
