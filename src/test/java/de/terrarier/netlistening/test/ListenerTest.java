package de.terrarier.netlistening.test;

import de.terrarier.netlistening.Server;
import de.terrarier.netlistening.api.event.DecodeEvent;
import de.terrarier.netlistening.api.event.DecodeListener;
import org.junit.Test;

public final class ListenerTest {

    @Test
    public void testConnect() {
        final Server server = Server.builder(54732).build();
        final long listenerId = server.registerListener(new DecodeListener() {
            @Override
            public void trigger(DecodeEvent value) {}
        });
        server.unregisterListener(listenerId);
        server.stop();
    }

}
