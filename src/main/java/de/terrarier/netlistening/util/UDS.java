/*
Copyright 2021 Terrarier2111

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package de.terrarier.netlistening.util;

import de.terrarier.netlistening.internal.AssumeNotNull;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
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
 * This class' sole purpose is to enable NetListening to support
 * older versions of Netty and UDS at the same time.
 *
 * @author Terrarier2111
 * @since 1.10
 */
@SuppressWarnings("unchecked")
public final class UDS {

    private static final boolean AVAILABLE;
    private static final boolean OSX = isOsx0();
    private static Class<? extends ServerChannel> KQUEUE_SERVER_SOCKET_CHANNEL;
    private static Class<? extends ServerChannel> KQUEUE_SERVER_DOMAIN_SOCKET_CHANNEL;
    private static Class<? extends ServerChannel> EPOLL_SERVER_DOMAIN_SOCKET_CHANNEL;
    private static Class<? extends Channel> KQUEUE_SOCKET_CHANNEL;
    private static Class<? extends Channel> KQUEUE_DOMAIN_SOCKET_CHANNEL;
    private static Class<? extends Channel> EPOLL_DOMAIN_SOCKET_CHANNEL;
    private static Class<? extends Channel> DOMAIN_SOCKET_CHANNEL;
    private static Class<? extends EventLoopGroup> KQUEUE_EVENT_LOOP_GROUP;
    private static Constructor<? extends SocketAddress> DOMAIN_SOCKET_ADDRESS_CONSTRUCTOR;

    static {
        final boolean epoll = Epoll.isAvailable();
        // We catch IllegalAccessError because if a totally different version of Netty is used at runtime,
        // it will cause this error to get thrown.
        if (OSX) {
            try {
                KQUEUE_SERVER_SOCKET_CHANNEL = (Class<? extends ServerChannel>) Class.forName(
                        "io.netty.channel.kqueue.KQueueServerSocketChannel");
                KQUEUE_SOCKET_CHANNEL = (Class<? extends Channel>) Class.forName(
                        "io.netty.channel.kqueue.KQueueSocketChannel");
                KQUEUE_EVENT_LOOP_GROUP = (Class<? extends EventLoopGroup>) Class.forName(
                        "io.netty.channel.kqueue.KQueueEventLoopGroup");
            } catch (ClassNotFoundException | IllegalAccessError e) {
                // KQueueServerSocketChannel, KQueueSocketChannel or KQueueEventLoopGroup is not available.
            }
            try {
                KQUEUE_SERVER_DOMAIN_SOCKET_CHANNEL = (Class<? extends ServerChannel>) Class.forName(
                        "io.netty.channel.kqueue.KQueueServerDomainSocketChannel");
            } catch (ClassNotFoundException | IllegalAccessError e) {
                // KQueueServerDomainSocketChannel is not available.
            }
            try {
                KQUEUE_DOMAIN_SOCKET_CHANNEL = (Class<? extends Channel>) Class.forName(
                        "io.netty.channel.kqueue.KQueueDomainSocketChannel");
            } catch (ClassNotFoundException | IllegalAccessError e) {
                // KQueueDomainSocketChannel is not available.
                try {
                    DOMAIN_SOCKET_CHANNEL = (Class<? extends Channel>) Class.forName(
                            "io.netty.channel.unix.DomainSocketChannel");
                } catch (ClassNotFoundException | IllegalAccessError e1) {
                    // DomainSocketChannel is not available.
                }
            }
        } else if (epoll) {
            try {
                EPOLL_DOMAIN_SOCKET_CHANNEL = (Class<? extends Channel>) Class.forName(
                        "io.netty.channel.epoll.EpollDomainSocketChannel");
            } catch (ClassNotFoundException | IllegalAccessError e) {
                // EpollDomainSocketChannel is not available.
            }
            try {
                EPOLL_SERVER_DOMAIN_SOCKET_CHANNEL = (Class<? extends ServerChannel>) Class.forName(
                        "io.netty.channel.epoll.EpollServerDomainSocketChannel");
            } catch (ClassNotFoundException | IllegalAccessError e) {
                // EpollServerDomainSocketChannel is not available.
            }
        }
        if (OSX || epoll) {
            try {
                final Class<? extends SocketAddress> DOMAIN_SOCKET_ADDRESS =
                        (Class<? extends SocketAddress>) Class.forName("io.netty.channel.unix.DomainSocketAddress");
                try {
                    DOMAIN_SOCKET_ADDRESS_CONSTRUCTOR = DOMAIN_SOCKET_ADDRESS.getDeclaredConstructor(String.class);
                } catch (NoSuchMethodException e) {
                    // Constructor is not available.
                }
            } catch (ClassNotFoundException | IllegalAccessError e) {
                // DomainSocketAddress is not available.
            }
            AVAILABLE = DOMAIN_SOCKET_ADDRESS_CONSTRUCTOR != null;
        } else {
            AVAILABLE = false;
        }
    }

    private UDS() {
        throw new UnsupportedOperationException("This class may not be instantiated!");
    }

    @ApiStatus.Internal
    @AssumeNotNull
    public static SocketAddress domainSocketAddress(@AssumeNotNull String filePath) {
        if (AVAILABLE) {
            try {
                return DOMAIN_SOCKET_ADDRESS_CONSTRUCTOR.newInstance(filePath);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                // UDS is unsupported.
            }
        }
        throw new UnsupportedOperationException("UDS are not supported on this platform.");
    }

    @ApiStatus.Internal
    @AssumeNotNull
    public static <T> Class<? extends T> channel(boolean uds) {
        final boolean epoll = Epoll.isAvailable();
        if (uds) {
            if (epoll) {
                return (Class<? extends T>) EPOLL_DOMAIN_SOCKET_CHANNEL;
            }
            if (OSX) {
                if (KQUEUE_DOMAIN_SOCKET_CHANNEL != null) {
                    return (Class<? extends T>) KQUEUE_DOMAIN_SOCKET_CHANNEL;
                }
                return (Class<? extends T>) DOMAIN_SOCKET_CHANNEL;
            }
            throw new UnsupportedOperationException();
        }
        if (epoll) {
            return (Class<? extends T>) EpollSocketChannel.class;
        }
        if (OSX && KQUEUE_SOCKET_CHANNEL != null) {
            return (Class<? extends T>) KQUEUE_SOCKET_CHANNEL;
        }
        return (Class<? extends T>) NioSocketChannel.class;
    }

    @ApiStatus.Internal
    @AssumeNotNull
    public static <T> Class<? extends T> serverChannel(boolean uds) {
        final boolean epoll = Epoll.isAvailable();
        if (uds) {
            if (epoll) {
                return (Class<? extends T>) EPOLL_SERVER_DOMAIN_SOCKET_CHANNEL;
            }
            if (OSX) {
                if (KQUEUE_SERVER_DOMAIN_SOCKET_CHANNEL != null) {
                    return (Class<? extends T>) KQUEUE_SERVER_DOMAIN_SOCKET_CHANNEL;
                }
                throw new UnsupportedOperationException(
                        "KQueue is not present in the classpath hence UDS is not supported on the server side.");
            }
            throw new UnsupportedOperationException();
        }
        if (epoll) {
            return (Class<? extends T>) EpollServerSocketChannel.class;
        }
        if (OSX && KQUEUE_SERVER_SOCKET_CHANNEL != null) {
            return (Class<? extends T>) KQUEUE_SERVER_SOCKET_CHANNEL;
        }
        return (Class<? extends T>) NioServerSocketChannel.class;
    }

    @ApiStatus.Internal
    @AssumeNotNull
    public static EventLoopGroup eventLoopGroup() {
        try {
            return OSX && KQUEUE_EVENT_LOOP_GROUP != null ? KQUEUE_EVENT_LOOP_GROUP.newInstance() :
                    Epoll.isAvailable() ? new EpollEventLoopGroup() : new NioEventLoopGroup();
        } catch (InstantiationException | IllegalAccessException e) {
            // KQueueEventLoopGroup is unsupported.
        }
        return new NioEventLoopGroup();
    }

    /**
     * @param server whether availability should be checked for the server side or the client side.
     * @return whether UDS is available or not based on OS and classes on classpath.
     */
    public static boolean isAvailable(boolean server) {
        return (!server || (EPOLL_SERVER_DOMAIN_SOCKET_CHANNEL != null || KQUEUE_SERVER_DOMAIN_SOCKET_CHANNEL != null)) &&
                AVAILABLE;
    }

    // Copied from PlatformDependent (and modified) from Netty in order to allow for correct backwards compatible osx checks.
    private static boolean isOsx0() {
        final String value = SystemPropertyUtil.get("os.name", "").toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "");
        return value.startsWith("macosx") || value.startsWith("osx") || value.startsWith("darwin");
    }

}
