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
package de.terrarier.netlistening.impl;

import de.terrarier.netlistening.Application;
import de.terrarier.netlistening.Client;
import de.terrarier.netlistening.Connection;
import de.terrarier.netlistening.api.DataContainer;
import de.terrarier.netlistening.api.PacketCaching;
import de.terrarier.netlistening.api.compression.CompressionSetting;
import de.terrarier.netlistening.api.encryption.EncryptionSetting;
import de.terrarier.netlistening.api.encryption.ServerKey;
import de.terrarier.netlistening.api.encryption.hash.HashingAlgorithm;
import de.terrarier.netlistening.api.event.ConnectionPostInitEvent;
import de.terrarier.netlistening.api.event.EventManager;
import de.terrarier.netlistening.api.event.KeyChangeEvent;
import de.terrarier.netlistening.api.event.ListenerType;
import de.terrarier.netlistening.api.proxy.Proxy;
import de.terrarier.netlistening.api.proxy.ProxyType;
import de.terrarier.netlistening.api.serialization.SerializationUtil;
import de.terrarier.netlistening.internal.AssumeNotNull;
import de.terrarier.netlistening.util.UDS;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

/**
 * @author Terrarier2111
 * @since 1.0
 */
public final class ClientImpl extends ApplicationImpl implements Client {

    private static final PacketCaching CACHING = PacketCaching.NONE;
    private Channel channel;
    private ConnectionImpl connection;
    private volatile boolean receivedHandshake;
    private List<DataContainer> preConnectData;
    private HashingAlgorithm serverKeyHashing = HashingAlgorithm.SHA_256;
    private ServerKey serverKey;

    private ClientImpl() {
        compressionSetting = new CompressionSetting();
    }

    @AssumeNotNull
    private Bootstrap start(long timeout, @AssumeNotNull Map<ChannelOption<?>, Object> options,
                            boolean uds, @Nullable Proxy proxy) {
        if (group == null) {
            throw new IllegalStateException("The client is already stopped!");
        }
        SerializationUtil.init(this, serializationProvider);

        return new Bootstrap().group(group)
                .channelFactory(UDS.channelFactory(uds))
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected final void initChannel(@AssumeNotNull Channel channel) {
                        final ConnectionImpl connection = prepareConnectionInitially(channel, timeout, options, null,
                                Integer.MAX_VALUE);
                        if (proxy != null) {
                            channel.pipeline().addFirst(PROXY_HANDLER, proxy.newHandler());
                        }

                        ClientImpl.this.connection = connection;
                        eventManager.callEvent(ListenerType.POST_INIT, new ConnectionPostInitEvent(connection));
                    }
                });
    }

    @ApiStatus.Internal
    public void receiveHandshake(@AssumeNotNull CompressionSetting compressionSetting, Charset stringEncoding,
                                 EncryptionSetting encryptionSetting, byte[] serverKey) {
        this.compressionSetting = compressionSetting;

        if (stringEncoding != null) {
            this.stringEncoding = stringEncoding;
        }

        if (encryptionSetting != null) {
            try {
                encryptionSetting.init(null);
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                e.printStackTrace();
                return;
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
                final ChannelPromise voidPromise = channel.voidPromise();
                for (Iterator<DataContainer> iterator = preConnectData.iterator(); iterator.hasNext(); ) {
                    channel.writeAndFlush(iterator.next(), voidPromise);
                    iterator.remove();
                }
                preConnectData = null;
            }
        }
        receivedHandshake = true;
    }

    @ApiStatus.Internal
    public void sendRawData(@AssumeNotNull ByteBuf data) {
        channel.writeAndFlush(data, channel.voidPromise());
    }

    /**
     * @see Application#sendData(DataContainer)
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

        channel.writeAndFlush(data, channel.voidPromise());
    }

    /**
     * @see Application#getConnections()
     */
    @AssumeNotNull
    @Override
    public Set<Connection> getConnections() {
        return Collections.singleton(connection);
    }

    @AssumeNotNull
    @Override
    public Collection<ConnectionImpl> getConnectionsRaw() {
        // currently unused as of 1.09
        return Collections.singleton(connection);
    }

    /**
     * @see ApplicationImpl#getCaching()
     */
    @AssumeNotNull
    @Override
    public PacketCaching getCaching() {
        return CACHING;
    }

    /**
     * @see Application#stop()
     */
    @Override
    public void stop() {
        if (group == null) {
            throw new IllegalStateException("The client is not running!");
        }

        if (connection != null && connection.isConnected()) {
            connection.disconnect0();
        }

        handler.unregisterListeners();
        group.shutdownGracefully();
        group = null;
        worker.interrupt();
        worker = null;
        cache.clear();
    }

    /**
     * @see Client#getConnection()
     */
    @Override
    public Connection getConnection() {
        return connection;
    }

    /**
     * @see Client#getServerKey()
     */
    @Override
    public ServerKey getServerKey() {
        return serverKey;
    }

    /**
     * @see Client#setServerKey(byte[])
     */
    @Override
    public boolean setServerKey(byte @NotNull [] data) {
        return setServerKey(new ServerKey(data));
    }

    private void setServerKey(@AssumeNotNull byte[] serverKeyData, @AssumeNotNull HashingAlgorithm hashingAlgorithm) {
        final ServerKey serverKey;
        try {
            serverKey = new ServerKey(serverKeyData, hashingAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return;
        }
        setServerKey(serverKey);
    }

    private boolean setServerKey(@AssumeNotNull ServerKey serverKey) {
        final boolean replace = this.serverKey != null;
        final boolean hashChanged = replace && !Arrays.equals(this.serverKey.getKeyHash(), serverKey.getKeyHash());
        final KeyChangeEvent.KeyChangeResult result = replace ?
                (hashChanged ? KeyChangeEvent.KeyChangeResult.HASH_CHANGED : KeyChangeEvent.KeyChangeResult.HASH_EQUAL)
                : KeyChangeEvent.KeyChangeResult.HASH_ABSENT;
        final KeyChangeEvent event = new KeyChangeEvent(replace ? this.serverKey.getKeyHash() : null, serverKey.getKeyHash(), result);

        if (!eventManager.callEvent(ListenerType.KEY_CHANGE, EventManager.CancelAction.IGNORE, event)) {
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
    @ApiStatus.Internal
    public boolean hasReceivedHandshake() {
        return receivedHandshake;
    }

    @ApiStatus.Internal
    public static final class Builder extends ApplicationImpl.Builder<ClientImpl, Builder> {

        private final SocketAddress remoteAddress;
        private final String filePath;
        private int localPort;
        private boolean changedHashingAlgorithm;
        private Proxy proxy;

        public Builder(@AssumeNotNull SocketAddress remoteAddress) {
            super(new ClientImpl());
            this.remoteAddress = remoteAddress;
            filePath = null;
        }

        public Builder(@AssumeNotNull String filePath) {
            super(new ClientImpl());
            if (!UDS.isAvailable(false)) {
                throw new UnsupportedOperationException("UDS are not supported in this environment.");
            }
            this.filePath = filePath;
            remoteAddress = null;
        }

        /**
         * @see Client.Builder#localPort(int)
         */
        public void localPort(int localPort) {
            validate();
            if (filePath != null) {
                throw new UnsupportedOperationException("You may not specify a local port when using UDS.");
            }
            this.localPort = localPort;
        }

        /**
         * @see Client.Builder#serverKeyHashingAlgorithm(HashingAlgorithm)
         */
        public void serverKeyHashingAlgorithm(@AssumeNotNull HashingAlgorithm hashingAlgorithm) {
            validate();
            application.serverKeyHashing = hashingAlgorithm;
            changedHashingAlgorithm = true;
        }

        /**
         * @see Client.Builder#serverKeyHash(byte[])
         */
        public void serverKeyHash(@AssumeNotNull byte[] bytes) {
            validate();
            application.serverKey = new ServerKey(bytes);
            if (!changedHashingAlgorithm) {
                application.serverKeyHashing = application.serverKey.getHashingAlgorithm();
            }
        }

        /**
         * @see Client.Builder#proxy(SocketAddress, ProxyType)
         */
        public void proxy(@AssumeNotNull SocketAddress address, @AssumeNotNull ProxyType proxyType) {
            validate();
            if (filePath != null) {
                throw new UnsupportedOperationException("You may not specify a proxy when using UDS.");
            }
            proxy = proxyType.getInstance(address);
        }

        /**
         * @see ApplicationImpl.Builder#build()
         */
        @Override
        void build0() {
            final boolean uds = filePath != null;
            application.worker = new Thread(() -> {
                final Bootstrap bootstrap = application.start(timeout, options, uds, uds ? null : proxy);
                final ChannelFuture channelFuture;
                if (uds) {
                    channelFuture = bootstrap.connect(UDS.domainSocketAddress(filePath));
                } else {
                    if (localPort > 0) {
                        final SocketAddress localAddress = new InetSocketAddress("localhost", localPort);
                        channelFuture = bootstrap.connect(remoteAddress, localAddress);
                    } else {
                        channelFuture = bootstrap.connect(remoteAddress);
                    }
                }
                try {
                    application.channel = channelFuture.sync().syncUninterruptibly().channel();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            application.worker.start();
        }

    }

}
