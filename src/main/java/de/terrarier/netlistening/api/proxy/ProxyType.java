package de.terrarier.netlistening.api.proxy;

import org.jetbrains.annotations.NotNull;

import java.net.SocketAddress;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public enum ProxyType {

    SOCKS4, SOCKS5;

    public Proxy getInstance(@NotNull SocketAddress address) {
        switch (this) {
            case SOCKS4:
                return new Socks4Proxy(address);
            case SOCKS5:
                return new Socks5Proxy(address);
            default:
                return null;
        }
    }

}
