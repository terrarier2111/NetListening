package de.terrarier.netlistening.test;

import de.terrarier.netlistening.Client;
import de.terrarier.netlistening.Server;
import de.terrarier.netlistening.api.type.DataType;
import de.terrarier.netlistening.impl.ClientImpl;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.junit.Test;

import java.lang.reflect.Field;

public class RegisterTest {

    @Test(timeout = 15000L)
    public void test() {
        final Server server = Server.builder(55843).compression().varIntCompression(false).nibbleCompression(false)
                .build().build();
        final Client client = Client.builder("localhost", 55843).build();
        try {
            // TODO: Fix this!
            final ByteBuf buffer = Unpooled.buffer();
            buffer.writeInt(0x0);
            buffer.writeByte(0x1);
            buffer.writeInt(0x6); // packet id - starting at 0x5 so we are using an higher id to start
            buffer.writeShort(0x1); // 1 data type
            buffer.writeByte(DataType.BYTE_ARRAY.getId());
            final Field field = ClientImpl.class.getDeclaredField("channel");
            field.setAccessible(true);
            Thread.sleep(1000L);
            final Channel channel = (Channel) field.get(client);
            channel.writeAndFlush(buffer);
            // client.sendData("hey!");
            final ByteBuf fakePacket = Unpooled.buffer();
            fakePacket.writeInt(0x6);
            fakePacket.writeInt(1);
            fakePacket.writeByte(0xF); // Just a random number
            channel.writeAndFlush(fakePacket);
            Thread.sleep(9000L);
        } catch (NoSuchFieldException | IllegalAccessException | InterruptedException e) {
            e.printStackTrace();
        }
        server.stop();
        client.stop();
    }

}
