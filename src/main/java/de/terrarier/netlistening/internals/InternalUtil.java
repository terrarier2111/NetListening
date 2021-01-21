package de.terrarier.netlistening.internals;

import de.terrarier.netlistening.Application;
import de.terrarier.netlistening.utils.ByteBufUtilExtension;
import de.terrarier.netlistening.utils.VarIntUtil;
import de.terrarier.netlistening.utils.VarIntUtil.VarIntParseException;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class InternalUtil {
	
	private InternalUtil() {}

	public static void writeInt(@NotNull Application application, @NotNull ByteBuf buffer, int value) {
		ByteBufUtilExtension.correctSize(buffer, getSize(application, value), application.getBuffer());
		if (!application.isVarIntCompressionEnabled()) {
			buffer.writeInt(value);
			return;
		}
		VarIntUtil.putVarInt(value, buffer);
	}
	
	public static int readInt(@NotNull Application application, @NotNull ByteBuf buffer) throws VarIntParseException {
		if(application.isVarIntCompressionEnabled()) {
			return VarIntUtil.getVarInt(buffer);
		}
		if(buffer.readableBytes() < 4) {
			throw VarIntUtil.FOUR_BYTES_PARSE_EXCEPTION;
		}
		return buffer.readInt();
	}
	
	public static int getSize(@NotNull Application application, int value) {
		return application.isVarIntCompressionEnabled() ? VarIntUtil.varIntSize(value) : 4;
	}

}
