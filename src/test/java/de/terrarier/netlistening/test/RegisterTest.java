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
                .build().encryption().disableHmac().build().build();
        final Client client = Client.builder("localhost", 55843).build();
        try {
            // Note: It is logical that the client will throw an exception, just keep that in mind!
            // This is because it doesn't know about the packet it allegedly sent, but which we faked
            // in reality.
            final ByteBuf buffer = Unpooled.buffer();
            buffer.writeInt(0x0);
            buffer.writeByte(0x1);
            buffer.writeInt(0x6); // packet id - starting at 0x5 so we are using an higher id to start
            buffer.writeShort(0x1); // 1 data type
            buffer.writeByte(DataType.BYTE_ARRAY.getId() - 1);
            final Field field = ClientImpl.class.getDeclaredField("channel");
            field.setAccessible(true);
            Thread.sleep(1000L);
            final Channel channel = (Channel) field.get(client);
            channel.writeAndFlush(buffer);
            // client.sendData("hey!");
            final ByteBuf fakePacket = Unpooled.buffer();
            fakePacket.writeInt(0x6); // Registered packet
            fakePacket.writeInt(0x1); // Byte array length
            fakePacket.writeByte(0xF); // Just a random number (Byte array content)
            channel.writeAndFlush(fakePacket);
            Thread.sleep(2000L);
        } catch (NoSuchFieldException | IllegalAccessException | InterruptedException e) {
            e.printStackTrace();
        }
        server.stop();
        client.stop();
    }

}
