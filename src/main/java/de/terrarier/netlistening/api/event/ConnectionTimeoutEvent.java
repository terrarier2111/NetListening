package de.terrarier.netlistening.api.event;

import de.terrarier.netlistening.Connection;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * This event gets called when a connection times out.
 * Note that in order for this event to be called a
 * timeout has to be defined in the builder of the
 * Application.
 *
 * @since 1.0
 * @author Terrarier2111
 */
public final class ConnectionTimeoutEvent extends ConnectionEvent {

	@ApiStatus.Internal
	public ConnectionTimeoutEvent(@NotNull Connection connection) {
		super(connection);
	}

}
