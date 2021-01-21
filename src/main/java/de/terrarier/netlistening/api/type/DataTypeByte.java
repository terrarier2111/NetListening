package de.terrarier.netlistening.api.type;

import de.terrarier.netlistening.Application;
import de.terrarier.netlistening.internals.CancelReadingSignal;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class DataTypeByte extends DataType<Byte> {
	
	protected DataTypeByte() {
		super((byte) 0x2, 1, true);
	}

	@Override
	protected Byte read(@NotNull Application application, @NotNull Channel channel, @NotNull ByteBuf buffer)
			throws CancelReadingSignal {
		return buffer.readByte();
	}

	@Override
	public void write(@NotNull Application application, @NotNull ByteBuf buffer, @NotNull Byte data) {
		buffer.writeByte(data);
	}

}
