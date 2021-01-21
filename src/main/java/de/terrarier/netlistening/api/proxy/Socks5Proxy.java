package de.terrarier.netlistening.api.proxy;

import io.netty.channel.ChannelHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import org.jetbrains.annotations.NotNull;

import java.net.SocketAddress;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class Socks5Proxy extends Proxy {

    public Socks5Proxy(@NotNull SocketAddress address, @NotNull ProxyType type) {
        super(address, type);
    }

    @NotNull
    @Override
    public ChannelHandler getHandler() {
        return new Socks5ProxyHandler(address);
    }

}
