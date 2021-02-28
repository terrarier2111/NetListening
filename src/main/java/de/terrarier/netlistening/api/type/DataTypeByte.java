package de.terrarier.netlistening.api.type;

import de.terrarier.netlistening.Application;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class DataTypeByte extends DataType<Byte> {
	
	protected DataTypeByte() {
		super((byte) 0x2, (byte) 1, true);
	}

	@Override
	protected Byte read(@NotNull Application application, @NotNull Channel channel, @NotNull ByteBuf buffer) {
		return buffer.readByte();
	}

	@Override
	protected void write(@NotNull Application application, @NotNull ByteBuf buffer, @NotNull Byte data) {
		buffer.writeByte(data);
	}

}
