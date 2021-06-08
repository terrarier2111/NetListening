package de.terrarier.netlistening.network;

import de.terrarier.netlistening.api.type.DataType;
import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.internals.AssumeNotNull;
import de.terrarier.netlistening.internals.InternalPayloadUpdateTranslationEntry;
import de.terrarier.netlistening.internals.InternalUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.ApiStatus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Terrarier2111
 * @since 1.11
 */
@ApiStatus.Internal
public final class PacketIdTranslationCache {

    private final Map<Integer, Integer> translations = new ConcurrentHashMap<>();
    private final ConnectionImpl connection;
    private final ApplicationImpl application;

    public PacketIdTranslationCache(@AssumeNotNull ConnectionImpl connection,
                                    @AssumeNotNull ApplicationImpl application) {
        this.connection = connection;
        this.application = application;
    }

    public void insert(int foreign, int local) {
        translations.put(foreign, local);
        final ByteBuf buffer = Unpooled.buffer(InternalUtil.getSingleByteSize(application) + 1 + 4 + 4); // TODO: Improve init size.
        DataType.getDTIP().write0(application, buffer, new InternalPayloadUpdateTranslationEntry(foreign, local));
        connection.getChannel().writeAndFlush(buffer);
    }

    public void delete(int foreign) {
        translations.remove(foreign);
    }

    public int tryTranslate(int id) {
        return translations.getOrDefault(id, id);
    }

}
