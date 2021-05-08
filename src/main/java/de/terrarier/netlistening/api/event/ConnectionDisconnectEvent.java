package de.terrarier.netlistening.api.event;

import de.terrarier.netlistening.Connection;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * This event is called when a connection is closed.
 *
 * @since 1.0
 * @author Terrarier2111
 */
public final class ConnectionDisconnectEvent extends ConnectionEvent {

    @ApiStatus.Internal
    public ConnectionDisconnectEvent(@NotNull Connection connection) {
        super(connection);
    }

}
