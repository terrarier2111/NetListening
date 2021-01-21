package de.terrarier.netlistening.api.event;

import de.terrarier.netlistening.Connection;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class ConnectionTimeoutEvent extends ConnectionEvent {
	
	public ConnectionTimeoutEvent(@NotNull Connection connection) {
		super(connection);
	}

}
