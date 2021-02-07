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
import de.terrarier.netlistening.api.serialization.JavaIoSerializationProvider;
import de.terrarier.netlistening.api.serialization.SerializationProvider;
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

    private static final PacketCaching CACHING = PacketCaching.NONE;
    private final PacketCache cache = new PacketCache();
    private final DataHandler handler = new DataHandler(this);
    private final EventManager eventManager = new EventManager(handler);
    private EventLoopGroup group;
    private Thread client;
    private Channel channel;
    private ConnectionImpl connection;
    private PacketSynchronization packetSynchronization = PacketSynchronization.NONE;
    private int buffer = 256;
    private CompressionSetting compressionSetting = new CompressionSetting();
    private boolean receivedHandshake;
    private List<DataContainer> preConnectData;
    private Charset stringEncoding = StandardCharsets.UTF_8;
    private EncryptionSetting encryptionSetting;
    private HashingAlgorithm serverKeyHashing = HashingAlgorithm.SHA_256;
    private ServerKey serverKey;
    private SerializationProvider serializationProvider = new JavaIoSerializationProvider();
    // TODO: Improve and test delayed data sending mechanics.

    private void start(long timeout, int localPort, @NotNull Map<ChannelOption<?>, Object> options, @NotNull SocketAddress remoteAddress, Proxy proxy) {
        if (group != null) {
            throw new IllegalStateException("The client is already running!");
        }

        if (receivedHandshake) {
            throw new IllegalStateException("The client is already stopped!");
        }
        serializationProvider.setEventManager(eventManager);

        final boolean epoll = Epoll.isAvailable();
        group = epoll ? new EpollEventLoopGroup() : new NioEventLoopGroup();
        client = new Thread(() -> {
            try {
                final Bootstrap bootstrap = new Bootstrap().group(group)
                        .channel(epoll ? EpollSocketChannel.class : NioSocketChannel.class)
                        .handler(new ChannelInitializer<Channel>() {
                            @Override
                            protected void initChannel(Channel channel) {
                                if (eventManager.callEvent(ListenerType.PRE_INIT, EventManager.CancelAction.INTERRUPT, new ConnectionPreInitEvent(channel))) {
                                    channel.close();
                                    return;
                                }
                                ChannelUtil.prepare(channel, options);

                                final ConnectionImpl connection = new ConnectionImpl(ClientImpl.this, channel, 0x0);
                                final ChannelPipeline pipeline = channel.pipeline();

                                if (timeout > 0) {
                                    pipeline.addLast("readTimeOutHandler",
                                            new TimeOutHandler(ClientImpl.this, eventManager, connection, timeout));
                                }

                                pipeline.addLast("decoder", new PacketDataDecoder(ClientImpl.this, handler, eventManager))
                                        .addAfter("decoder", "encoder", new PacketDataEncoder(ClientImpl.this));

                                if (proxy != null) {
                                    pipeline.addFirst("proxyHandler", proxy.getHandler());
                                }

                                ClientImpl.this.connection = connection;
                                eventManager.callEvent(ListenerType.POST_INIT, new ConnectionPostInitEvent(connection));
                            }
                        });
                final ChannelFuture channelFuture;
                if (localPort > 0) {
                    final SocketAddress localAddress = new InetSocketAddress("localhost", localPort);
                    channelFuture = bootstrap.connect(remoteAddress, localAddress);
                } else {
                    channelFuture = bootstrap.connect(remoteAddress);
                }
                channel = channelFuture.sync().syncUninterruptibly().channel();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        client.start();
    }

    public void receiveHandshake(@NotNull CompressionSetting compressionSetting, @NotNull PacketSynchronization packetSynchronization,
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

    public void pushCachedData() {
        if (receivedHandshake) {
            throw new IllegalStateException("An internal error occurred - duplicate push request");
        }

        if(preConnectData != null) {
            for (Iterator<DataContainer> iterator = preConnectData.iterator(); iterator.hasNext();) {
                channel.writeAndFlush(iterator.next());
                iterator.remove();
            }
            preConnectData = null;
        }
        receivedHandshake = true;
    }

    public void sendRawData(@NotNull ByteBuf data) {
        channel.writeAndFlush(data);
    }

    /**
     * @see de.terrarier.netlistening.Application
     */
    @Override
    public void sendData(@NotNull DataContainer data) {
        if (!receivedHandshake) {
            if(preConnectData == null) {
                preConnectData = new ArrayList<>();
            }
            preConnectData.add(data);
            return;
        }

        channel.writeAndFlush(data);
    }

    /**
     * @see de.terrarier.netlistening.Application
     */
    @Override
    public @NotNull PacketCache getCache() {
        return cache;
    }

    /**
     * @see de.terrarier.netlistening.Application
     */
    @Override
    public Connection getConnection(Channel channel) {
        return connection;
    }

    /**
     * @see de.terrarier.netlistening.Application
     */
    @Override
    public Connection getConnection(int id) {
        return connection;
    }

    /**
     * @see de.terrarier.netlistening.Application
     */
    @NotNull
    @Override
    public Set<Connection> getConnections() {
        return Collections.singleton(connection);
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
    public @NotNull PacketCaching getCaching() {
        return CACHING;
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
    @Override
    public void stop() throws IllegalStateException {
        if (group == null) {
            throw new IllegalStateException("The client is not running!");
        }

        connection.disconnect0();
        handler.unregisterListeners();
        group.shutdownGracefully();
        group = null;
        client.interrupt();
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

        ((ConnectionImpl) connection).disconnect0();
    }

    /**
     * @see de.terrarier.netlistening.Application
     */
    @Deprecated
    @Override
    public void sendData(@NotNull DataContainer data, int connectionId) {
        sendData(data);
    }

    /**
     * @see de.terrarier.netlistening.Application
     */
    @Deprecated
    @Override
    public void sendData(@NotNull DataComponent<?> data, int connectionId) {
        sendData(data);
    }

    /**
     * @see de.terrarier.netlistening.Application
     */
    @Override
    public void sendData(@NotNull DataContainer data, Connection connection) {
        sendData(data);
    }

    /**
     * @see de.terrarier.netlistening.Application
     */
    @Deprecated
    @Override
    public void sendData(@NotNull DataComponent<?> data, Connection connection) {
        final DataContainer container = new DataContainer();
        container.addComponent(data);
        sendData(container);
    }

    /**
     * @see de.terrarier.netlistening.Application
     */
    @Deprecated
    @Override
    public void sendData(@NotNull DataComponent<?> data) {
        sendData(data);
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
    @NotNull
    @Override
    public CompressionSetting getCompressionSetting() {
        return compressionSetting;
    }

    /**
     * @see de.terrarier.netlistening.Application
     */
    @NotNull
    @Override
    public SerializationProvider getSerializationProvider() {
        return serializationProvider;
    }

    /**
     * @see de.terrarier.netlistening.Application
     */
    @Override
    public void registerListener(@NotNull Listener<?> listener) {
        eventManager.registerListener(listener);
    }

    /**
     * @see Client
     */
    @Override
    public ServerKey getServerKey() {
        return serverKey;
    }

    /**
     * @see Client
     */
    @Override
    public boolean setServerKey(byte[] data) {
        return setServerKey(new ServerKey(data));
    }

    private void setServerKey(byte[] serverKeyData, @NotNull HashingAlgorithm hashingAlgorithm) {
        final ServerKey serverKey;
        try {
            serverKey = new ServerKey(serverKeyData, hashingAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return;
        }
        setServerKey(serverKey);
    }

    private boolean setServerKey(@NotNull ServerKey serverKey) {
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

    /**
     * @return the hashing algorithm used to hash the server key.
     */
    public HashingAlgorithm getServerKeyHashing() {
        return serverKeyHashing;
    }

    /**
     * @return if the client has already received a handshake (and push request)
     * from the server.
     */
    public boolean hasReceivedHandshake() {
        return receivedHandshake;
    }

    public static final class Builder {

        private final ClientImpl client = new ClientImpl();
        private final Map<ChannelOption<?>, Object> options = new HashMap<>();
        private final SocketAddress remoteAddress;
        private long timeout;
        private int localPort;
        private boolean changedHashingAlgorithm;
        private Proxy proxy;
        private boolean built;

        public Builder(@NotNull SocketAddress remoteAddress) {
            this.remoteAddress = remoteAddress;
            // https://en.wikipedia.org/wiki/Type_of_service
            options.put(ChannelOption.IP_TOS, 0x18);
        }

        /**
         * @see Client.Builder
         */
        public void timeout(long timeout) {
            validate();
            this.timeout = timeout;
        }

        /**
         * @see Client.Builder
         */
        public void localPort(int localPort) {
            validate();
            this.localPort = localPort;
        }

        /**
         * @see Client.Builder
         */
        public void buffer(int buffer) {
            validate();
            client.buffer = buffer;
        }

        /**
         * @see Client.Builder
         */
        public void serverKeyHashingAlgorithm(@NotNull HashingAlgorithm hashingAlgorithm) {
            validate();
            client.serverKeyHashing = hashingAlgorithm;
            changedHashingAlgorithm = true;
        }

        /**
         * @see Client.Builder
         */
        public void serverKeyHash(byte[] bytes) {
            validate();
            client.serverKey = new ServerKey(bytes);
            if(!changedHashingAlgorithm) {
                client.serverKeyHashing = client.serverKey.getHashingAlgorithm();
            }
        }

        /**
         * @see Client.Builder
         */
        public <T> void option(@NotNull ChannelOption<T> option, T value) {
            validate();
            options.put(option, value);
        }

        /**
         * @see Client.Builder
         */
        public void serialization(@NotNull SerializationProvider serializationProvider) {
            validate();
            client.serializationProvider = serializationProvider;
        }

        /**
         * @see Client.Builder
         */
        public void proxy(@NotNull SocketAddress address, @NotNull ProxyType proxyType) {
            validate();
            proxy = proxyType.getInstance(address);
        }

        /**
         * @see Client.Builder
         */
        @NotNull
        public ClientImpl build() {
            validate();
            built = true;
            client.start(timeout, localPort, options, remoteAddress, proxy);
            return client;
        }

        private void validate() {
            if (built) {
                throw ClientAlreadyBuiltException.INSTANCE;
            }
        }

        private static final class ClientAlreadyBuiltException extends IllegalStateException {

            private static transient final ClientAlreadyBuiltException INSTANCE = new ClientAlreadyBuiltException();

            private ClientAlreadyBuiltException() {
                super("The builder can't be used anymore because the client was already built!");
            }

        }

    }

}
