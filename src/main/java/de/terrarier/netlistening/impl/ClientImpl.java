package de.terrarier.netlistening.impl;

import de.terrarier.netlistening.Client;
import de.terrarier.netlistening.Connection;
import de.terrarier.netlistening.api.DataComponent;
import de.terrarier.netlistening.api.DataContainer;
import de.terrarier.netlistening.api.PacketCaching;
import de.terrarier.netlistening.api.compression.CompressionSetting;
import de.terrarier.netlistening.api.encryption.EncryptionSetting;
import de.terrarier.netlistening.api.encryption.ServerKey;
import de.terrarier.netlistening.api.encryption.hash.HashUtil;
import de.terrarier.netlistening.api.encryption.hash.HashingAlgorithm;
import de.terrarier.netlistening.api.event.*;
import de.terrarier.netlistening.api.proxy.Proxy;
import de.terrarier.netlistening.api.proxy.ProxyType;
import de.terrarier.netlistening.network.*;
import de.terrarier.netlistening.utils.ChannelUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class ClientImpl implements Client {

    private EventLoopGroup group;
    private Channel channel;
    private ConnectionImpl connection;
    private final PacketCache cache;
    private final DataHandler handler = new DataHandler(this);
    private static final PacketCaching CACHING = PacketCaching.NONE;
    private PacketSynchronization packetSynchronization = PacketSynchronization.NONE;
    private final String host;
    private final int targetPort;
    private int localPort;
    private int buffer = 256;
    private final EventManager eventManager;
    private CompressionSetting compressionSetting = new CompressionSetting();
    private boolean receivedHandshake;
    private List<DataContainer> preCachedData = new ArrayList<>();
    private Thread client;
    private Charset stringEncoding = StandardCharsets.UTF_8;
    private EncryptionSetting encryptionSetting;
    private HashingAlgorithm serverKeyHashing = HashingAlgorithm.SHA_256;
    private ServerKey serverKey;

    public ClientImpl(@NotNull String host, int targetPort) {
        this.host = host;
        this.targetPort = targetPort;

        cache = new PacketCache();
        eventManager = new EventManager(handler);
    }

    private <T> void start(long timeout, Map<ChannelOption<T>, T> options, Proxy proxy) {
        if (group != null) {
            throw new IllegalStateException("The client is already started!");
        }

        final boolean epoll = Epoll.isAvailable();
        group = epoll ? new EpollEventLoopGroup() : new NioEventLoopGroup();
        client = new Thread(() -> {
            try {
                final Bootstrap bootstrap = new Bootstrap().group(group)
                        .channel(epoll ? EpollSocketChannel.class : NioSocketChannel.class)
                        .handler(new ChannelInitializer<Channel>() {
                            @Override
                            protected void initChannel(Channel channel) {
                                if(eventManager.callEvent(ListenerType.PRE_INIT, EventManager.CancelAction.INTERRUPT, new ConnectionPreInitEvent(channel))) {
                                    channel.close();
                                }
                                ChannelUtil.prepare(channel, options);

                                final ConnectionImpl newConnection = new ConnectionImpl(ClientImpl.this, channel, 0x0);

                                if (timeout > 0) {
                                    channel.pipeline().addLast("readTimeOutHandler",
                                            new TimeOutHandler(ClientImpl.this, eventManager, newConnection, timeout));
                                }

                                channel.pipeline().addLast("decoder", new PacketDataDecoder(ClientImpl.this, handler, eventManager))
                                        .addAfter("decoder", "encoder", new PacketDataEncoder(ClientImpl.this));

                                if(proxy != null) {
                                    channel.pipeline().addFirst("proxyHandler", proxy.getHandler());
                                }

                                connection = newConnection;
                                eventManager.callEvent(ListenerType.POST_INIT, new ConnectionPostInitEvent(newConnection));
                            }
                        });
                ChannelFuture channelFuture;
                if (localPort > 0) {
                    final SocketAddress localAddress = new InetSocketAddress("localhost", localPort);
                    final SocketAddress remoteAddress = new InetSocketAddress(host, targetPort);
                    channelFuture = bootstrap.connect(remoteAddress, localAddress);
                } else {
                    channelFuture = bootstrap.connect(host, targetPort);
                }
                channel = channelFuture.sync().syncUninterruptibly().channel();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        client.start();
    }

    public void pushCachedData() {
        if (preCachedData == null) {
            throw new IllegalStateException("An internal error occurred - duplicate push request");
        }

        for (DataContainer data : preCachedData) {
            channel.writeAndFlush(data);
        }

        preCachedData.clear();
        preCachedData = null;
        receivedHandshake = true;
    }

    @Override
    public void sendData(@NotNull DataContainer data) {
        if (!receivedHandshake) {
            preCachedData.add(data);
            return;
        }

        channel.writeAndFlush(data);
    }

    public void sendRawData(@NotNull ByteBuf data) {
        channel.writeAndFlush(data);
    }

    @Override
    public @NotNull PacketCache getCache() {
        return cache;
    }

    @Override
    public Connection getConnection(Channel channel) {
        return connection;
    }

    @Override
    public @NotNull PacketSynchronization getPacketSynchronization() {
        return packetSynchronization;
    }

    @Override
    public @NotNull PacketCaching getCaching() {
        return CACHING;
    }

    @Override
    public @NotNull Charset getStringEncoding() {
        return stringEncoding;
    }

    public void receiveHandshake(CompressionSetting compressionSetting, @NotNull PacketSynchronization packetSynchronization,
                                 Charset stringEncoding, EncryptionSetting encryptionSetting, byte[] serverKey) {
        this.compressionSetting = compressionSetting;
        this.packetSynchronization = packetSynchronization;

        if (stringEncoding != null) {
            this.stringEncoding = stringEncoding;
        }

        if(encryptionSetting != null) {
            try {
                encryptionSetting.init(null);
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                e.printStackTrace();
            }
            this.encryptionSetting = encryptionSetting;
            setServerKey(serverKey, serverKeyHashing);
        }
    }

    @Override
    public void stop() throws IllegalStateException {
        if (group == null) {
            throw new IllegalStateException("The client is already stopped!");
        }

        connection.disconnect0();
        handler.unregisterListeners();
        group.shutdownGracefully();
        group = null;
        client.interrupt();
    }

    @Override
    public void disconnect(@NotNull Connection connection) {
        if (!connection.isConnected()) {
            throw new IllegalStateException(
                    "The connection " + Integer.toHexString(connection.getId()) + " is not connected!");
        }

        ((ConnectionImpl) connection).disconnect0();
    }

    @Override
    public void sendData(@NotNull DataContainer data, int connectionId) {
        sendData(data);
    }

    @Override
    public void sendData(@NotNull DataComponent<?> data, int connectionId) {
        sendData(data, null);
    }

    @Override
    public void sendData(@NotNull DataContainer data, Connection connection) {
        sendData(data);
    }

    @Override
    public void sendData(@NotNull DataComponent<?> data, Connection connection) {
        final DataContainer container = new DataContainer();
        container.addComponent(data);
        sendData(container);
    }

    @Override
    public void sendData(@NotNull DataComponent<?> data) {
        sendData(data, null);
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
    public CompressionSetting getCompressionSetting() {
        return compressionSetting;
    }

    @Override
    public void registerListener(@NotNull Listener<?> listener) {
        eventManager.registerListener(listener);
    }

    public boolean hasReceivedHandshake() {
        return receivedHandshake;
    }

    public ServerKey getServerKey() {
        return serverKey;
    }

    public HashingAlgorithm getServerKeyHashing() {
        return serverKeyHashing;
    }

    public boolean setServerKey(byte[] data) {
        return setServerKey(new ServerKey(data));
    }

    private boolean setServerKey(byte[] serverKeyData, @NotNull HashingAlgorithm hashingAlgorithm) {
        ServerKey serverKey;
        try {
            serverKey = new ServerKey(serverKeyData, hashingAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        }
        return setServerKey(serverKey);
    }

    private boolean setServerKey(ServerKey serverKey) {
        if(!eventManager.callEvent(ListenerType.KEY_CHANGE, EventManager.CancelAction.IGNORE, (EventManager.EventProvider<KeyChangeEvent>) () -> {
            final boolean replace = this.serverKey != null;
            final boolean hashChanged = replace && !HashUtil.isHashMatching(this.serverKey.getKeyHash(), serverKey.getKeyHash());
            final KeyChangeEvent.KeyChangeResult result = replace ?
                    (hashChanged ? KeyChangeEvent.KeyChangeResult.HASH_CHANGED : KeyChangeEvent.KeyChangeResult.HASH_EQUAL)
                    : KeyChangeEvent.KeyChangeResult.HASH_ABSENT;
            return new KeyChangeEvent(replace ? this.serverKey.getKeyHash() : null, serverKey.getKeyHash(), result);
        })) {
            return false;
        }
        this.serverKey = serverKey;
        return true;
    }

    @NotNull
    @Override
    public Set<Connection> getConnections() {
        return Collections.singleton(connection);
    }

    public static class Builder {

        private final ClientImpl client;
        private long timeout;
        private final Map options = new HashMap<>();
        private boolean changedHashingAlgorithm;
        private Proxy proxy;
        private boolean built;

        @SuppressWarnings("unchecked")
        public Builder(@NotNull String host, int targetPort) {
            client = new ClientImpl(host, targetPort);
            options.put(ChannelOption.IP_TOS, 0x18);
        }

        public void timeout(long timeout) {
            validate();
            this.timeout = timeout;
        }

        public void localPort(int localPort) {
            validate();
            client.localPort = localPort;
        }

        public void buffer(int buffer) {
            validate();
            client.buffer = buffer;
        }

        public void serverKeyHashingAlgorithm(@NotNull HashingAlgorithm hashingAlgorithm) {
            validate();
            client.serverKeyHashing = hashingAlgorithm;
            changedHashingAlgorithm = true;
        }

        public void serverKeyHash(byte[] bytes) {
            validate();
            client.serverKey = new ServerKey(bytes);
            if(!changedHashingAlgorithm) {
                client.serverKeyHashing = client.serverKey.getHashingAlgorithm();
            }
        }

        @SuppressWarnings("unchecked")
        public <T> void option(@NotNull ChannelOption<T> option, T value) {
            validate();
            options.put(option, value);
        }

        public void proxy(@NotNull SocketAddress address, @NotNull ProxyType proxyType) {
            validate();
            this.proxy = proxyType.getInstance(address);
        }

        @SuppressWarnings("unchecked")
        @NotNull
        public ClientImpl build() {
            validate();
            built = true;
            client.start(timeout, options, proxy);
            return client;
        }

        private void validate() {
            if (built) {
                throw new ClientAlreadyBuiltException();
            }
        }

        private static final class ClientAlreadyBuiltException extends IllegalStateException {

            public ClientAlreadyBuiltException() {
                super("The builder can't be used anymore because the client was already built!");
            }

        }

    }

}
