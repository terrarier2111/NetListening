package de.terrarier.netlistening.network;

import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.internals.InternalPayloadUpdateTranslationEntry;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.ApiStatus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @since 1.07
 * @author Terrarier2111
 */
@ApiStatus.Internal
public final class PacketIdTranslationCache {

    private final Map<Integer, Integer> translations = new ConcurrentHashMap<>();
    private final ConnectionImpl connection;
    private final ApplicationImpl application;

    public PacketIdTranslationCache(ConnectionImpl connection, ApplicationImpl application) {
        this.connection = connection;
        this.application = application;
    }

    public void insert(int foreign, int local) {
        translations.put(foreign, local);
        final ByteBuf buffer = Unpooled.buffer(4 + 1 + 4 + 4); // TODO: Improve init size.
        new InternalPayloadUpdateTranslationEntry(foreign, local).write(application, buffer);
        connection.getChannel().writeAndFlush(buffer);
    }

    public void delete(int foreign) {
        translations.remove(foreign);
    }

    public int tryTranslate(int id) {
        final Integer translation = translations.get(id);
        return translation == null ? id : translation;
    }

}
