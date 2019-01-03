package de.hpi.swa.graal.squeak.util;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;

public final class ArrayUtils {
    public static final Object[] EMPTY_ARRAY = new Object[0];

    private static final Random RANDOM = new Random();

    private ArrayUtils() {
    }

    public static Object[] allButFirst(final Object[] values) {
        return Arrays.copyOfRange(values, 1, values.length);
    }

    public static boolean contains(final byte[] objects, final byte element) {
        for (int i = 0; i < objects.length; i++) {
            if (objects[i] == element) {
                return true;
            }
        }
        return false;
    }

    public static boolean contains(final char[] objects, final char element) {
        for (int i = 0; i < objects.length; i++) {
            if (objects[i] == element) {
                return true;
            }
        }
        return false;
    }

    public static boolean contains(final long[] objects, final long element) {
        for (int i = 0; i < objects.length; i++) {
            if (objects[i] == element) {
                return true;
            }
        }
        return false;
    }

    public static boolean contains(final double[] objects, final double element) {
        for (int i = 0; i < objects.length; i++) {
            if (objects[i] == element) {
                return true;
            }
        }
        return false;
    }

    public static boolean contains(final Object[] objects, final Object element) {
        for (int i = 0; i < objects.length; i++) {
            if (objects[i] == element) {
                return true;
            }
        }
        return false;
    }

    public static Object[] copyWithFirst(final Object[] objects, final Object first) {
        final int numObjects = objects.length;
        final Object[] newObjects = new Object[numObjects + 1];
        newObjects[0] = first;
        for (int i = 0; i < numObjects; i++) {
            newObjects[1 + i] = objects[i];
        }
        return newObjects;
    }

    @TruffleBoundary
    public static void fillRandomly(final byte[] bytes) {
        RANDOM.nextBytes(bytes);
    }

    public static Object[] fillWith(final Object[] input, final int newSize, final Object fill) {
        final int inputSize = input.length;
        if (inputSize >= newSize) {
            return input;
        } else {
            final Object[] array = Arrays.copyOf(input, newSize);
            Arrays.fill(array, inputSize, newSize, fill);
            return array;
        }
    }

    public static byte[] swapOrderCopy(final byte[] bytes) {
        return swapOrderInPlace(Arrays.copyOf(bytes, bytes.length));
    }

    public static byte[] swapOrderInPlace(final byte[] bytes) {
        for (int i = 0; i < bytes.length / 2; i++) {
            final byte b = bytes[i];
            bytes[i] = bytes[bytes.length - 1 - i];
            bytes[bytes.length - 1 - i] = b;
        }
        return bytes;
    }

    @TruffleBoundary
    public static Object[] toArray(final List<AbstractSqueakObject> list) {
        return list.toArray(); // needs to be behind a TruffleBoundary
    }

    @TruffleBoundary
    public static String[] toStrings(final Object[] objects) {
        final String[] strings = new String[objects.length];
        for (int i = 0; i < objects.length; i++) {
            strings[i] = objects[i] == null ? "null" : objects[i].toString();
        }
        return strings;
    }

    @TruffleBoundary
    public static String toJoinedString(final CharSequence delimiter, final Object[] objects) {
        return String.join(delimiter, toStrings(objects));
    }

    public static Object[] withAll(final int size, final Object element) {
        final Object[] array = new Object[size];
        Arrays.fill(array, element);
        return array;
    }
}
