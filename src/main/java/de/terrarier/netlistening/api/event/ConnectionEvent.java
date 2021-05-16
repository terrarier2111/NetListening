package de.terrarier.netlistening.api.event;

import de.terrarier.netlistening.Connection;
import de.terrarier.netlistening.internals.AssumeNotNull;

/**
 * @author Terrarier2111
 * @since 1.0
 */
public abstract class ConnectionEvent extends Cancellable implements Event {

    private final Connection connection;

    ConnectionEvent(@AssumeNotNull Connection connection) {
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
