package de.terrarier.netlistening.api.proxy;

import io.netty.channel.ChannelHandler;
import io.netty.handler.proxy.Socks4ProxyHandler;
import org.jetbrains.annotations.NotNull;

import java.net.SocketAddress;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class Socks4Proxy extends Proxy {

    public Socks4Proxy(@NotNull SocketAddress address, @NotNull ProxyType type) {
        super(address, type);
    }

    @NotNull
    @Override
    public ChannelHandler getHandler() {
        return new Socks4ProxyHandler(address);
    }

}
