package de.terrarier.netlistening.api.type;

import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.internals.CancelReadSignal;
import de.terrarier.netlistening.utils.ByteBufUtilExtension;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class DataTypeByteArray extends DataType<byte[]> {
	
	DataTypeByteArray() {
		super((byte) 0x3, (byte) 4, true);
	}

	@Override
	protected byte[] read(@NotNull ApplicationImpl application, @NotNull ConnectionImpl connection,
						  @NotNull ByteBuf buffer) throws CancelReadSignal {
		final int length = buffer.readInt();

		checkReadable(buffer, length);
		
		return ByteBufUtilExtension.readBytes(buffer, length);
	}

	@Override
	protected void write(@NotNull ApplicationImpl application, @NotNull ByteBuf buffer, byte[] data) {
		ByteBufUtilExtension.writeBytes(buffer, data, application.getBuffer());
	}

}
