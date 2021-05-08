package de.terrarier.netlistening.api.type;

import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.internals.AssumeNotNull;
import io.netty.buffer.ByteBuf;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class DataTypeFloat extends DataType<Float> {
	
	DataTypeFloat() {
		super((byte) 0xC, (byte) 4, true);
	}

	@Override
	protected Float read(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ConnectionImpl connection,
						 @AssumeNotNull ByteBuf buffer) {
		return buffer.readFloat();
	}

	@Override
	protected void write(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ByteBuf buffer,
						 @AssumeNotNull Float data) {
		buffer.writeFloat(data);
	}

}
