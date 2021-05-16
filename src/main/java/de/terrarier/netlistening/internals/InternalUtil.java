package de.terrarier.netlistening.internals;

import de.terrarier.netlistening.api.compression.VarIntUtil;
import de.terrarier.netlistening.api.compression.VarIntUtil.VarIntParseException;
import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.utils.ByteBufUtilExtension;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.ApiStatus;

/**
 * @author Terrarier2111
 * @since 1.0
 */
@ApiStatus.Internal
public final class InternalUtil {

    private InternalUtil() {
        throw new UnsupportedOperationException("This class may not be instantiated!");
    }

    public static void writeInt(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ByteBuf buffer, int value) {
        ByteBufUtilExtension.correctSize(buffer, getSize(application, value), application.getBuffer());
        writeIntUnchecked(application, buffer, value);
    }

    public static void writeIntUnchecked(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ByteBuf buffer,
                                         int value) {
        if (!application.getCompressionSetting().isVarIntCompression()) {
            buffer.writeInt(value);
            return;
        }
        VarIntUtil.writeVarInt(value, buffer);
    }

    public static int readInt(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ByteBuf buffer)
            throws VarIntParseException {
        if (application.getCompressionSetting().isVarIntCompression()) {
            return VarIntUtil.getVarInt(buffer);
        }
        if (buffer.readableBytes() < 4) {
            throw VarIntParseException.FOUR_BYTES;
        }
        return buffer.readInt();
    }

    static int getSize(@AssumeNotNull ApplicationImpl application, int value) {
        return application.getCompressionSetting().isVarIntCompression() ? VarIntUtil.varIntSize(value) : 4;
    }

}
