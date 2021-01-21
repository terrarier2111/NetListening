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
public final class DataTypeChar extends DataType<Character> {
	
	protected DataTypeChar() {
		super((byte) 0x4, 2, true);
	}

	@Override
	protected Character read(@NotNull Application application, @NotNull Channel channel, @NotNull ByteBuf buffer)
			throws CancelReadingSignal {
		return buffer.readChar();
	}

	@Override
	public void write(@NotNull Application application, @NotNull ByteBuf buffer, @NotNull Character data) {
		buffer.writeChar(data);
	}

}
