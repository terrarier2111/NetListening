package de.terrarier.netlistening.api.event;

import de.terrarier.netlistening.Connection;
import de.terrarier.netlistening.internals.AssumeNotNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * This event gets called after a connection was established.
 *
 * @since 1.0
 * @author Terrarier2111
 */
public final class ConnectionPostInitEvent extends ConnectionEvent {

    @ApiStatus.Internal
    public ConnectionPostInitEvent(@AssumeNotNull Connection connection) {
        super(connection);
    }

}
