package de.terrarier.netlistening.api.type;

import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.internals.AssumeNotNull;
import io.netty.buffer.ByteBuf;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class DataTypeBoolean extends DataType<Boolean> {
	
	DataTypeBoolean() {
		super((byte) 0x1, (byte) 1, true);
	}

	@Override
	protected Boolean read(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ConnectionImpl connection,
						   @AssumeNotNull ByteBuf buffer) {
		return buffer.readBoolean();
	}

	@Override
	protected void write(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ByteBuf buffer, @AssumeNotNull Boolean data) {
		buffer.writeBoolean(data);
	}

}
