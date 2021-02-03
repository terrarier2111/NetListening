package de.terrarier.netlistening.api.type;

import de.terrarier.netlistening.Application;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class DataTypeInt extends DataType<Integer> {
	
	protected DataTypeInt() {
		super((byte) 0x5, 4, true);
	}

	@Override
	protected Integer read(@NotNull Application application, @NotNull Channel channel, @NotNull ByteBuf buffer) {
		return buffer.readInt();
	}

	@Override
	public void write(@NotNull Application application, @NotNull ByteBuf buffer, @NotNull Integer data) {
		buffer.writeInt(data);
	}

}
