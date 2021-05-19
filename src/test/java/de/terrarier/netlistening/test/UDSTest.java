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
            }
        });
        final Client client = Client.builder(ioFilePath).build();
        client.registerListener(new DecodeListener() {
            @Override
            public void trigger(DecodeEvent value) {
                final String message = value.getData().read();
                System.out.println(message);
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
    }

}
