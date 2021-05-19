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
import de.terrarier.netlistening.api.serialization.SerializationProvider;
import de.terrarier.netlistening.internals.AssumeNotNull;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

public final class SerializationTest {

    // TODO: Improve tests somehow!

    @Test(timeout = 15000L)
    public void testServerDeserialization() {
        final Server server = Server.builder(55843).compression().varIntCompression(false).nibbleCompression(true).build().build();
        final Client client = Client.builder("localhost", 55843).serialization(new SerializationProvider() {
            @Override
            protected boolean isSerializable(Object obj) {
                return true;
            }

            @Override
            protected boolean isDeserializable(@AssumeNotNull byte[] data) {
                return true;
            }

            @Override
            protected byte[] serialize(Object obj) {
                return obj.toString().getBytes(StandardCharsets.UTF_8);
            }

            @Override
            protected Object deserialize(@AssumeNotNull byte[] data) {
                return new String(data, StandardCharsets.UTF_8);
            }

        }).build();
        server.registerListener(new DecodeListener() {
            @Override
            public void trigger(DecodeEvent value) {
                while(value.getData().isReadable()) {
                    final Object data = value.getData().read();
                    System.out.println(data);
                }
            }
        });
        client.sendData("test");
        client.sendData("test0", new Object(), "test1", 46);
        client.sendData("test123");
        try {
            Thread.sleep(10000L);
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
                while(value.getData().isReadable()) {
                    final Object data = value.getData().read();
                    System.out.println(data);
                }
            }
        });
        client.sendData("test");
        client.sendData("test00", new Object(), "test11", 46);
        client.sendData("test123");
        try {
            Thread.sleep(10000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        client.stop();
        server.stop();
    }

}
