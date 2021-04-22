package de.terrarier.netlistening.api.type;

import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.internals.AssumeNotNull;
import de.terrarier.netlistening.internals.CancelReadSignal;
import de.terrarier.netlistening.utils.ByteBufUtilExtension;
import io.netty.buffer.ByteBuf;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class DataTypeString extends DataType<String> {

	private static final String EMPTY_STRING = "";

	DataTypeString() {
		super((byte) 0x7, (byte) 4, true);
	}
	
	@Override
	protected String read(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ConnectionImpl connection,
						  @AssumeNotNull ByteBuf buffer) throws CancelReadSignal {
		final int length = buffer.readInt();

		if(length < 1) {
			if (length == 0) {
				return EMPTY_STRING;
			}
			throw new IllegalStateException("Received a malicious string of length " + length + '.');
		}
		
		checkReadable(buffer, length);
		
		final byte[] bytes = ByteBufUtilExtension.readBytes(buffer, length);
		return new String(bytes, application.getStringEncoding());
	}

	@Override
	protected void write(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ByteBuf buffer, @AssumeNotNull String data) {
		ByteBufUtilExtension.writeBytes(buffer, data.getBytes(application.getStringEncoding()), application.getBuffer());
	}

}
