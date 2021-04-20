package de.terrarier.netlistening.api.type;

import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class DataTypeBoolean extends DataType<Boolean> {
	
	DataTypeBoolean() {
		super((byte) 0x1, (byte) 1, true);
	}

	@Override
	protected Boolean read(@NotNull ApplicationImpl application, @NotNull ConnectionImpl connection,
						   @NotNull ByteBuf buffer) {
		return buffer.readBoolean();
	}

	@Override
	protected void write(@NotNull ApplicationImpl application, @NotNull ByteBuf buffer, @NotNull Boolean data) {
		buffer.writeBoolean(data);
	}

}
