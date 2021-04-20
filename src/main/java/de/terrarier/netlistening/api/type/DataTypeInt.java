package de.terrarier.netlistening.api.type;

import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class DataTypeInt extends DataType<Integer> {
	
	DataTypeInt() {
		super((byte) 0x5, (byte) 4, true);
	}

	@Override
	protected Integer read(@NotNull ApplicationImpl application, @NotNull ConnectionImpl connection,
						   @NotNull ByteBuf buffer) {
		return buffer.readInt();
	}

	@Override
	protected void write(@NotNull ApplicationImpl application, @NotNull ByteBuf buffer, @NotNull Integer data) {
		buffer.writeInt(data);
	}

}
