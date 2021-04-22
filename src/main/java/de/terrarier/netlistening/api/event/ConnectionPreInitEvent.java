package de.terrarier.netlistening.api.event;

import de.terrarier.netlistening.internals.AssumeNotNull;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;

/**
 * This event gets called before a connection is established.
 *
 * @since 1.0
 * @author Terrarier2111
 */
public final class ConnectionPreInitEvent extends Cancellable implements Event {
	
	private final Channel channel;
	
	public ConnectionPreInitEvent(@NotNull Channel channel) {
		this.channel = channel;
	}

	/**
	 * @return the channel, representing a connection which is about
	 * to be established by the api.
	 */
	@AssumeNotNull
	public Channel getChannel() {
		return channel;
	}

}
