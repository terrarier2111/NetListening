package de.terrarier.netlistening.utils;

import de.terrarier.netlistening.internals.AssumeNotNull;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.internal.SystemPropertyUtil;
import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.SocketAddress;
import java.nio.channels.Channel;
import java.util.Locale;

/**
 * This class' sole purpose is the enable NetListening to support
 * older versions of netty and UDS at the same time.
 *
 * @author Terrarier2111
 * @since 1.10
 */
@SuppressWarnings("unchecked")
public final class UDS {

    private static Class<? extends ServerChannel> KQUEUE_SSC;
    private static Class<? extends ServerChannel> KQUEUE_SDSC;
    private static Class<? extends ServerChannel> EPOLL_SDSC;
    private static Class<? extends Channel> KQUEUE_SC;
    private static Class<? extends Channel> KQUEUE_DSC;
    private static Class<? extends Channel> EPOLL_DSC;
    private static Class<? extends Channel> DSC;
    private static Constructor<?> DOMAIN_SOCKET_ADDRESS_CONSTRUCTOR;

    private static final boolean AVAILABLE;
    private static final boolean OSX = isOsx0();

    static {
        final boolean epoll = Epoll.isAvailable();
        if(OSX) {
            try {
                KQUEUE_SSC = (Class<? extends ServerChannel>) Class.forName(
                        "io.netty.channel.kqueue.KQueueServerSocketChannel");
            } catch (ClassNotFoundException e) {
                // KQueueServerSocketChannel is not available.
            }
            try {
                KQUEUE_SDSC = (Class<? extends ServerChannel>) Class.forName(
                        "io.netty.channel.kqueue.KQueueServerDomainSocketChannel");
            } catch (ClassNotFoundException e) {
                // KQueueServerDomainSocketChannel is not available.
            }
            try {
                KQUEUE_SC = (Class<? extends Channel>) Class.forName(
                        "io.netty.channel.kqueue.KQueueSocketChannel");
            } catch (ClassNotFoundException e) {
                // KQueueSocketChannel is not available.
            }
            try {
                KQUEUE_DSC = (Class<? extends Channel>) Class.forName(
                        "io.netty.channel.kqueue.KQueueDomainSocketChannel");
            } catch (ClassNotFoundException e) {
                // KQueueDomainSocketChannel is not available.
                try {
                    DSC = (Class<? extends Channel>) Class.forName("io.netty.channel.unix.DomainSocketChannel");
                } catch (ClassNotFoundException ex) {
                    // DomainSocketChannel is not available.
                }
            }
        }else if(epoll) {
            try {
                EPOLL_DSC = (Class<? extends Channel>) Class.forName("io.netty.channel.epoll.EpollDomainSocketChannel");
            } catch (ClassNotFoundException e) {
                // EpollDomainSocketChannel is not available.
            }
            try {
                EPOLL_SDSC = (Class<? extends ServerChannel>) Class.forName(
                        "io.netty.channel.epoll.EpollServerDomainSocketChannel");
            } catch (ClassNotFoundException e) {
                // EpollServerDomainSocketChannel is not available.
            }
        }
        if(OSX || epoll) {
            try {
                final Class<? extends SocketAddress> DOMAIN_SOCKET_ADDRESS =
                        (Class<? extends SocketAddress>) Class.forName("io.netty.channel.unix.DomainSocketAddress");
                try {
                    DOMAIN_SOCKET_ADDRESS_CONSTRUCTOR = DOMAIN_SOCKET_ADDRESS.getDeclaredConstructor(String.class);
                } catch (NoSuchMethodException e) {
                    // Constructor is not available.
                }
            } catch (ClassNotFoundException e) {
                // DomainSocketAddress is not available.
            }
            AVAILABLE = DOMAIN_SOCKET_ADDRESS_CONSTRUCTOR != null;
        }else {
            AVAILABLE = false;
        }
    }

    private UDS() {
        throw new UnsupportedOperationException("This class may not be instantiated!");
    }

    @AssumeNotNull
    @ApiStatus.Internal
    public static SocketAddress domainSocketAddress(@AssumeNotNull String filePath) {
        if(AVAILABLE) {
            try {
                return (SocketAddress) DOMAIN_SOCKET_ADDRESS_CONSTRUCTOR.newInstance(filePath);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                // UDS is unsupported.
            }
        }
        throw new UnsupportedOperationException("UDS are not supported on this platform.");
    }

    @ApiStatus.Internal
    public static <T> Class<? extends T> channel(boolean uds) {
        final boolean epoll = Epoll.isAvailable();
        if(uds) {
            if(epoll) {
                return (Class<? extends T>) EPOLL_DSC;
            }
            if(OSX) {
                if(KQUEUE_DSC != null) {
                    return (Class<? extends T>) KQUEUE_DSC;
                }
                return (Class<? extends T>) DSC;
            }
            throw new UnsupportedOperationException();
        }
        if(epoll) {
            return (Class<? extends T>) EpollSocketChannel.class;
        }
        if(OSX && KQUEUE_SC != null) {
            return (Class<? extends T>) KQUEUE_SC;
        }
        return (Class<? extends T>) NioSocketChannel.class;
    }

    @ApiStatus.Internal
    public static <T> Class<? extends T> serverChannel(boolean uds) {
        final boolean epoll = Epoll.isAvailable();
        if(uds) {
            if(epoll) {
                return (Class<? extends T>) EPOLL_SDSC;
            }
            if(OSX) {
                if(KQUEUE_SDSC != null) {
                    return (Class<? extends T>) KQUEUE_SDSC;
                }
                throw new UnsupportedOperationException("Kqueue is not present in the classpath hence UDS is not supported on the server side.");
            }
            throw new UnsupportedOperationException();
        }
        if(epoll) {
            return (Class<? extends T>) EpollServerSocketChannel.class;
        }
        if(OSX && KQUEUE_SSC != null) {
            return (Class<? extends T>) KQUEUE_SSC;
        }
        return (Class<? extends T>) NioServerSocketChannel.class;
    }

    @ApiStatus.Internal
    public static EventLoopGroup eventLoopGroup() {
        return OSX ? new KQueueEventLoopGroup() : Epoll.isAvailable() ? new EpollEventLoopGroup() :
                new NioEventLoopGroup();
    }

    public static boolean isAvailable(boolean server) {
        return (!server || (EPOLL_SDSC != null || KQUEUE_SDSC != null)) && AVAILABLE;
    }

    // Copied from PlatformDependent (and modified) from netty in order to allow for correct backwards compatible osx checks.
    private static boolean isOsx0() {
        final String value = SystemPropertyUtil.get("os.name", "").toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "");
        return value.startsWith("macosx") || value.startsWith("osx") || value.startsWith("darwin");
    }

}
