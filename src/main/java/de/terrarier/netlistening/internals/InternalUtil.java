package de.terrarier.netlistening.internals;

import de.terrarier.netlistening.api.compression.VarIntUtil;
import de.terrarier.netlistening.api.compression.VarIntUtil.VarIntParseException;
import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.utils.ByteBufUtilExtension;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
@ApiStatus.Internal
public final class InternalUtil {
	
	private InternalUtil() {
		throw new UnsupportedOperationException("This class may not be instantiated!");
	}

	public static void writeInt(@NotNull ApplicationImpl application, @NotNull ByteBuf buffer, int value) {
		ByteBufUtilExtension.correctSize(buffer, getSize(application, value), application.getBuffer());
		writeIntUnchecked(application, buffer, value);
	}

	public static void writeIntUnchecked(@NotNull ApplicationImpl application, @NotNull ByteBuf buffer, int value) {
		if (!application.getCompressionSetting().isVarIntCompression()) {
			buffer.writeInt(value);
			return;
		}
		VarIntUtil.writeVarInt(value, buffer);
	}
	
	public static int readInt(@NotNull ApplicationImpl application, @NotNull ByteBuf buffer) throws VarIntParseException {
		if(application.getCompressionSetting().isVarIntCompression()) {
			return VarIntUtil.getVarInt(buffer);
		}
		if(buffer.readableBytes() < 4) {
			throw VarIntParseException.FOUR_BYTES;
		}
		return buffer.readInt();
	}
	
	public static int getSize(@NotNull ApplicationImpl application, int value) {
		return application.getCompressionSetting().isVarIntCompression() ? VarIntUtil.varIntSize(value) : 4;
	}

}
