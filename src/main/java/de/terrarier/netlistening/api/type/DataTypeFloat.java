package de.terrarier.netlistening.api.type;

import de.terrarier.netlistening.Application;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class DataTypeFloat extends DataType<Float> {
	
	protected DataTypeFloat() {
		super((byte) 0xC, (byte) 4, true);
	}

	@Override
	protected Float read(@NotNull Application application, @NotNull Channel channel, @NotNull ByteBuf buffer) {
		return buffer.readFloat();
	}

	@Override
	public void write(@NotNull Application application, @NotNull ByteBuf buffer, @NotNull Float data) {
		buffer.writeFloat(data);
	}

}
