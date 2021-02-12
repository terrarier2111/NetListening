package de.terrarier.netlistening.internals;

import de.terrarier.netlistening.Application;
import de.terrarier.netlistening.api.type.DataType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class DataTypeInternalPayload extends DataType<InternalPayload> {

	public DataTypeInternalPayload() {
		super((byte) 0x0, (byte) 1, false);
	}
	
	@Override
	public InternalPayload read(@NotNull Application application, @NotNull Channel channel, @NotNull ByteBuf buffer)
			throws CancelReadingSignal {
		final byte payloadId = buffer.readByte();
		try {
			InternalPayload.fromId(payloadId).read(application, channel, buffer);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}catch (CancelReadingSignal signal) {
			buffer.readerIndex(buffer.readerIndex() - 1);
			throw signal;
		}
		return null;
	}

	@Override
	public void write0(@NotNull Application application, @NotNull ByteBuf buffer, @NotNull InternalPayload data) {
		InternalUtil.writeInt(application, buffer, 0x0); // We use this sneaky hack which allows us to ignore the fact that we
															   // have to send the packet id of the packet containing the payload
															   // (0x0) when using InternalPayloads.
		write(application, buffer, data);
	}

	@Override
	public void write(@NotNull Application application, @NotNull ByteBuf buffer, @NotNull InternalPayload data) {
		data.write0(application, buffer);
	}
	
}
