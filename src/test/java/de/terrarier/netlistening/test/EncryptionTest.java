package de.terrarier.netlistening.test;

import de.terrarier.netlistening.Client;
import de.terrarier.netlistening.Server;
import de.terrarier.netlistening.api.DataContainer;
import de.terrarier.netlistening.api.PacketCaching;
import de.terrarier.netlistening.api.encryption.EncryptionSetting;
import de.terrarier.netlistening.api.event.DecodeEvent;
import de.terrarier.netlistening.api.event.DecodeListener;
import org.junit.Test;

public final class EncryptionTest {

    @Test(timeout = 5000L)
    public void testTimeOut() {
        final Server server = new Server.Builder(55843).compression().varIntCompression(true)
                .nibbleCompression(true).build().caching(PacketCaching.GLOBAL).timeout(15000)
                .encryption(new EncryptionSetting()).build();
        final Client client = Client.builder("localhost", 55843).timeout(30000L).build();
        final boolean[] receivedServer = {false};
        final boolean[] receivedClient = {false};
        server.registerListener(new DecodeListener() {
            @Override
            public void trigger(DecodeEvent value) {
                receivedServer[0] = true;
            }
        });
        client.registerListener(new DecodeListener() {
            @Override
            public void trigger(DecodeEvent value) {
                receivedClient[0] = true;
            }
        });
        DataContainer serverData = new DataContainer();
        serverData.add("Test");
        serverData.setEncrypted(true);
        DataContainer clientData = new DataContainer();
        clientData.addAll("Test2", 8);
        clientData.setEncrypted(true);
        client.sendData(clientData);
        try {
            Thread.sleep(500L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        server.sendData(serverData);
        try {
            Thread.sleep(500L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        client.sendData(serverData);
        try {
            Thread.sleep(500L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        server.sendData(clientData);
        try {
            Thread.sleep(500L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (!receivedServer[0] || !receivedClient[0]) {
            throw new Error("Connection timed out!");
        }
    }

}
