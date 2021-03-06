package de.terrarier.netlistening.api.type;

import de.terrarier.netlistening.impl.ApplicationImpl;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class DataTypeUUID extends DataType<UUID> {
	
	protected DataTypeUUID() {
		super((byte) 0xB, (byte) 16, true);
	}

	@Override
	protected UUID read(@NotNull ApplicationImpl application, @NotNull Channel channel, @NotNull ByteBuf buffer) {
		final long mostSignificantBits = buffer.readLong();
		final long leastSignificantBits = buffer.readLong();
		return new UUID(mostSignificantBits, leastSignificantBits);
	}

	@Override
	protected void write(@NotNull ApplicationImpl application, @NotNull ByteBuf buffer, @NotNull UUID uuid) {
		final long mostSignificantBits = uuid.getMostSignificantBits();
		final long leastSignificantBits = uuid.getLeastSignificantBits();
		buffer.writeLong(mostSignificantBits);
		buffer.writeLong(leastSignificantBits);
	}
	
}
