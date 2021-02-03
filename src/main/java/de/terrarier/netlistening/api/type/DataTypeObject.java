package de.terrarier.netlistening.api.type;

import de.terrarier.netlistening.Application;
import de.terrarier.netlistening.api.serialization.SerializationUtil;
import de.terrarier.netlistening.internals.CancelReadingSignal;
import de.terrarier.netlistening.utils.ByteBufUtilExtension;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class DataTypeObject extends DataType<Object> {
	
	protected DataTypeObject() {
		super((byte) 0x8, 4, true);
	}

	@Override
	protected Object read(@NotNull Application application, @NotNull Channel channel, @NotNull ByteBuf buffer)
			throws CancelReadingSignal {
		final int length = buffer.readInt();

		checkReadable(buffer, length, true);

		final byte[] bytes = ByteBufUtilExtension.readBytes(buffer, length);
		return SerializationUtil.deserialize(application, bytes);
	}

	@Override
	public void write(@NotNull Application application, @NotNull ByteBuf buffer, @NotNull Object data) {
		ByteBufUtilExtension.writeBytes(buffer, SerializationUtil.serialize(application, data), application.getBuffer());
	}

}
