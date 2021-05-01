package de.terrarier.netlistening.api.proxy;

import de.terrarier.netlistening.internals.AssumeNotNull;
import io.netty.channel.ChannelHandler;
import io.netty.handler.proxy.Socks4ProxyHandler;

import java.net.SocketAddress;

/**
 * @since 1.0
 * @author Terrarier2111
 */
final class Socks4Proxy extends Proxy {

    Socks4Proxy(@AssumeNotNull SocketAddress address) {
        super(address);
    }

    @AssumeNotNull
    @Override
    public ChannelHandler newHandler() {
        return new Socks4ProxyHandler(address);
    }

}
