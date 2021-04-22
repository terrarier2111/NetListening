package de.terrarier.netlistening.api.type;

import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.internals.AssumeNotNull;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class DataTypeLong extends DataType<Long> {
	
	DataTypeLong() {
		super((byte) 0x9, (byte) 8, true);
	}

	@Override
	protected Long read(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ConnectionImpl connection,
						@AssumeNotNull ByteBuf buffer) {
		return buffer.readLong();
	}

	@Override
	protected void write(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ByteBuf buffer, @AssumeNotNull Long data) {
		buffer.writeLong(data);
	}

}
