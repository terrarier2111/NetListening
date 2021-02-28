package de.terrarier.netlistening.api.type;

import de.terrarier.netlistening.Application;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class DataTypeBoolean extends DataType<Boolean> {
	
	protected DataTypeBoolean() {
		super((byte) 0x1, (byte) 1, true);
	}

	@Override
	protected Boolean read(@NotNull Application application, @NotNull Channel channel, @NotNull ByteBuf buffer) {
		return buffer.readBoolean();
	}

	@Override
	protected void write(@NotNull Application application, @NotNull ByteBuf buffer, @NotNull Boolean data) {
		buffer.writeBoolean(data);
	}

}
