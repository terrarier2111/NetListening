package de.terrarier.netlistening.api.type;

import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.internals.AssumeNotNull;
import io.netty.buffer.ByteBuf;

import java.util.UUID;

/**
 * @author Terrarier2111
 * @since 1.0
 */
public final class DataTypeUUID extends DataType<UUID> {

    DataTypeUUID() {
        super((byte) 0xB, (byte) 16, true);
    }

    @Override
    protected UUID read(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ConnectionImpl connection,
                        @AssumeNotNull ByteBuf buffer) {
        final long mostSignificantBits = buffer.readLong();
        final long leastSignificantBits = buffer.readLong();
        return new UUID(mostSignificantBits, leastSignificantBits);
    }

    @Override
    protected void write(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ByteBuf buffer,
                         @AssumeNotNull UUID uuid) {
        final long mostSignificantBits = uuid.getMostSignificantBits();
        final long leastSignificantBits = uuid.getLeastSignificantBits();
        buffer.writeLong(mostSignificantBits);
        buffer.writeLong(leastSignificantBits);
    }

}
