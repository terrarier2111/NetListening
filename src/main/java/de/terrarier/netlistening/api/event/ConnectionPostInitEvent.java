package de.terrarier.netlistening.api.event;

import de.terrarier.netlistening.Connection;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * This event gets called after a connection was established.
 *
 * @since 1.0
 * @author Terrarier2111
 */
public final class ConnectionPostInitEvent extends ConnectionEvent {

    @ApiStatus.Internal
    public ConnectionPostInitEvent(@NotNull Connection connection) {
        super(connection);
    }

}
