package de.terrarier.netlistening;

import de.terrarier.netlistening.api.DataComponent;
import de.terrarier.netlistening.api.DataContainer;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public interface Connection {

    /**
     * Sends data to the connection.
     *
     * @param data the data to be sent to the connection
     */
    void sendData(@NotNull DataContainer data);

    /**
     * Sends data to the connection.
     *
     * @param data the data to be sent to the connection
     */
    void sendData(@NotNull DataComponent<?> data);

    /**
     * Disconnects the connection.
     */
    void disconnect();

    /**
     * @return if the connection is connected
     */
    boolean isConnected();

    /**
     * @return the channel underlying the connection.
     */
    @NotNull
    Channel getChannel();

    /**
     * @return the id of the connection
     */
    int getId();

}
