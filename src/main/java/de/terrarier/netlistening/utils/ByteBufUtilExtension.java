/*
Copyright 2021 The Netty Project/Terrarier2111

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
package de.terrarier.netlistening.utils;

import de.terrarier.netlistening.internals.AssumeNotNull;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.util.internal.SystemPropertyUtil;
import org.jetbrains.annotations.ApiStatus;

import java.util.Arrays;

import static io.netty.util.internal.EmptyArrays.EMPTY_BYTES;

/**
 * @author The Netty Project/Terrarier2111
 * @since 1.0
 */
@ApiStatus.Internal
public final class ByteBufUtilExtension {

    private static final boolean NEW_NETTY_VERSION;
    private static final boolean CHECK_BOUNDS = SystemPropertyUtil.getBoolean("de.terrarier.netlistening.CheckBounds",
            false);

    static {
        boolean newNettyVersion;
        try {
            ByteBufUtil.class.getDeclaredMethod("getBytes", ByteBuf.class, int.class, int.class);
            newNettyVersion = true;
        } catch (NoSuchMethodException e) {
            // Apparently we are using an old version of netty and we have to support
            // getBytes on our own and can't rely on Netty's ByteBufUtil#getBytes method.
            newNettyVersion = false;
        }
        NEW_NETTY_VERSION = newNettyVersion;
    }

    private ByteBufUtilExtension() {
        throw new UnsupportedOperationException("This class may not be instantiated!");
    }

    @AssumeNotNull
    public static byte[] readBytes(@AssumeNotNull ByteBuf buffer, int bytes) {
        if (bytes == 0) {
            return EMPTY_BYTES;
        }

        final byte[] read = getBytes(buffer, bytes);
        buffer.skipBytes(bytes);
        return read;
    }

    public static void writeBytes(@AssumeNotNull ByteBuf buf, @AssumeNotNull byte[] bytes, int buffer) {
        final int length = bytes.length;
        correctSize(buf, 4 + length, buffer);
        buf.writeInt(length);
        if (length > 0) {
            buf.writeBytes(bytes);
        }
    }

    public static void correctSize(@AssumeNotNull ByteBuf buf, int bytes, int buffer) {
        final int capacity = buf.capacity();
        final int available = capacity - (buf.writerIndex() + bytes);

        if (available < 0) {
            buf.capacity(capacity - available + buffer);
        }
    }

    @AssumeNotNull
    public static byte[] getBytes(@AssumeNotNull ByteBuf buffer, int bytes) {
        if (bytes == 0) {
            return EMPTY_BYTES;
        }

        return NEW_NETTY_VERSION ? ByteBufUtil.getBytes(buffer, buffer.readerIndex(), bytes) : getBytes0(buffer, bytes);
    }

    @AssumeNotNull
    public static byte[] getBytes(@AssumeNotNull ByteBuf buffer) {
        return getBytes(buffer, buffer.readableBytes());
    }

    /**
     * Copied from netty to allow the usage of older netty versions:
     * <p>
     * https://github.com/netty/netty/blob/4.1/buffer/src/main/java/io/netty/buffer/ByteBufUtil.java
     */
    @AssumeNotNull
    private static byte[] getBytes0(@AssumeNotNull ByteBuf buffer, int length) {
        final int start = buffer.readerIndex();
        final int capacity = buffer.capacity();

        if (CHECK_BOUNDS && isOutOfBounds(start, length, capacity))
            throw new IndexOutOfBoundsException("expected: 0 <= start(" + start + ") <= start + length(" + length
                    + ") <= buf.capacity(" + capacity + ')');

        if (buffer.hasArray()) {
            final int baseOffset = buffer.arrayOffset() + start;
            final byte[] bytes = buffer.array();
            if (/*copy || */baseOffset != 0 || length != bytes.length) {
                return Arrays.copyOfRange(bytes, baseOffset, baseOffset + length);
            }
            return bytes;
        }

        final byte[] bytes = new byte[length];
        buffer.getBytes(start, bytes);
        return bytes;
    }

    /**
     * Copied from netty to allow the usage of older netty versions:
     * <p>
     * https://github.com/netty/netty/blob/4.1/common/src/main/java/io/netty/util/internal/MathUtil.java
     */
    private static boolean isOutOfBounds(int index, int length, int capacity) {
        return (index | length | capacity | (index + length) | (capacity - (index + length))) < 0;
    }

}
