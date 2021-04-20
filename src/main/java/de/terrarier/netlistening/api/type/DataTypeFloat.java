package de.terrarier.netlistening.api.type;

import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class DataTypeFloat extends DataType<Float> {
	
	DataTypeFloat() {
		super((byte) 0xC, (byte) 4, true);
	}

	@Override
	protected Float read(@NotNull ApplicationImpl application, @NotNull ConnectionImpl connection,
						 @NotNull ByteBuf buffer) {
		return buffer.readFloat();
	}

	@Override
	protected void write(@NotNull ApplicationImpl application, @NotNull ByteBuf buffer, @NotNull Float data) {
		buffer.writeFloat(data);
	}

}
