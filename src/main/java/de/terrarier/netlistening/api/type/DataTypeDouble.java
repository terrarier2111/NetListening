package de.terrarier.netlistening.api.type;

import de.terrarier.netlistening.Application;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class DataTypeDouble extends DataType<Double> {
	
	protected DataTypeDouble() {
		super((byte) 0xA, (byte) 8, true);
	}

	@Override
	protected Double read(@NotNull Application application, @NotNull Channel channel, @NotNull ByteBuf buffer) {
		return buffer.readDouble();
	}

	@Override
	public void write(@NotNull Application application, @NotNull ByteBuf buffer, @NotNull Double data) {
		buffer.writeDouble(data);
	}

}
