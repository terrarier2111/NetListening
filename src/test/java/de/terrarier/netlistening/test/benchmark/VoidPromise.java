package de.terrarier.netlistening.test.benchmark;

import de.terrarier.netlistening.Client;
import de.terrarier.netlistening.Server;
import de.terrarier.netlistening.impl.ClientImpl;
import de.terrarier.netlistening.internals.InternalUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.junit.Test;

import static java.lang.Byte.MAX_VALUE;
import static java.lang.Byte.MIN_VALUE;

public class VoidPromise {

    private static final int WARMUP_ITERATIONS = 2500;
    private static final int ITERATIONS = 25000;

    @Test
    public void benchmarkVoidPromise() {
        Server server = Server.builder(8839).build();
        try {
            Thread.sleep(50L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ClientImpl client = (ClientImpl) Client.builder("localhost", 8839).build();
        try {
            Thread.sleep(100L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int counter = MIN_VALUE;
        Channel channel = client.getConnection().getChannel();
        /*
        ByteBuf buffer = Unpooled.buffer(client.getCompressionSetting().isVarIntCompression() ? 2 : 5);
            InternalUtil.writeIntUnchecked(client, buffer, 0x1);
            buffer.markWriterIndex();
         */
        for(int i = 0; i < WARMUP_ITERATIONS; i++) {
            ByteBuf buffer = Unpooled.buffer(client.getCompressionSetting().isVarIntCompression() ? 2 : 5);
            InternalUtil.writeIntUnchecked(client, buffer, 0x1);
            buffer.markWriterIndex();
            if (counter == MAX_VALUE) {
                counter = MIN_VALUE;
            }

            buffer.resetWriterIndex();
            buffer.writeByte(++counter);
            buffer.retain();
            channel.writeAndFlush(buffer);
            System.currentTimeMillis();
        }
        for(int i = 0; i < WARMUP_ITERATIONS; i++) {
            ByteBuf buffer = Unpooled.buffer(client.getCompressionSetting().isVarIntCompression() ? 2 : 5);
            InternalUtil.writeIntUnchecked(client, buffer, 0x1);
            buffer.markWriterIndex();
            if (counter == MAX_VALUE) {
                counter = MIN_VALUE;
            }

            buffer.resetWriterIndex();
            buffer.writeByte(++counter);
            buffer.retain();
            channel.writeAndFlush(buffer, channel.voidPromise());
            System.currentTimeMillis();
        }
        long defStart = System.currentTimeMillis();
        for(int i = 0; i < ITERATIONS; i++) {
            ByteBuf buffer = Unpooled.buffer(client.getCompressionSetting().isVarIntCompression() ? 2 : 5);
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
        long defEnd = System.currentTimeMillis();
        long voidStart = System.currentTimeMillis();
        for(int i = 0; i < ITERATIONS; i++) {
            ByteBuf buffer = Unpooled.buffer(client.getCompressionSetting().isVarIntCompression() ? 2 : 5);
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
        long voidEnd = System.currentTimeMillis();
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Def time: " + (defEnd - defStart));
        System.out.println("Void time: " + (voidEnd - voidStart));
    }

}
