package de.terrarier.netlistening.api.proxy;

import io.netty.channel.ChannelHandler;
import org.jetbrains.annotations.NotNull;

import java.net.SocketAddress;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public abstract class Proxy {

    protected final SocketAddress address;
    protected final ProxyType type;

    public Proxy(@NotNull SocketAddress address, @NotNull ProxyType type) {
        this.address = address;
        this.type = type;
    }

    @NotNull
    public final ProxyType getType() {
        return type;
    }

    @NotNull
    public abstract ChannelHandler getHandler();

}
