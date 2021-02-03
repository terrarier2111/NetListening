package de.terrarier.netlistening.api.type;

import de.terrarier.netlistening.Application;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class DataTypeLong extends DataType<Long> {
	
	protected DataTypeLong() {
		super((byte) 0x9, 8, true);
	}

	@Override
	protected Long read(@NotNull Application application, @NotNull Channel channel, @NotNull ByteBuf buffer) {
		return buffer.readLong();
	}

	@Override
	public void write(@NotNull Application application, @NotNull ByteBuf buffer, @NotNull Long data) {
		buffer.writeLong(data);
	}

}
