package de.terrarier.netlistening.api.type;

import de.terrarier.netlistening.Application;
import de.terrarier.netlistening.internals.CancelReadingSignal;
import de.terrarier.netlistening.utils.ByteBufUtilExtension;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class DataTypeString extends DataType<String> {

	private static final String EMPTY_STRING = "";

	protected DataTypeString() {
		super((byte) 0x7, (byte) 4, true);
	}
	
	@Override
	protected String read(@NotNull Application application, @NotNull Channel channel, @NotNull ByteBuf buffer)
			throws CancelReadingSignal {
		final int length = buffer.readInt();
		
		if(length < 1) { // TODO: Throw exception when length < 0
			return EMPTY_STRING;
		}
		
		checkReadable(buffer, length);
		
		final byte[] bytes = ByteBufUtilExtension.readBytes(buffer, length);
		return new String(bytes, application.getStringEncoding());
	}

	@Override
	protected void write(@NotNull Application application, @NotNull ByteBuf buffer, @NotNull String data) {
		ByteBufUtilExtension.writeBytes(buffer, data.getBytes(application.getStringEncoding()), application.getBuffer());
	}

}
