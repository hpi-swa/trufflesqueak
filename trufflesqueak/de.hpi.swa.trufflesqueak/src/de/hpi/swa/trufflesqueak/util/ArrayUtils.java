package de.hpi.swa.trufflesqueak.util;

public class ArrayUtils {

    public static Object[] allButFirst(Object[] values) {
        Object[] result = new Object[values.length - 1];
        for (int i = 0; i < result.length; i++) {
            result[i] = values[1 + i];
        }
        return result;
    }
}
