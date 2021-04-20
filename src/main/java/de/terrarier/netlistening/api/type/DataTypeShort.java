package de.terrarier.netlistening.api.type;

import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class DataTypeShort extends DataType<Short> {
	
	DataTypeShort() {
		super((byte) 0x6, (byte) 2, true);
	}

	@Override
	protected Short read(@NotNull ApplicationImpl application, @NotNull ConnectionImpl connection,
						 @NotNull ByteBuf buffer) {
		return buffer.readShort();
	}

	@Override
	protected void write(@NotNull ApplicationImpl application, @NotNull ByteBuf buffer, @NotNull Short data) {
		buffer.writeShort(data);
	}

}
