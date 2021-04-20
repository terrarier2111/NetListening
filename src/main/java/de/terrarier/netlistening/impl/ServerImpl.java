package de.terrarier.netlistening.impl;

import de.terrarier.netlistening.Connection;
import de.terrarier.netlistening.Server;
import de.terrarier.netlistening.api.DataContainer;
import de.terrarier.netlistening.api.PacketCaching;
import de.terrarier.netlistening.api.compression.CompressionSetting;
import de.terrarier.netlistening.api.encryption.EncryptionSetting;
import de.terrarier.netlistening.api.encryption.SymmetricEncryptionUtil;
import de.terrarier.netlistening.api.encryption.hash.HmacSetting;
import de.terrarier.netlistening.api.event.ConnectionPostInitEvent;
import de.terrarier.netlistening.api.event.ConnectionPreInitEvent;
import de.terrarier.netlistening.api.event.EventManager;
import de.terrarier.netlistening.api.event.ListenerType;
import de.terrarier.netlistening.network.PacketDataDecoder;
import de.terrarier.netlistening.network.PacketDataEncoder;
import de.terrarier.netlistening.network.TimeOutHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.concurrent.*;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class ServerImpl extends ApplicationImpl implements Server {

    private final Map<Channel, ConnectionImpl> connections = new ConcurrentHashMap<>();
    private PacketCaching caching = PacketCaching.NONE;
    private ScheduledExecutorService delayedExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {

        final ThreadFactory factory = Executors.defaultThreadFactory();

        @Override
        public Thread newThread(@NotNull Runnable r) {
            final Thread t = factory.newThread(r);
            t.setDaemon(true);
            return t;
        }
    });

    private void start(long timeout, int port, @NotNull Map<ChannelOption<?>, Object> options) {
        if (group != null) {
            throw new IllegalStateException("The server is already started!");
        }
        serializationProvider.setEventManager(eventManager);

        final boolean epoll = Epoll.isAvailable();
        group = epoll ? new EpollEventLoopGroup() : new NioEventLoopGroup();
        worker = new Thread(() -> {
            try {
                final Channel channel = new ServerBootstrap().group(group)
                        .channel(epoll ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                        .childHandler(new ChannelInitializer<Channel>() {
                            @Override
                            protected void initChannel(Channel channel) {
                                if(eventManager.callEvent(ListenerType.PRE_INIT, EventManager.CancelAction.INTERRUPT,
                                        new ConnectionPreInitEvent(channel))) {
                                    channel.close();
                                    return;
                                }
                                channel.config().setOptions(options);

                                final ConnectionImpl connection = new ConnectionImpl(ServerImpl.this, channel);
                                if (encryptionSetting != null) {
                                    try {
                                        connection.setSymmetricKey(ServerImpl.this,
                                                SymmetricEncryptionUtil.generate(encryptionSetting.getSymmetricSetting())
                                                        .getSecretKey());

                                        final HmacSetting hmacSetting = encryptionSetting.getHmacSetting();
                                        if (hmacSetting != null) {
                                            connection.setHmacKey(SymmetricEncryptionUtil.generate(
                                                    hmacSetting.getEncryptionSetting()).getSecretKey());
                                        }
                                    } catch (NoSuchAlgorithmException e) {
                                        e.printStackTrace();
                                    }
                                }
                                final ChannelPipeline pipeline = channel.pipeline();

                                if (timeout > 0) {
                                    pipeline.addLast(TIMEOUT_HANDLER,
                                            new TimeOutHandler(ServerImpl.this, connection, timeout));
                                }
                                pipeline.addLast(DECODER, new PacketDataDecoder(ServerImpl.this, handler, connection))
                                        .addAfter(DECODER, ENCODER, new PacketDataEncoder(ServerImpl.this, delayedExecutor,
                                                connection));

                                connections.put(channel, connection);
                                eventManager.callEvent(ListenerType.POST_INIT, new ConnectionPostInitEvent(connection));

                            }
                        }).bind(port).sync().channel();
                channel.config().setOptions(options);
                channel.closeFuture().syncUninterruptibly();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        worker.start();
    }

    /**
     * @see de.terrarier.netlistening.Application
     */
    @Override
    public @NotNull PacketCaching getCaching() {
        return caching;
    }

    /**
     * @see de.terrarier.netlistening.Application
     */
    @Override
    public Connection getConnection(@NotNull Channel channel) {
        return connections.get(channel);
    }

    /**
     * @see de.terrarier.netlistening.Application
     */
    @Override
    public Connection getConnection(int id) {
        if(id < 0 || id > ConnectionImpl.ID.get()) {
            return null;
        }

        for (Connection connection : connections.values()) {
            if (connection.getId() == id) {
                return connection;
            }
        }
        return null;
    }

    /**
     * @see de.terrarier.netlistening.Application
     */
    @Override
    public void stop() {
        if (group == null) {
            throw new IllegalStateException("The server is already stopped!");
        }

        for (Iterator<ConnectionImpl> iterator = connections.values().iterator(); iterator.hasNext();) {
            final ConnectionImpl connection = iterator.next();
            connection.disconnect0();
            iterator.remove();
        }
        handler.unregisterListeners();
        group.shutdownGracefully();
        group = null;
        worker.interrupt();
        worker = null;
        cache.clear();
        if(!delayedExecutor.isShutdown()) {
            delayedExecutor.shutdown();
            try {
                if (!delayedExecutor.awaitTermination(250L, TimeUnit.MILLISECONDS)) {
                    delayedExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                delayedExecutor.shutdownNow();
            }
            delayedExecutor = null;
        }
    }

    /**
     * @see de.terrarier.netlistening.Application
     */
    @Deprecated
    @Override
    public void disconnect(@NotNull Connection connection) {
        connection.disconnect();
    }

    void disconnect0(@NotNull Connection connection) {
        connections.remove(connection.getChannel());
    }

    /**
     * @see de.terrarier.netlistening.Application
     */
    @Override
    public void sendData(@NotNull DataContainer data) {
        for (Connection connection : connections.values()) {
            connection.sendData(data);
        }
    }

    /**
     * @see de.terrarier.netlistening.Application
     */
    @Override
    public @NotNull Set<Connection> getConnections() {
        return Collections.unmodifiableSet(new HashSet<>(connections.values()));
    }

    @ApiStatus.Internal
    public static final class Builder extends ApplicationImpl.Builder<ServerImpl, Builder> {

        private final int port;

        public Builder(int port) {
            super(new ServerImpl());
            this.port = port;
        }

        /**
         * @see Server.Builder
         */
        public void caching(@NotNull PacketCaching caching) {
            validate();
            application.caching = caching;
        }

        /**
         * @see Server.Builder
         */
        public void compression(@NotNull CompressionSetting compressionSetting) {
            validate();
            application.compressionSetting = compressionSetting;
        }

        /**
         * @see Server.Builder
         */
        public void stringEncoding(@NotNull Charset charset) {
            validate();
            application.stringEncoding = charset;
        }

        /**
         * @see Server.Builder
         */
        public void encryption(@NotNull EncryptionSetting encryptionSetting) {
            validate();
            application.encryptionSetting = encryptionSetting;
        }

        /**
         * @see ApplicationImpl.Builder
         */
        @Override
        void build0() {
            if (application.caching == PacketCaching.NONE) {
                application.caching = PacketCaching.GLOBAL;
            }

            if(application.compressionSetting == null) {
                application.compressionSetting = new CompressionSetting();
            }

            if (application.encryptionSetting != null && !application.encryptionSetting.isInitialized()) {
                try {
                    application.encryptionSetting.init(null);
                } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                    e.printStackTrace();
                    return;
                }
            }
            application.start(timeout, port, options);
        }

    }

}
