package de.terrarier.netlistening.utils;

import org.jetbrains.annotations.ApiStatus;

/**
 * @since 1.08
 * @author Terrarier2111
 */
@ApiStatus.Internal
public final class ObjectUtilFallback {

    private ObjectUtilFallback() {
        throw new UnsupportedOperationException("This class may not be instantiated!");
    }

    /**
     * Copied from netty to allow the usage of older netty versions:
     *
     * @see <a href="https://github.com/netty/netty/blob/4.1/common/src/main/java/io/netty/util/internal/ObjectUtil.java">https://github.com/netty/netty/blob/4.1/common/src/main/java/io/netty/util/internal/ObjectUtil.java</a>
     */
    public static int checkPositive(int i, String name) {
        if (i <= 0) {
            throw new IllegalArgumentException(name + " : " + i + " (expected: > 0)");
        }
        return i;
    }

    /**
     * Copied from netty to allow the usage of older netty versions:
     *
     * @see <a href="https://github.com/netty/netty/blob/4.1/common/src/main/java/io/netty/util/internal/ObjectUtil.java">https://github.com/netty/netty/blob/4.1/common/src/main/java/io/netty/util/internal/ObjectUtil.java</a>
     */
    public static long checkPositive(long l, String name) {
        if (l <= 0L) {
            throw new IllegalArgumentException(name + " : " + l + " (expected: > 0)");
        }
        return l;
    }

    /**
     * Copied from netty to allow the usage of older netty versions:
     *
     * @see <a href="https://github.com/netty/netty/blob/4.1/common/src/main/java/io/netty/util/internal/ObjectUtil.java">https://github.com/netty/netty/blob/4.1/common/src/main/java/io/netty/util/internal/ObjectUtil.java</a>
     */
    public static int checkPositiveOrZero(int i, String name) {
        if (i < 0) {
            throw new IllegalArgumentException(name + " : " + i + " (expected: >= 0)");
        }
        return i;
    }

    /**
     * Copied from netty to allow the usage of older netty versions:
     *
     * @see <a href="https://github.com/netty/netty/blob/4.1/common/src/main/java/io/netty/util/internal/ObjectUtil.java">https://github.com/netty/netty/blob/4.1/common/src/main/java/io/netty/util/internal/ObjectUtil.java</a>
     */
    public static long checkPositiveOrZero(long l, String name) {
        if (l < 0L) {
            throw new IllegalArgumentException(name + " : " + l + " (expected: >= 0)");
        }
        return l;
    }

}
