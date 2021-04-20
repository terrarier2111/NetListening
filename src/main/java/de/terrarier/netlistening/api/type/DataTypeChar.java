package de.terrarier.netlistening.api.type;

import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class DataTypeChar extends DataType<Character> {
	
	DataTypeChar() {
		super((byte) 0x4, (byte) 2, true);
	}

	@Override
	protected Character read(@NotNull ApplicationImpl application, @NotNull ConnectionImpl connection,
							 @NotNull ByteBuf buffer) {
		return buffer.readChar();
	}

	@Override
	protected void write(@NotNull ApplicationImpl application, @NotNull ByteBuf buffer, @NotNull Character data) {
		buffer.writeChar(data);
	}

}
