package de.terrarier.netlistening.api.event;

import de.terrarier.netlistening.Connection;
import de.terrarier.netlistening.internals.AssumeNotNull;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public abstract class ConnectionEvent extends Cancellable implements Event {

    private final Connection connection;

    ConnectionEvent(@NotNull Connection connection) {
        this.connection = connection;
    }

    /**
     * @return the connection this event is related to.
     */
    @AssumeNotNull
    public final Connection getConnection() {
        return connection;
    }

}
