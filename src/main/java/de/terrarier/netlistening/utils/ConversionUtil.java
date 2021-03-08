package de.terrarier.netlistening.utils;

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
	
	public static byte[] intToByteArray(int value) {
		return new byte[] { (byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8), (byte) value };
	}

	public static short getShortFromByteArray(byte[] bytes, int offset) {
		return (short) (bytes[offset] << 8 | bytes[offset + 1] & 0xFF);
	}

}
