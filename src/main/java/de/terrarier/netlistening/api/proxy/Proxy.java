package de.terrarier.netlistening.api.proxy;

import de.terrarier.netlistening.internals.AssumeNotNull;
import io.netty.channel.ChannelHandler;
import org.jetbrains.annotations.ApiStatus;

import java.net.SocketAddress;

/**
 * @since 1.0
 * @author Terrarier2111
 */
@ApiStatus.Internal
public abstract class Proxy {

    final SocketAddress address;

    Proxy(@AssumeNotNull SocketAddress address) {
        this.address = address;
    }

    @AssumeNotNull
    public abstract ChannelHandler newHandler();

}
