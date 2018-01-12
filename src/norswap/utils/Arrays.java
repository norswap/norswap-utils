package norswap.utils;

import java.util.function.Function;

/**
 * Utilities to deal with arrays.
 */
public final class Arrays
{
    // ---------------------------------------------------------------------------------------------

    /**
     * Returns an array obtained by applying the function {@code f} to each item in {@code array}.
     */
    public static <T, R> R[] map (T[] array, R[] witness, Function<T, R> f)
    {
        R[] out = java.util.Arrays.copyOf(witness, array.length);
        for (int i = 0; i < array.length; ++i)
            out[i] = f.apply(array[i]);
        return out;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a copy of {@code array} whose size is the least power of two greater or equal
     * to {@code min_size}. Max admissible value for min_size is 2^30.
     */
    public static <T> T[] resize_binary_power (T[] array, int min_size)
    {
        int size = 1;
        while (size < min_size) size <<= 1;
        return java.util.Arrays.copyOf(array, size);
    }

    // ---------------------------------------------------------------------------------------------
}
