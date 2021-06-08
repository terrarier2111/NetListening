package de.terrarier.netlistening.test.benchmark;

import de.terrarier.netlistening.Client;
import de.terrarier.netlistening.Server;
import de.terrarier.netlistening.impl.ClientImpl;
import de.terrarier.netlistening.internals.InternalUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Byte.MAX_VALUE;
import static java.lang.Byte.MIN_VALUE;

public class VoidPromise {

    private static final int ITERATIONS = 25000;
    private static final AtomicInteger PORT = new AtomicInteger(8839);

    @State(Scope.Thread)
    public static class ConnectionsState {

        public Server server;
        public ClientImpl client;

        @Setup
        public void doSetup() {
            final int curr = PORT.getAndIncrement();
            server = Server.builder(curr).build();
            try {
                Thread.sleep(50L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            client = (ClientImpl) Client.builder("localhost", curr).build();
            try {
                Thread.sleep(50L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @TearDown
        public void doTearDown() {
            server.stop();
            client.stop();
        }

    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void benchmarkNonVoidPromise(ConnectionsState state) {
        int counter = MIN_VALUE;
        final ClientImpl client = state.client;
        Channel channel = client.getConnection().getChannel();
        for (int i = 0; i < ITERATIONS; i++) {
            ByteBuf buffer = Unpooled.buffer(InternalUtil.getSingleByteSize(client) + 1);
            InternalUtil.writeIntUnchecked(client, buffer, 0x1);
            buffer.markWriterIndex();
            if (counter == MAX_VALUE) {
                counter = MIN_VALUE;
            }

            buffer.resetWriterIndex();
            buffer.writeByte(++counter);
            buffer.retain();
            channel.writeAndFlush(buffer);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void benchmarkVoidPromise(ConnectionsState state) {
        int counter = MIN_VALUE;
        final ClientImpl client = state.client;
        Channel channel = client.getConnection().getChannel();
        for (int i = 0; i < ITERATIONS; i++) {
            ByteBuf buffer = Unpooled.buffer(InternalUtil.getSingleByteSize(client) + 1);
            InternalUtil.writeIntUnchecked(client, buffer, 0x1);
            buffer.markWriterIndex();
            if (counter == MAX_VALUE) {
                counter = MIN_VALUE;
            }

            buffer.resetWriterIndex();
            buffer.writeByte(++counter);
            buffer.retain();
            channel.writeAndFlush(buffer);
        }
    }

}
