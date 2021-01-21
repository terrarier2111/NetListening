package de.terrarier.netlistening.api.event;

import de.terrarier.netlistening.Connection;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class ConnectionDisconnectEvent extends ConnectionEvent {

    public ConnectionDisconnectEvent(@NotNull Connection connection) {
        super(connection);
    }

}
