package de.hpi.swa.trufflesqueak.util;

import java.util.Arrays;

public class ArrayUtils {

    public static final Object[] allButFirst(final Object[] values) {
        final Object[] result = new Object[values.length - 1];
        for (int i = 0; i < result.length; i++) {
            result[i] = values[1 + i];
        }
        return result;
    }

    public static final byte[] swapOrderCopy(final byte[] bytes) {
        return swapOrderInPlace(Arrays.copyOf(bytes, bytes.length));
    }

    public static final byte[] swapOrderInPlace(final byte[] bytes) {
        for (int i = 0; i < bytes.length / 2; i++) {
            byte b = bytes[i];
            bytes[i] = bytes[bytes.length - 1 - i];
            bytes[bytes.length - 1 - i] = b;
        }
        return bytes;
    }

    public static final Object[] withAll(final int size, final Object element) {
        final Object[] array = new Object[size];
        Arrays.fill(array, element);
        return array;
    }
}
