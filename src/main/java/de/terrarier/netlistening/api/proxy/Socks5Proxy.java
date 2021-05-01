package de.terrarier.netlistening.api.proxy;

import de.terrarier.netlistening.internals.AssumeNotNull;
import io.netty.channel.ChannelHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;

import java.net.SocketAddress;

/**
 * @since 1.0
 * @author Terrarier2111
 */
final class Socks5Proxy extends Proxy {

    Socks5Proxy(@AssumeNotNull SocketAddress address) {
        super(address);
    }

    @AssumeNotNull
    @Override
    public ChannelHandler newHandler() {
        return new Socks5ProxyHandler(address);
    }

}
