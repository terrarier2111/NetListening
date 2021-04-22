package de.terrarier.netlistening.api.type;

import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.internals.AssumeNotNull;
import io.netty.buffer.ByteBuf;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class DataTypeByte extends DataType<Byte> {
	
	DataTypeByte() {
		super((byte) 0x2, (byte) 1, true);
	}

	@Override
	protected Byte read(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ConnectionImpl connection,
						@AssumeNotNull ByteBuf buffer) {
		return buffer.readByte();
	}

	@Override
	protected void write(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ByteBuf buffer, @AssumeNotNull Byte data) {
		buffer.writeByte(data);
	}

}
