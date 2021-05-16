package de.terrarier.netlistening.api.event;

import de.terrarier.netlistening.internals.AssumeNotNull;
import io.netty.channel.Channel;
import org.jetbrains.annotations.ApiStatus;

/**
 * This event gets called before a connection is established.
 *
 * @author Terrarier2111
 * @since 1.0
 */
public final class ConnectionPreInitEvent extends Cancellable implements Event {

    private final Channel channel;

    @ApiStatus.Internal
    public ConnectionPreInitEvent(@AssumeNotNull Channel channel) {
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
