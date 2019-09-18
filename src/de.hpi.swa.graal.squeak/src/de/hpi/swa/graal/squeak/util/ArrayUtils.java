/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.util;

import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Random;
import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObjectWithHash;

public final class ArrayUtils {
    @CompilationFinal(dimensions = 1) public static final Object[] EMPTY_ARRAY = new Object[0];

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
        System.arraycopy(objects, 0, newObjects, 1, numObjects);
        return newObjects;
    }

    public static boolean equals(final byte[] bytes, final String string) {
        final int bytesLength = bytes.length;
        if (bytesLength == string.length()) {
            for (int i = 0; i < bytesLength; i++) {
                if (bytes[i] != string.charAt(i)) {
                    return false;
                }
            }
            return true;
        }
        return false;
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

    public static int indexOf(final long[] array, final long value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == value) {
                return i;
            }
        }
        return -1;
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
    public static Object[] toArray(final AbstractCollection<AbstractSqueakObjectWithHash> list) {
        return list.toArray(new Object[list.size()]);
    }

    @TruffleBoundary
    public static Object[] toArray(final Set<AbstractSqueakObject> list) {
        return list.toArray(new Object[list.size()]);
    }

    @TruffleBoundary
    public static String[] toStrings(final Object[] objects) {
        final String[] strings = new String[objects.length];
        for (int i = 0; i < objects.length; i++) {
            strings[i] = String.valueOf(objects[i]);
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
