package de.terrarier.netlistening.api.event;

import de.terrarier.netlistening.Connection;
import de.terrarier.netlistening.internals.AssumeNotNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * This event gets called when a connection times out.
 * Note that in order for this event to be called a
 * timeout has to be defined in the builder of the
 * Application.
 *
 * @author Terrarier2111
 * @since 1.0
 */
public final class ConnectionTimeoutEvent extends ConnectionEvent {

    @ApiStatus.Internal
    public ConnectionTimeoutEvent(@AssumeNotNull Connection connection) {
        super(connection);
    }

}
