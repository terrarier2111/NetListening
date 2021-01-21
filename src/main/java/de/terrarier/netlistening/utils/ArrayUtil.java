package de.terrarier.netlistening.utils;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class ArrayUtil {

    private ArrayUtil() {}

    @SuppressWarnings("unchecked")
    @NotNull
    public static <T> T[] reduceSize(@NotNull T[] array, int reduction) {
        final int length = array.length - reduction;
        final T[] ret = (T[]) Array.newInstance(array.getClass(), length);
        System.arraycopy(array, 0, ret, 0, length);
        return ret;
    }

}
