package de.terrarier.netlistening.impl;

import de.terrarier.netlistening.Client;
import de.terrarier.netlistening.Connection;
import de.terrarier.netlistening.api.DataContainer;
import de.terrarier.netlistening.api.PacketCaching;
import de.terrarier.netlistening.api.compression.CompressionSetting;
import de.terrarier.netlistening.api.encryption.EncryptionSetting;
import de.terrarier.netlistening.api.encryption.ServerKey;
import de.terrarier.netlistening.api.encryption.hash.HashingAlgorithm;
import de.terrarier.netlistening.api.event.*;
import de.terrarier.netlistening.api.proxy.Proxy;
import de.terrarier.netlistening.api.proxy.ProxyType;
import de.terrarier.netlistening.network.PacketDataDecoder;
import de.terrarier.netlistening.network.PacketDataEncoder;
import de.terrarier.netlistening.network.TimeOutHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class ClientImpl extends ApplicationImpl implements Client {

    private static final PacketCaching CACHING = PacketCaching.NONE;
    private Channel channel;
    private ConnectionImpl connection;
    private volatile boolean receivedHandshake;
    private List<DataContainer> preConnectData;
    private HashingAlgorithm serverKeyHashing = HashingAlgorithm.SHA_256;
    private ServerKey serverKey;

    private void start(long timeout, int localPort, @NotNull Map<ChannelOption<?>, Object> options,
                       @NotNull SocketAddress remoteAddress, Proxy proxy) {
        if (group != null) {
            throw new IllegalStateException("The client is already running!");
        }

        if (receivedHandshake) {
            throw new IllegalStateException("The client is already stopped!");
        }
        compressionSetting = new CompressionSetting();
        serializationProvider.setEventManager(eventManager);

        final boolean epoll = Epoll.isAvailable();
        group = epoll ? new EpollEventLoopGroup() : new NioEventLoopGroup();
        worker = new Thread(() -> {
            try {
                final Bootstrap bootstrap = new Bootstrap().group(group)
                        .channel(epoll ? EpollSocketChannel.class : NioSocketChannel.class)
                        .handler(new ChannelInitializer<Channel>() {
                            @Override
                            protected void initChannel(Channel channel) {
                                if (eventManager.callEvent(ListenerType.PRE_INIT, EventManager.CancelAction.INTERRUPT,
                                        new ConnectionPreInitEvent(channel))) {
                                    channel.close();
                                    return;
                                }
                                channel.config().setOptions(options);

                                final ConnectionImpl connection = new ConnectionImpl(ClientImpl.this, channel,
                                        ID.getAndIncrement());
                                final ChannelPipeline pipeline = channel.pipeline();

                                if (timeout > 0) {
                                    pipeline.addLast(TIMEOUT_HANDLER,
                                            new TimeOutHandler(ClientImpl.this, connection, timeout));
                                }

                                pipeline.addLast(DECODER, new PacketDataDecoder(ClientImpl.this, handler))
                                        .addAfter(DECODER, ENCODER, new PacketDataEncoder(ClientImpl.this, null));

                                if (proxy != null) {
                                    pipeline.addFirst(PROXY_HANDLER, proxy.newHandler());
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
        worker.start();
    }

    @ApiStatus.Internal
    public void receiveHandshake(@NotNull CompressionSetting compressionSetting, Charset stringEncoding,
                                 EncryptionSetting encryptionSetting, byte[] serverKey) {
        this.compressionSetting = compressionSetting;

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

    @ApiStatus.Internal
    public void pushCachedData() {
        if (receivedHandshake) {
            throw new IllegalStateException("An internal error occurred - duplicate push request");
        }

        synchronized (this) {
            if (preConnectData != null) {
                for (Iterator<DataContainer> iterator = preConnectData.iterator(); iterator.hasNext();) {
                    channel.writeAndFlush(iterator.next());
                    iterator.remove();
                }
                preConnectData = null;
            }
        }
        receivedHandshake = true;
    }

    @ApiStatus.Internal
    public void sendRawData(@NotNull ByteBuf data) {
        channel.writeAndFlush(data);
    }

    /**
     * @see de.terrarier.netlistening.Application
     */
    @Override
    public void sendData(@NotNull DataContainer data) {
        if (!receivedHandshake) {
            synchronized (this) {
                if (preConnectData == null) {
                    preConnectData = new ArrayList<>();
                }
                preConnectData.add(data);
            }
            return;
        }

        channel.writeAndFlush(data);
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
    @NotNull
    @Override
    public Set<Connection> getConnections() {
        return Collections.singleton(connection);
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
    public void stop() {
        if (group == null) {
            throw new IllegalStateException("The client is not running!");
        }

        connection.disconnect0();
        handler.unregisterListeners();
        group.shutdownGracefully();
        group = null;
        worker.interrupt();
        cache.clear();
    }

    /**
     * @see de.terrarier.netlistening.Application
     */
    @Deprecated
    @Override
    public void disconnect(Connection connection) {
        disconnect();
    }

    /**
     * @see Client
     */
    @Override
    public void disconnect() {
        if (connection == null) {
            throw new IllegalStateException("The connection is not established!");
        }
        connection.disconnect();
    }

    /**
     * @see Client
     */
    @Override
    public Connection getConnection() {
        return connection;
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
        if(!eventManager.callEvent(ListenerType.KEY_CHANGE, EventManager.CancelAction.IGNORE,
                (EventManager.EventProvider<KeyChangeEvent>) () -> {
            final boolean replace = this.serverKey != null;
            final boolean hashChanged = replace && !Arrays.equals(this.serverKey.getKeyHash(), serverKey.getKeyHash());
            final KeyChangeEvent.KeyChangeResult result = replace ?
                    (hashChanged ? KeyChangeEvent.KeyChangeResult.HASH_CHANGED : KeyChangeEvent.KeyChangeResult.HASH_EQUAL)
                    : KeyChangeEvent.KeyChangeResult.HASH_ABSENT;
            return new KeyChangeEvent(replace ? this.serverKey.getKeyHash() : null, serverKey.getKeyHash(), result);
        })) return false;

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
    @ApiStatus.Internal
    public boolean hasReceivedHandshake() {
        return receivedHandshake;
    }

    @ApiStatus.Internal
    public static final class Builder extends ApplicationImpl.Builder<ClientImpl, Builder> {

        private final SocketAddress remoteAddress;
        private int localPort;
        private boolean changedHashingAlgorithm;
        private Proxy proxy;

        public Builder(@NotNull SocketAddress remoteAddress) {
            super(new ClientImpl());
            this.remoteAddress = remoteAddress;
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
        public void serverKeyHashingAlgorithm(@NotNull HashingAlgorithm hashingAlgorithm) {
            validate();
            application.serverKeyHashing = hashingAlgorithm;
            changedHashingAlgorithm = true;
        }

        /**
         * @see Client.Builder
         */
        public void serverKeyHash(byte[] bytes) {
            validate();
            application.serverKey = new ServerKey(bytes);
            if(!changedHashingAlgorithm) {
                application.serverKeyHashing = application.serverKey.getHashingAlgorithm();
            }
        }

        /**
         * @see Client.Builder
         */
        public void proxy(@NotNull SocketAddress address, @NotNull ProxyType proxyType) {
            validate();
            proxy = proxyType.getInstance(address);
        }

        /**
         * @see ApplicationImpl.Builder#build()
         */
        @Override
        void build0() {
            application.start(timeout, localPort, options, remoteAddress, proxy);
        }

        @Override
        void fail() {
            throw ClientAlreadyBuiltException.INSTANCE;
        }

        private static final class ClientAlreadyBuiltException extends IllegalStateException {

            private static final ClientAlreadyBuiltException INSTANCE = new ClientAlreadyBuiltException();

            private ClientAlreadyBuiltException() {
                super("The builder can't be used anymore because the client was already built!");
            }

        }

    }

}
