package de.terrarier.netlistening.impl;

import de.terrarier.netlistening.Connection;
import de.terrarier.netlistening.Server;
import de.terrarier.netlistening.api.DataComponent;
import de.terrarier.netlistening.api.DataContainer;
import de.terrarier.netlistening.api.encryption.EncryptionSetting;
import de.terrarier.netlistening.api.encryption.SymmetricEncryptionUtil;
import de.terrarier.netlistening.api.encryption.hash.HmacSetting;
import de.terrarier.netlistening.api.event.*;
import de.terrarier.netlistening.network.*;
import de.terrarier.netlistening.utils.ChannelUtil;
import de.terrarier.netlistening.api.PacketCaching;
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

    private final Map<Integer, Connection> connections = new ConcurrentHashMap<>();
    private EventLoopGroup group;
    private final AtomicInteger id = new AtomicInteger();
    private final PacketCache cache;
    private final int port;
    private int buffer = 256;
    private PacketCaching caching = PacketCaching.NONE;
    private PacketSynchronization packetSynchronization = PacketSynchronization.NONE;
    private final DataHandler handler = new DataHandler(this);
    private final EventManager eventManager;
    private boolean useVarIntCompression;
    private Thread server;
    private Charset stringEncoding = StandardCharsets.UTF_8;
    private EncryptionSetting encryptionSetting;

    private ServerImpl(int port) {
        this.port = port;
        cache = new PacketCache();
        eventManager = new EventManager(handler);
    }

    @Override
    public boolean isVarIntCompressionEnabled() {
        return useVarIntCompression;
    }

    @Override
    public @NotNull PacketCaching getCaching() {
        return caching;
    }

    @Override
    public @NotNull PacketSynchronization getPacketSynchronization() {
        return packetSynchronization;
    }

    @Override
    public @NotNull Charset getStringEncoding() {
        return stringEncoding;
    }

    @Override
    public void sendData(@NotNull DataContainer data, int connectionId) {
        final Connection connection = connections.get(connectionId);

        if (connection == null) {
            throw new IllegalArgumentException("There is no connection with the id " + Integer.toHexString(connectionId) + ".");
        }

        connection.sendData(data);
    }

    @Override
    public void sendData(@NotNull DataComponent<?> data, int connectionId) {
        final DataContainer container = new DataContainer();
        container.addComponent(data);
        sendData(container, connectionId);
    }

    @Override
    public Connection getConnection(@NotNull Channel channel) {
        for (Integer connectionId : connections.keySet()) {
            final Connection connection = connections.get(connectionId);
            if (connection.getChannel().equals(channel)) {
                return connection;
            }
        }
        return null;
    }

    @Override
    public @NotNull PacketCache getCache() {
        return cache;
    }

    private <T> void start(long timeout, Map<ChannelOption<T>, T> options) {
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
                                }
                                ChannelUtil.prepare(channel, options);

                                final int connectionId = id.getAndIncrement();
                                final ConnectionImpl connection = new ConnectionImpl(ServerImpl.this, channel, connectionId);

                                if (encryptionSetting != null) {
                                    try {
                                        connection.setSymmetricKey(ServerImpl.this, SymmetricEncryptionUtil.generate(encryptionSetting.getSymmetricSetting()).getSecretKey());

                                        HmacSetting hmacSetting = encryptionSetting.getHmacSetting();
                                        if (hmacSetting != null) {
                                            connection.setHmacKey(SymmetricEncryptionUtil.generate(hmacSetting.getEncryptionSetting()).getSecretKey());
                                        }
                                    } catch (NoSuchAlgorithmException e) {
                                        e.printStackTrace();
                                    }
                                }
                                final ChannelPipeline pipeline = channel.pipeline();

                                if (timeout > 0) {
                                    pipeline.addLast("readTimeOutHandler", new TimeOutHandler(ServerImpl.this, eventManager, connection, timeout));
                                }
                                pipeline.addLast("decoder", new PacketDataDecoder(ServerImpl.this, handler, eventManager))
                                        .addAfter("decoder", "encoder", new PacketDataEncoder(ServerImpl.this));

                                connections.put(connectionId, connection);
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

    @Override
    public void stop() throws IllegalStateException {
        if (group == null) {
            throw new IllegalStateException("The server is already stopped!");
        }

        for (Connection connection : connections.values()) {
            ((ConnectionImpl) connection).disconnect0();
        }
        connections.clear();
        handler.unregisterListeners();
        group.shutdownGracefully();
        group = null;
        server.interrupt();
        server = null;
    }

    @Override
    public void disconnect(@NotNull Connection connection) {
        if (!connection.isConnected()) {
            throw new IllegalStateException(
                    "The connection " + Integer.toHexString(connection.getId()) + " is not connected!");
        }

        ((ConnectionImpl) connections.remove(connection.getId())).disconnect0();
    }

    @Override
    public void sendData(@NotNull DataContainer data, @NotNull Connection connection) {
        connection.sendData(data);
    }

    @Override
    public void sendData(@NotNull DataComponent<?> data, @NotNull Connection connection) {
        final DataContainer container = new DataContainer();
        container.addComponent(data);
        sendData(container, connection);
    }

    @Override
    public void sendData(@NotNull DataContainer data) {
        for (Connection connection : connections.values()) {
            connection.sendData(data);
        }
    }

    @Override
    public void sendData(@NotNull DataComponent<?> data) {
        for (Connection connection : connections.values()) {
            connection.sendData(data);
        }
    }

    @Override
    public int getBuffer() {
        return buffer;
    }

    @Override
    public EncryptionSetting getEncryptionSetting() {
        return encryptionSetting;
    }

    @Override
    public void registerListener(@NotNull Listener<?> listener) {
        eventManager.registerListener(listener);
    }

    @Override
    public @NotNull Set<Connection> getConnections() {
        return Collections.unmodifiableSet(new HashSet<>(connections.values()));
    }

    public static class Builder {

        private final ServerImpl server;
        private long timeout;
        private final Map options = new HashMap<>();
        private boolean built;

        @SuppressWarnings("unchecked")
        public Builder(int port) {
            server = new ServerImpl(port);
            options.put(ChannelOption.IP_TOS, 0x18);
        }

        public void caching(@NotNull PacketCaching caching) {
            validate();
            server.caching = caching;
        }

        public void timeout(long timeout) {
            validate();
            this.timeout = timeout;
        }

        public void buffer(int buffer) {
            validate();
            server.buffer = buffer;
        }

        public void packetSynchronization(@NotNull PacketSynchronization packetSynchronization) {
            validate();
            server.packetSynchronization = packetSynchronization;
        }

        public void varIntCompression(boolean enabled) {
            validate();
            server.useVarIntCompression = enabled;
        }

        public void stringEncoding(@NotNull Charset charset) {
            validate();
            server.stringEncoding = charset;
        }

        public void encryption(@NotNull EncryptionSetting encryptionSetting) {
            validate();
            server.encryptionSetting = encryptionSetting;
        }

        @SuppressWarnings("unchecked")
        public <T> void option(ChannelOption<T> option, T value) {
            validate();
            options.put(option, value);
        }

        @SuppressWarnings("unchecked")
        public ServerImpl build() {
            validate();
            built = true;

            if (server.caching == PacketCaching.NONE) {
                server.caching = PacketCaching.GLOBAL;
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
            server.start(timeout, options);
            return server;
        }

        private void validate() {
            if (built) {
                throw new ServerAlreadyBuiltException();
            }
        }

        private static final class ServerAlreadyBuiltException extends IllegalStateException {

            public ServerAlreadyBuiltException() {
                super("The builder can't be used anymore because the server was already built!");
            }

        }

    }

}
