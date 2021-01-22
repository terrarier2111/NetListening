package de.terrarier.netlistening.api.compression;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class NibbleUtil {

    private NibbleUtil() {}

    public static byte getFirstNibble(byte combined) {
        return (byte) (combined & 0xF);
    }

    public static byte getSecondNibble(byte combined) {
        return (byte) ((combined >>> 4) & 0xF);
    }

    public static byte buildByte(byte firstNibble, byte secondNibble) {
        return (byte) ((firstNibble & 0xF) | ((secondNibble & 0xF) << 4));
    }

    public static int nibbleToByteSize(int nibbles) {
        final int expectedBytes = nibbles / 2;
        return ((expectedBytes * 2) == nibbles) ? expectedBytes : (expectedBytes + 1); // workaround so we don't have to perform a second division cuz it's expensive
    }

}
