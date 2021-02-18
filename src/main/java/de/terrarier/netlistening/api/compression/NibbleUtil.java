package de.terrarier.netlistening.api.compression;

/**
 * This util is used to work with 4 bit integers (nibbles)
 * combined together in pairs of two to single bytes.
 *
 * @since 1.0
 * @author Terrarier2111
 */
public final class NibbleUtil {

    private NibbleUtil() {
        throw new UnsupportedOperationException("This class may not be instantiated!");
    }

    /**
     * @param combined a byte representing a pair of nibbles.
     * @return the first nibble contained in the passed byte.
     */
    public static byte getFirstNibble(byte combined) {
        return (byte) (combined & 0xF);
    }

    /**
     * @param combined a byte representing a pair of nibbles.
     * @return the second nibble contained in the passed byte.
     */
    public static byte getSecondNibble(byte combined) {
        return (byte) ((combined >>> 4) & 0xF);
    }

    /**
     * @param firstNibble the first nibble.
     * @param secondNibble the second nibble.
     * @return a byte representing a pair of nibbles.
     */
    public static byte buildNibblePair(byte firstNibble, byte secondNibble) {
        return (byte) ((firstNibble & 0xF) | ((secondNibble & 0xF) << 4));
    }

    public static int nibbleToByteCount(int nibbles) {
        final int expectedBytes = nibbles / 2;
        return ((expectedBytes * 2) == nibbles) ? expectedBytes : (expectedBytes + 1); // workaround so we don't have to perform a second division because they are expensive
    }

}
