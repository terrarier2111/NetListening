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

import de.terrarier.netlistening.internal.AssumeNotNull;
import de.terrarier.netlistening.internal.CancelReadSignal;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.ApiStatus;

/**
 * @author Terrarier2111
 * @since 1.0
 */
@ApiStatus.Internal
public final class VarIntUtil {

    private static final CancelReadSignal ONE_BYTE = new CancelReadSignal(1);
    private static final CancelReadSignal TWO_BYTES = new CancelReadSignal(2);
    private static final CancelReadSignal THREE_BYTES = new CancelReadSignal(3);
    public static final CancelReadSignal FOUR_BYTES = new CancelReadSignal(4);
    private static final CancelReadSignal FIVE_BYTES = new CancelReadSignal(5);
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

	public static void writeVarInt(int value, @AssumeNotNull ByteBuf out) {
		if (value > 0x0FFFFFFF || value < 0) out.writeByte((byte) (0x80 | ((value >>> 28))));
		if (value > 0x1FFFFF || value < 0)   out.writeByte((byte) (0x80 | ((value >>> 21) & 0x7F)));
		if (value > 0x3FFF || value < 0)     out.writeByte((byte) (0x80 | ((value >>> 14) & 0x7F)));
		if (value > 0x7F || value < 0)       out.writeByte((byte) (0x80 | ((value >>>  7) & 0x7F)));

        out.writeByte((byte) (value & 0x7F));
    }

    public static int getVarInt(@AssumeNotNull ByteBuf buffer) throws CancelReadSignal {
        if (!buffer.isReadable()) {
            throw ONE_BYTE;
        }
        byte b = buffer.readByte();

        if (b == (byte) 0x80)
            throw new RuntimeException("Attempting to read null value as int");

        int value = b & 0x7F;
        byte required = 0;
        while ((b & 0x80) != 0) {
            required++;
            if (!buffer.isReadable()) {
                throw valueOf(required);
            }
            b = buffer.readByte();
            value <<= 7;
            value |= b & 0x7F;
        }

        return value;
    }

    private static CancelReadSignal valueOf(byte missing) {
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
                throw new UnsupportedOperationException("Var ints may exclusively have the size of 1-5 bytes.");
        }
    }

}
