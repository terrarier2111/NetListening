package de.terrarier.netlistening.impl;

import de.terrarier.netlistening.Connection;
import de.terrarier.netlistening.Server;
import de.terrarier.netlistening.api.DataComponent;
import de.terrarier.netlistening.api.DataContainer;
import de.terrarier.netlistening.api.PacketCaching;
import de.terrarier.netlistening.api.compression.CompressionSetting;
import de.terrarier.netlistening.api.encryption.EncryptionSetting;
import de.terrarier.netlistening.api.encryption.SymmetricEncryptionUtil;
import de.terrarier.netlistening.api.encryption.hash.HmacSetting;
import de.terrarier.netlistening.api.event.*;
import de.terrarier.netlistening.network.*;
import de.terrarier.netlistening.utils.ChannelUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class ServerImpl implements Server {

    private final Map<Channel, ConnectionImpl> connections = new ConcurrentHashMap<>();
    private final AtomicInteger id = new AtomicInteger();
    private final PacketCache cache = new PacketCache();
    private final DataHandler handler = new DataHandler(this);
    private final EventManager eventManager = new EventManager(handler);
    private int buffer = 256;
    private PacketCaching caching = PacketCaching.NONE;
    private PacketSynchronization packetSynchronization = PacketSynchronization.NONE;
    private CompressionSetting compressionSetting;
    private EventLoopGroup group;
    private Thread server;
    private Charset stringEncoding = StandardCharsets.UTF_8;
    private EncryptionSetting encryptionSetting;

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
    public @NotNull PacketSynchronization getPacketSynchronization() {
        return packetSynchronization;
    }

    /**
     * @see de.terrarier.netlistening.Application
     */
    @Override
    public @NotNull Charset getStringEncoding() {
        return stringEncoding;
    }

    /**
     * @see de.terrarier.netlistening.Application
     */
    @Deprecated
    @Override
    public void sendData(@NotNull DataContainer data, int connectionId) {
        final Connection connection = getConnection(connectionId);

        if (connection == null) {
            throw new IllegalArgumentException("There is no connection with the id " + Integer.toHexString(connectionId) + ".");
        }

        connection.sendData(data);
    }

    /**
     * @see de.terrarier.netlistening.Application
     */
    @Deprecated
    @Override
    public void sendData(@NotNull DataComponent<?> data, int connectionId) {
        final DataContainer container = new DataContainer();
        container.addComponent(data);
        sendData(container, connectionId);
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
    public @NotNull PacketCache getCache() {
        return cache;
    }

    private void start(long timeout, int port, @NotNull Map<ChannelOption<?>, Object> options) {
        if (group != null) {
            throw new IllegalStateException("The server is already started!");
        }

        final boolean epoll = Epoll.isAvailable();
        group = epoll ? new EpollEventLoopGroup() : new NioEventLoopGroup();
        server = new Thread(() -> {
            try {
               final Channel channel = new ServerBootstrap().group(group)
                        .channel(epoll ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                        .childHandler(new ChannelInitializer<Channel>() {
                            @Override
                            protected void initChannel(Channel channel) {
                                if(eventManager.callEvent(ListenerType.PRE_INIT, EventManager.CancelAction.INTERRUPT, new ConnectionPreInitEvent(channel))) {
                                    channel.close();
                                    return;
                                }
                                ChannelUtil.prepare(channel, options);

                                final int connectionId = id.getAndIncrement();
                                final ConnectionImpl connection = new ConnectionImpl(ServerImpl.this, channel, connectionId);

                                if (encryptionSetting != null) {
                                    try {
                                        connection.setSymmetricKey(ServerImpl.this,
                                                SymmetricEncryptionUtil.generate(encryptionSetting.getSymmetricSetting()).getSecretKey());

                                        final HmacSetting hmacSetting = encryptionSetting.getHmacSetting();
                                        if (hmacSetting != null) {
                                            connection.setHmacKey(SymmetricEncryptionUtil.generate(hmacSetting.getEncryptionSetting()).getSecretKey());
                                        }
                                    } catch (NoSuchAlgorithmException e) {
                                        e.printStackTrace();
                                    }
                                }
                                final ChannelPipeline pipeline = channel.pipeline();

                                if (timeout > 0) {
                                    pipeline.addLast("readTimeOutHandler",
                                            new TimeOutHandler(ServerImpl.this, eventManager, connection, timeout));
                                }
                                pipeline.addLast("decoder", new PacketDataDecoder(ServerImpl.this, handler, eventManager))
                                        .addAfter("decoder", "encoder", new PacketDataEncoder(ServerImpl.this));

                                connections.put(channel, connection);
                                eventManager.callEvent(ListenerType.POST_INIT, new ConnectionPostInitEvent(connection));

                            }
                        }).bind(port).sync().channel();
                ChannelUtil.prepare(channel, options);
                channel.closeFuture().syncUninterruptibly();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        server.start();
    }

    /**
     * @see de.terrarier.netlistening.Application
     */
    @Override
    public void stop() throws IllegalStateException {
        if (group == null) {
            throw new IllegalStateException("The server is already stopped!");
        }

        for (Iterator<ConnectionImpl> iterator = connections.values().iterator(); iterator.hasNext();) {
            iterator.next().disconnect0();
            iterator.remove();
        }
        handler.unregisterListeners();
        group.shutdownGracefully();
        group = null;
        server.interrupt();
        server = null;
    }

    /**
     * @see de.terrarier.netlistening.Application
     */
    @Override
    public void disconnect(@NotNull Connection connection) {
        if (!connection.isConnected()) {
            throw new IllegalStateException(
                    "The connection " + Integer.toHexString(connection.getId()) + " is not connected!");
        }

        connections.remove(connection.getChannel()).disconnect0();
    }

    /**
     * @see de.terrarier.netlistening.Application
     */
    @Override
    public void sendData(@NotNull DataContainer data, @NotNull Connection connection) {
        connection.sendData(data);
    }

    /**
     * @see de.terrarier.netlistening.Application
     */
    @Deprecated
    @Override
    public void sendData(@NotNull DataComponent<?> data, @NotNull Connection connection) {
        final DataContainer container = new DataContainer();
        container.addComponent(data);
        sendData(container, connection);
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
    @Deprecated
    @Override
    public void sendData(@NotNull DataComponent<?> data) {
        for (Connection connection : connections.values()) {
            connection.sendData(data);
        }
    }

    /**
     * @see de.terrarier.netlistening.Application
     */
    @Override
    public int getBuffer() {
        return buffer;
    }

    /**
     * @see de.terrarier.netlistening.Application
     */
    @Override
    public EncryptionSetting getEncryptionSetting() {
        return encryptionSetting;
    }

    /**
     * @see de.terrarier.netlistening.Application
     */
    @Override
    public @NotNull CompressionSetting getCompressionSetting() {
        return compressionSetting;
    }

    /**
     * @see de.terrarier.netlistening.Application
     */
    @Override
    public void registerListener(@NotNull Listener<?> listener) {
        eventManager.registerListener(listener);
    }

    /**
     * @see de.terrarier.netlistening.Application
     */
    @Override
    public @NotNull Set<Connection> getConnections() {
        return Collections.unmodifiableSet(new HashSet<>(connections.values()));
    }

    public static final class Builder {

        private final ServerImpl server;
        private final Map<ChannelOption<?>, Object> options = new HashMap<>();
        private final int port;
        private long timeout;
        private boolean built;

        public Builder(int port) {
            server = new ServerImpl();
            this.port = port;
            options.put(ChannelOption.IP_TOS, 0x18);
        }

        /**
         * @see Server.Builder
         */
        public void caching(@NotNull PacketCaching caching) {
            validate();
            server.caching = caching;
        }

        /**
         * @see Server.Builder
         */
        public void timeout(long timeout) {
            validate();
            this.timeout = timeout;
        }

        /**
         * @see Server.Builder
         */
        public void buffer(int buffer) {
            validate();
            server.buffer = buffer;
        }

        /**
         * @see Server.Builder
         */
        public void packetSynchronization(@NotNull PacketSynchronization packetSynchronization) {
            validate();
            server.packetSynchronization = packetSynchronization;
        }

        /**
         * @see Server.Builder
         */
        public void compression(@NotNull CompressionSetting compressionSetting) {
            validate();
            server.compressionSetting = compressionSetting;
        }

        /**
         * @see Server.Builder
         */
        public void stringEncoding(@NotNull Charset charset) {
            validate();
            server.stringEncoding = charset;
        }

        /**
         * @see Server.Builder
         */
        public void encryption(@NotNull EncryptionSetting encryptionSetting) {
            validate();
            server.encryptionSetting = encryptionSetting;
        }

        /**
         * @see Server.Builder
         */
        public <T> void option(@NotNull ChannelOption<T> option, T value) {
            validate();
            options.put(option, value);
        }

        /**
         * @see Server.Builder
         */
        public ServerImpl build() {
            validate();
            built = true;

            if (server.caching == PacketCaching.NONE) {
                server.caching = PacketCaching.GLOBAL;
            }

            if(server.compressionSetting == null) {
                server.compressionSetting = new CompressionSetting();
            }

            if (server.encryptionSetting != null) {
                if(!server.encryptionSetting.isInitialized()) {
                    try {
                        server.encryptionSetting.init(null);
                    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                        e.printStackTrace();
                        return null;
                    }
                }
            }
            server.start(timeout, port, options);
            return server;
        }

        private void validate() {
            if (built) {
                throw ServerAlreadyBuiltException.INSTANCE;
            }
        }

        private static final class ServerAlreadyBuiltException extends IllegalStateException {

            private static transient final ServerAlreadyBuiltException INSTANCE = new ServerAlreadyBuiltException();

            private ServerAlreadyBuiltException() {
                super("The builder can't be used anymore because the server was already built!");
            }

        }

    }

}
