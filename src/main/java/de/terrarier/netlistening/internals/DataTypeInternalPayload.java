package de.terrarier.netlistening.internals;

import de.terrarier.netlistening.api.type.DataType;
import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
@ApiStatus.Internal
public final class DataTypeInternalPayload extends DataType<InternalPayload> {

	public DataTypeInternalPayload() {
		super((byte) 0x0, (byte) 1, false);
	}
	
	@Override
	public InternalPayload read(@NotNull ApplicationImpl application, @NotNull ConnectionImpl connection, @NotNull ByteBuf buffer)
			throws CancelReadSignal {
		final byte payloadId = buffer.readByte();
		InternalPayload.fromId(payloadId).read(application, connection, buffer);
		return null;
	}

	@Override
	public void write0(@NotNull ApplicationImpl application, @NotNull ByteBuf buffer, @NotNull InternalPayload data) {
		InternalUtil.writeInt(application, buffer, 0x0); // We use this sneaky hack which allows us to ignore the fact that we
															   // have to send the packet id of the packet containing the payload
															   // (0x0) when using InternalPayloads.
		write(application, buffer, data);
	}

	@Override
	public void write(@NotNull ApplicationImpl application, @NotNull ByteBuf buffer, @NotNull InternalPayload data) {
		data.write0(application, buffer);
	}
	
}
