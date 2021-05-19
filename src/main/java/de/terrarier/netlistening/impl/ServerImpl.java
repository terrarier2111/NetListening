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
import de.terrarier.netlistening.internals.AssumeNotNull;
import de.terrarier.netlistening.network.PacketDataDecoder;
import de.terrarier.netlistening.network.PacketDataEncoder;
import de.terrarier.netlistening.network.TimeOutHandler;
import de.terrarier.netlistening.utils.UDS;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.concurrent.*;

import static de.terrarier.netlistening.utils.ObjectUtilFallback.checkPositiveOrZero;

/**
 * @author Terrarier2111
 * @since 1.0
 */
public final class ServerImpl extends ApplicationImpl implements Server {

    private final Map<Channel, ConnectionImpl> connections = new ConcurrentHashMap<>();
    private PacketCaching caching = PacketCaching.NONE;
    private ScheduledExecutorService delayedExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {

        final ThreadFactory factory = Executors.defaultThreadFactory();

        @Override
        public Thread newThread(@NotNull Runnable runnable) {
            final Thread thread = factory.newThread(runnable);
            thread.setDaemon(true);
            return thread;
        }
    });

    private ServerBootstrap start(long timeout, @AssumeNotNull Map<ChannelOption<?>, Object> options,
                                  boolean uds) {
        if (group != null) {
            throw new IllegalStateException("The server is already started!");
        }
        serializationProvider.setEventManager(eventManager);

        group = UDS.eventLoopGroup();
        return new ServerBootstrap().group(group)
                .channel(UDS.serverChannel(uds))
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel channel) {
                        if (eventManager.callEvent(ListenerType.PRE_INIT, EventManager.CancelAction.INTERRUPT,
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
                });
    }

    /**
     * @see ApplicationImpl#getCaching()
     */
    @AssumeNotNull
    @Override
    public PacketCaching getCaching() {
        return caching;
    }

    /**
     * @see Server#getConnection(Channel)
     */
    @Override
    public Connection getConnection(@NotNull Channel channel) {
        return connections.get(channel);
    }

    /**
     * @see Server#getConnection(int)
     */
    @Override
    public Connection getConnection(int id) {
        if (checkPositiveOrZero(id, "id") > ConnectionImpl.ID.get()) {
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
     * @see Application#stop()
     */
    @Override
    public void stop() {
        if (group == null) {
            throw new IllegalStateException("The server is already stopped!");
        }

        for (Iterator<ConnectionImpl> iterator = connections.values().iterator(); iterator.hasNext(); ) {
            final ConnectionImpl connection = iterator.next();
            connection.disconnect0();
            iterator.remove();
        }
        handler.unregisterListeners();
        group.shutdownGracefully();
        group = null;
        worker.interrupt();
        worker = null;
        cache.getPackets().clear();
        if (!delayedExecutor.isShutdown()) {
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

    void disconnect0(@AssumeNotNull Connection connection) {
        connections.remove(connection.getChannel());
    }

    /**
     * @see Application#sendData(DataContainer)
     */
    @Override
    public void sendData(@NotNull DataContainer data) {
        for (Connection connection : connections.values()) {
            connection.sendData(data);
        }
    }

    /**
     * @see Application#getConnections()
     */
    @AssumeNotNull
    @Override
    public Set<Connection> getConnections() {
        return Collections.unmodifiableSet(new HashSet<>(connections.values()));
    }

    @AssumeNotNull
    @Override
    public Collection<ConnectionImpl> getConnectionsRaw() {
        return connections.values();
    }

    @ApiStatus.Internal
    public static final class Builder extends ApplicationImpl.Builder<ServerImpl, Builder> {

        private final int port;
        private final String filePath;

        public Builder(int port) {
            super(new ServerImpl());
            this.port = port;
            filePath = null;
        }

        public Builder(@AssumeNotNull String filePath) {
            super(new ServerImpl());
            if(!UDS.isAvailable(true)) {
                throw new UnsupportedOperationException("UDS are not supported in this environment.");
            }
            this.filePath = filePath;
            port = 0;
        }

        /**
         * @see Server.Builder#caching(PacketCaching)
         */
        public void caching(@AssumeNotNull PacketCaching caching) {
            validate();
            application.caching = caching;
        }

        /**
         * @see Server.Builder#compression(CompressionSetting)
         */
        public void compression(@AssumeNotNull CompressionSetting compressionSetting) {
            validate();
            application.compressionSetting = compressionSetting;
        }

        /**
         * @see Server.Builder#stringEncoding(Charset)
         */
        public void stringEncoding(@AssumeNotNull Charset charset) {
            validate();
            application.stringEncoding = charset;
        }

        /**
         * @see Server.Builder#encryption(EncryptionSetting)
         */
        public void encryption(@AssumeNotNull EncryptionSetting encryptionSetting) {
            validate();
            application.encryptionSetting = encryptionSetting;
        }

        /**
         * @see ApplicationImpl.Builder#build()
         */
        @Override
        void build0() {
            if (application.caching == PacketCaching.NONE) {
                application.caching = PacketCaching.GLOBAL;
            }

            if (application.compressionSetting == null) {
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
            application.worker = new Thread(() -> {
                try {
                    final Channel channel;
                    if(filePath != null) {
                        channel = application.start(timeout, options, true).bind(
                                UDS.domainSocketAddress(filePath)).sync().channel();
                    }else {
                        channel = application.start(timeout, options, false).bind(port).sync().channel();
                    }
                    channel.config().setOptions(options);
                    channel.closeFuture().syncUninterruptibly();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            application.worker.start();
        }

    }

}
