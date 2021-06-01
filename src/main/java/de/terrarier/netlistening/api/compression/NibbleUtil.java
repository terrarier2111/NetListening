/*
Copyright 2021 Terrarier2111

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package de.terrarier.netlistening.api.compression;

import org.jetbrains.annotations.ApiStatus;

/**
 * This util can be used to work with 4 bit integers (nibbles)
 * combined together in pairs of two to single bytes.
 *
 * @author Terrarier2111
 * @since 1.0
 */
@ApiStatus.Internal
public final class NibbleUtil {

    private NibbleUtil() {
        throw new UnsupportedOperationException("This class may not be instantiated!");
    }

    /**
     * @param pair a byte representing a pair of nibbles.
     * @return the high nibble contained in the passed byte.
     */
    public static byte highNibble(byte pair) {
        return (byte) (pair & 0xF);
    }

    /**
     * @param pair a byte representing a pair of nibbles.
     * @return the low nibble contained in the passed byte.
     */
    public static byte lowNibble(byte pair) {
        return (byte) ((pair >>> 4) & 0xF);
    }

    /**
     * @param highNibble the high nibble.
     * @param lowNibble  the low nibble.
     * @return a byte representing a pair of nibbles.
     */
    public static byte buildNibblePair(byte highNibble, byte lowNibble) {
        return (byte) ((highNibble & 0xF) | ((lowNibble & 0xF) << 4));
    }

    /**
     * @param nibbleCount the nibble count which is to be converted.
     * @return the number of nibbles converted into its respective number of bytes.
     */
    public static int nibbleToByteCount(int nibbleCount) {
        final int expectedBytes = nibbleCount / 2;
        return expectedBytes + nibbleCount - expectedBytes * 2; // a branch prediction friendly solution
    }

}
