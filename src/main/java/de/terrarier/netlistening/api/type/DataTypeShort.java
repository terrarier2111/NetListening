package de.terrarier.netlistening.api.type;

import de.terrarier.netlistening.Application;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class DataTypeShort extends DataType<Short> {
	
	protected DataTypeShort() {
		super((byte) 0x6, (byte) 2, true);
	}

	@Override
	protected Short read(@NotNull Application application, @NotNull Channel channel, @NotNull ByteBuf buffer) {
		return buffer.readShort();
	}

	@Override
	public void write(@NotNull Application application, @NotNull ByteBuf buffer, @NotNull Short data) {
		buffer.writeShort(data);
	}

}
