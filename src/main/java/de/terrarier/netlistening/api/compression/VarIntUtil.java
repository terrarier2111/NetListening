package de.terrarier.netlistening.api.compression;

import de.terrarier.netlistening.internals.AssumeNotNull;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.ApiStatus;

/**
 * @author Terrarier2111
 * @since 1.0
 */
@ApiStatus.Internal
public final class VarIntUtil {

    // Original source: https://github.com/Netflix/hollow/blob/master/hollow/src/main/java/com/netflix/hollow/core/memory/encoding/VarInt.java

    private VarIntUtil() {
        throw new UnsupportedOperationException("This class may not be instantiated!");
    }

    public static int varIntSize(int value) {
        if (value < 0)
            return 5;
        if (value < 0x80)
            return 1;
        if (value < 0x4000)
            return 2;
        if (value < 0x200000)
            return 3;
        if (value < 0x10000000)
            return 4;
        return 5;
    }

	public static void writeVarInt(int value, @AssumeNotNull byte[] data) {
		int pos = 0;
		if (value > 0x0FFFFFFF || value < 0) data[pos++] = (byte) (0x80 | ((value >>> 28)));
		if (value > 0x1FFFFF || value < 0)   data[pos++] = (byte) (0x80 | ((value >>> 21) & 0x7F));
		if (value > 0x3FFF || value < 0)     data[pos++] = (byte) (0x80 | ((value >>> 14) & 0x7F));
		if (value > 0x7F || value < 0)       data[pos++] = (byte) (0x80 | ((value >>>  7) & 0x7F));

        data[pos] = (byte) (value & 0x7F);
    }

	@AssumeNotNull
	public static byte[] toVarInt(int value) {
		final byte[] bytes = new byte[varIntSize(value)];
		writeVarInt(value, bytes);
		return bytes;
	}

	public static void writeVarInt(int value, @AssumeNotNull ByteBuf out) {
		if (value > 0x0FFFFFFF || value < 0) out.writeByte((byte) (0x80 | ((value >>> 28))));
		if (value > 0x1FFFFF || value < 0)   out.writeByte((byte) (0x80 | ((value >>> 21) & 0x7F)));
		if (value > 0x3FFF || value < 0)     out.writeByte((byte) (0x80 | ((value >>> 14) & 0x7F)));
		if (value > 0x7F || value < 0)       out.writeByte((byte) (0x80 | ((value >>>  7) & 0x7F)));

        out.writeByte((byte) (value & 0x7F));
    }

    public static int getVarInt(@AssumeNotNull ByteBuf buffer) throws VarIntParseException {
        if (!buffer.isReadable()) {
            throw VarIntParseException.ONE_BYTE;
        }
        byte b = buffer.readByte();

        if (b == (byte) 0x80)
            throw new RuntimeException("Attempting to read null value as int");

        int value = b & 0x7F;
        byte required = 0;
        while ((b & 0x80) != 0) {
            required++;
            if (!buffer.isReadable()) {
                throw VarIntParseException.valueOf(required);
            }
            b = buffer.readByte();
            value <<= 7;
            value |= b & 0x7F;
        }

        return value;
    }

    @ApiStatus.Internal
    public static final class VarIntParseException extends Exception {

        public static final VarIntParseException ONE_BYTE = new VarIntParseException((byte) 1);
        public static final VarIntParseException TWO_BYTES = new VarIntParseException((byte) 2);
        public static final VarIntParseException THREE_BYTES = new VarIntParseException((byte) 3);
        public static final VarIntParseException FOUR_BYTES = new VarIntParseException((byte) 4);
        public static final VarIntParseException FIVE_BYTES = new VarIntParseException((byte) 5);
        public final byte requiredBytes;

        private VarIntParseException(byte requiredBytes) {
            this.requiredBytes = requiredBytes;
        }

        @AssumeNotNull
        @Override
        public Throwable initCause(Throwable cause) {
            return this;
        }

        @AssumeNotNull
        @Override
        public Throwable fillInStackTrace() {
            return this;
        }

        public static VarIntParseException valueOf(byte missing) {
            switch (missing) {
                case 1:
                    return ONE_BYTE;
                case 2:
                    return TWO_BYTES;
                case 3:
                    return THREE_BYTES;
                case 4:
                    return FOUR_BYTES;
                case 5:
                    return FIVE_BYTES;
                default:
                    return null;
            }
        }

    }

}
