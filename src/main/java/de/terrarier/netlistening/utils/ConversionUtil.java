package de.terrarier.netlistening.utils;

import de.terrarier.netlistening.internals.AssumeNotNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * @since 1.0
 * @author Terrarier2111
 */
@ApiStatus.Internal
public final class ConversionUtil {
	
	private ConversionUtil() {
		throw new UnsupportedOperationException("This class may not be instantiated!");
	}

	@AssumeNotNull
	public static byte[] intToBytes(int value) {
		return new byte[] { (byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8), (byte) value };
	}

	@AssumeNotNull
	public static void intToBytes(@AssumeNotNull byte[] bytes, int offset, int value) {
		bytes[offset++] = (byte) (value >> 24);
		bytes[offset++] = (byte) (value >> 16);
		bytes[offset++] = (byte) (value >> 8);
		bytes[offset]   = (byte)  value;
	}

	public static short getShortFromByteArray(@AssumeNotNull byte[] bytes, int offset) {
		return (short) (bytes[offset] << 8 | bytes[offset + 1] & 0xFF);
	}

}
