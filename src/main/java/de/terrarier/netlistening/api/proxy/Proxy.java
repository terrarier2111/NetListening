package de.terrarier.netlistening.api.proxy;

import io.netty.channel.ChannelHandler;
import org.jetbrains.annotations.NotNull;

import java.net.SocketAddress;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public abstract class Proxy {

    final SocketAddress address;

    Proxy(@NotNull SocketAddress address) {
        this.address = address;
    }

    @NotNull
    public abstract ChannelHandler newHandler();

}
