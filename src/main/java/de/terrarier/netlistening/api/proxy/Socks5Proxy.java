package de.terrarier.netlistening.api.proxy;

import io.netty.channel.ChannelHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import org.jetbrains.annotations.NotNull;

import java.net.SocketAddress;

/**
 * @since 1.0
 * @author Terrarier2111
 */
final class Socks5Proxy extends Proxy {

    protected Socks5Proxy(@NotNull SocketAddress address) {
        super(address);
    }

    @NotNull
    @Override
    public ChannelHandler newHandler() {
        return new Socks5ProxyHandler(address);
    }

}
