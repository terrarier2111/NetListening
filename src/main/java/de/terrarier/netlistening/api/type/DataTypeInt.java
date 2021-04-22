package de.terrarier.netlistening.api.type;

import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.internals.AssumeNotNull;
import io.netty.buffer.ByteBuf;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class DataTypeInt extends DataType<Integer> {
	
	DataTypeInt() {
		super((byte) 0x5, (byte) 4, true);
	}

	@Override
	protected Integer read(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ConnectionImpl connection,
						   @AssumeNotNull ByteBuf buffer) {
		return buffer.readInt();
	}

	@Override
	protected void write(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ByteBuf buffer, @AssumeNotNull Integer data) {
		buffer.writeInt(data);
	}

}
