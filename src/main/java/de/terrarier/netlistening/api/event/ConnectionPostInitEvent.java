package de.terrarier.netlistening.api.event;

import de.terrarier.netlistening.Connection;
import org.jetbrains.annotations.NotNull;

/**
 * This event gets called after a connection was established.
 *
 * @since 1.0
 * @author Terrarier2111
 */
public final class ConnectionPostInitEvent extends ConnectionEvent {

    public ConnectionPostInitEvent(@NotNull Connection connection) {
        super(connection);
    }

}
