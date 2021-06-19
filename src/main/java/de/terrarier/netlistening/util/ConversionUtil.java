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
package de.terrarier.netlistening.util;

import de.terrarier.netlistening.internal.AssumeNotNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * @author Terrarier2111
 * @since 1.0
 */
@ApiStatus.Internal
public final class ConversionUtil {

    private ConversionUtil() {
        throw new UnsupportedOperationException("This class may not be instantiated!");
    }

    @AssumeNotNull
    public static byte[] intToBytes(int value) {
        return new byte[]{
                (byte) (value >> 24),
                (byte) (value >> 16),
                (byte) (value >> 8),
                (byte) value
        };
    }

    @AssumeNotNull
    public static void intToBytes(@AssumeNotNull byte[] bytes, int offset, int value) {
        bytes[offset++] = (byte) (value >> 24);
        bytes[offset++] = (byte) (value >> 16);
        bytes[offset++] = (byte) (value >> 8);
        bytes[offset] = (byte) value;
    }

    public static int getIntFromByteArray(@AssumeNotNull byte[] bytes, int offset) {
        return (bytes[offset++] & 0xFF) << 24 |
                (bytes[offset++] & 0xFF) << 16 |
                (bytes[offset++] & 0xFF) << 8 |
                (bytes[offset] & 0xFF) & 0xFF;
    }

}
