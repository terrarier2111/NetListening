package de.terrarier.netlistening.api.event;

import de.terrarier.netlistening.Connection;
import de.terrarier.netlistening.internals.AssumeNotNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * This event is called when a connection is closed.
 *
 * @author Terrarier2111
 * @since 1.0
 */
public final class ConnectionDisconnectEvent extends ConnectionEvent {

    @ApiStatus.Internal
    public ConnectionDisconnectEvent(@AssumeNotNull Connection connection) {
        super(connection);
    }

}
