package de.terrarier.netlistening.internals;

import de.terrarier.netlistening.Application;
import de.terrarier.netlistening.api.compression.NibbleUtil;
import de.terrarier.netlistening.api.compression.VarIntUtil;
import de.terrarier.netlistening.api.compression.VarIntUtil.VarIntParseException;
import de.terrarier.netlistening.utils.ByteBufUtilExtension;
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
		if (!application.getCompressionSetting().isVarIntCompression()) {
			buffer.writeInt(value);
			return;
		}
		VarIntUtil.writeVarInt(value, buffer);
	}
	
	public static int readInt(@NotNull Application application, @NotNull ByteBuf buffer) throws VarIntParseException {
		if(application.getCompressionSetting().isVarIntCompression()) {
			return VarIntUtil.getVarInt(buffer);
		}
		if(buffer.readableBytes() < 4) {
			throw VarIntParseException.FOUR_BYTES;
		}
		return buffer.readInt();
	}
	
	public static int getSize(@NotNull Application application, int value) {
		return application.getCompressionSetting().isVarIntCompression() ? VarIntUtil.varIntSize(value) : 4;
	}

}
