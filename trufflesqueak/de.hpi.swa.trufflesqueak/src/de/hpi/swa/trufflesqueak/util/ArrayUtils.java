package de.hpi.swa.trufflesqueak.util;

import java.util.Arrays;

public class ArrayUtils {

    public static Object[] allButFirst(Object[] values) {
        Object[] result = new Object[values.length - 1];
        for (int i = 0; i < result.length; i++) {
            result[i] = values[1 + i];
        }
        return result;
    }

    public static byte[] swapOrderCopy(byte[] bytes) {
        return swapOrderInPlace(Arrays.copyOf(bytes, bytes.length));
    }

    public static byte[] swapOrderInPlace(byte[] bytes) {
        for (int i = 0; i < bytes.length / 2; i++) {
            byte b = bytes[i];
            bytes[i] = bytes[bytes.length - 1 - i];
            bytes[bytes.length - 1 - i] = b;
        }
        return bytes;
    }
}
