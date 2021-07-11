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
import de.terrarier.netlistening.api.event.DecodeEvent;
import de.terrarier.netlistening.api.event.DecodeListener;
import de.terrarier.netlistening.api.serialization.RegisterSerializationProvider;
import io.netty.buffer.ByteBuf;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

public final class SerializationTest {

    // TODO: Improve tests somehow!

    @Test(timeout = 15000L)
    public void testServerDeserialization() {
        final RegisterSerializationProvider serverSerializationProvider = new RegisterSerializationProvider();
        serverSerializationProvider.registerTransformer(new RegisterSerializationProvider.ByteBufTransformer<Object>() {
            @Override
            protected Object fromBytes(ByteBuf data, int length) {
                return new String(readBytes(data, length));
            }

            @Override
            protected void toBytes(ByteBuf buffer, Object input) {
                writeBytes(buffer, input.toString().getBytes(StandardCharsets.UTF_8));
            }

        });
        final Server server = Server.builder(55843).
                serialization(serverSerializationProvider).compression().varIntCompression(false).nibbleCompression(true).build().build();
        final RegisterSerializationProvider clientSerializationProvider = new RegisterSerializationProvider();
        clientSerializationProvider.registerTransformer(new RegisterSerializationProvider.ByteBufTransformer<Object>() {

            @Override
            protected Object fromBytes(ByteBuf data, int length) {
                return new String(readBytes(data, length));
            }

            @Override
            protected void toBytes(ByteBuf buffer, Object input) {
                writeBytes(buffer, input.toString().getBytes(StandardCharsets.UTF_8));
            }

        });
        final Client client = Client.builder("localhost", 55843).
                serialization(clientSerializationProvider).build();
        server.registerListener(new DecodeListener() {
            @Override
            public void trigger(DecodeEvent value) {
                while (value.getData().isReadable()) {
                    final Object data = value.getData().read();
                    System.out.println(data);
                }
            }
        });
        client.sendData("test");
        client.sendData("test0", new Object(), "test1", 46);
        client.sendData("test123");
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        server.stop();
        client.stop();
    }

    @Test(timeout = 15000L)
    public void testClientSerialization() {
        final Server server = Server.builder(55843).compression().varIntCompression(false).nibbleCompression(true).build().build();
        final Client client = Client.builder("localhost", 55843).build();
        server.registerListener(new DecodeListener() {
            @Override
            public void trigger(DecodeEvent value) {
                while (value.getData().isReadable()) {
                    final Object data = value.getData().read();
                    System.out.println(data);
                }
            }
        });
        client.sendData("test");
        // client.sendData("-3test00", new Object(), "test11", 46); // - this should lead to an error!
        client.sendData("test123");
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        client.stop();
        server.stop();
    }

}
