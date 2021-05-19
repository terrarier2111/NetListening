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
import de.terrarier.netlistening.api.event.ConnectionTimeoutEvent;
import de.terrarier.netlistening.api.event.ConnectionTimeoutListener;
import org.junit.Test;

public final class KeepAliveTest {

    @Test(timeout = 30000L)
    public void testTimeOut() {
        final boolean[] error = {false};
        final Server server = Server.builder(55843).timeout(2000L).build();
        final Client client = Client.builder("localhost", 55843).timeout(2000L).build();
        server.registerListener(new ConnectionTimeoutListener() {
            @Override
            public void trigger(ConnectionTimeoutEvent value) {
                error[0] = true;
            }
        });
        client.registerListener(new ConnectionTimeoutListener() {
            @Override
            public void trigger(ConnectionTimeoutEvent value) {
                error[0] = true;
            }
        });
        try {
            Thread.sleep(25000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(error[0]) {
            throw new Error("Connection timed out!");
        }
    }

}
