package de.terrarier.netlistening.api.type;

import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.internals.AssumeNotNull;
import io.netty.buffer.ByteBuf;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class DataTypeShort extends DataType<Short> {
	
	DataTypeShort() {
		super((byte) 0x6, (byte) 2, true);
	}

	@Override
	protected Short read(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ConnectionImpl connection,
						 @AssumeNotNull ByteBuf buffer) {
		return buffer.readShort();
	}

	@Override
	protected void write(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ByteBuf buffer, @AssumeNotNull Short data) {
		buffer.writeShort(data);
	}

}
