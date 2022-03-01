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
import io.netty.util.internal.PlatformDependent;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public final class UDSTest {

    @Test
    public void test() {
        final boolean[] serverRecv = {false};
        final boolean[] clientRecv = {false};
        final String ioFilePath = PlatformDependent.tmpdir().getAbsolutePath() + "/nl_io";
        try {
            new File(ioFilePath).createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        final Server server = Server.builder(ioFilePath).build();
        server.registerListener(new DecodeListener() {
            @Override
            public void trigger(DecodeEvent value) {
                final String message = value.getData().read();
                System.out.println(message);
                serverRecv[0] = true;
            }
        });
        final Client client = Client.builder(ioFilePath).build();
        client.registerListener(new DecodeListener() {
            @Override
            public void trigger(DecodeEvent value) {
                final String message = value.getData().read();
                System.out.println(message);
                clientRecv[0] = true;
            }
        });
        client.sendData("test");
        try {
            Thread.sleep(2500L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        server.sendData("test2");
        try {
            Thread.sleep(500L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        new File(ioFilePath).delete();
        if (!serverRecv[0] || !clientRecv[0]) {
            throw new Error();
        }
    }

}
