/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.util;

import java.util.AbstractCollection;
import java.util.Arrays;

import org.graalvm.collections.UnmodifiableEconomicMap;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.trufflesqueak.model.AbstractSqueakObjectWithClassAndHash;

public final class ArrayUtils {
    @CompilationFinal(dimensions = 1) public static final Object[] EMPTY_ARRAY = new Object[0];
    @CompilationFinal(dimensions = 1) public static final String[] EMPTY_STRINGS_ARRAY = new String[0];

    private ArrayUtils() {
    }

    public static Object[] allButFirst(final Object[] values) {
        return copyOfRange(values, 1, values.length);
    }

    public static void arraycopy(final Object src, final int srcPos, final Object dest, final int destPos, final int length) {
        try {
            System.arraycopy(src, srcPos, dest, destPos, length);
        } catch (final Throwable t) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw t;
        }
    }

    public static boolean contains(final byte[] objects, final byte element) {
        for (final byte object : objects) {
            if (object == element) {
                return true;
            }
        }
        return false;
    }

    public static boolean contains(final char[] objects, final char element) {
        for (final char object : objects) {
            if (object == element) {
                return true;
            }
        }
        return false;
    }

    public static boolean contains(final double[] objects, final double element) {
        for (final double object : objects) {
            if (object == element) {
                return true;
            }
        }
        return false;
    }

    public static boolean contains(final int[] objects, final int element) {
        for (final long object : objects) {
            if (object == element) {
                return true;
            }
        }
        return false;
    }

    public static boolean contains(final long[] objects, final long element) {
        for (final long object : objects) {
            if (object == element) {
                return true;
            }
        }
        return false;
    }

    public static boolean contains(final Object[] objects, final Object element) {
        for (final Object object : objects) {
            if (object == element) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsEqual(final String[] strings, final String element) {
        for (final String string : strings) {
            if (string.equals(element)) {
                return true;
            }
        }
        return false;
    }

    public static byte[] copyOf(final byte[] original, final int newLength) {
        try {
            return Arrays.copyOf(original, newLength);
        } catch (final Throwable t) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw t;
        }
    }

    public static <T> T[] copyOf(final T[] original, final int newLength) {
        try {
            return Arrays.copyOf(original, newLength);
        } catch (final Throwable t) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw t;
        }
    }

    public static <T> T[] copyOfRange(final T[] original, final int from, final int to) {
        try {
            return Arrays.copyOfRange(original, from, to);
        } catch (final Throwable t) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw t;
        }
    }

    public static Object[] copyWithFirst(final Object[] objects, final Object first) {
        final int numObjects = objects.length;
        final Object[] newObjects = new Object[numObjects + 1];
        newObjects[0] = first;
        arraycopy(objects, 0, newObjects, 1, numObjects);
        return newObjects;
    }

    public static void fill(final byte[] array, final byte value) {
        try {
            Arrays.fill(array, value);
        } catch (final Throwable t) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw t;
        }
    }

    public static void fill(final char[] array, final char value) {
        try {
            Arrays.fill(array, value);
        } catch (final Throwable t) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw t;
        }
    }

    public static void fill(final double[] array, final double value) {
        try {
            Arrays.fill(array, value);
        } catch (final Throwable t) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw t;
        }
    }

    public static void fill(final int[] array, final int value) {
        try {
            Arrays.fill(array, value);
        } catch (final Throwable t) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw t;
        }
    }

    public static void fill(final long[] array, final long value) {
        try {
            Arrays.fill(array, value);
        } catch (final Throwable t) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw t;
        }
    }

    public static void fill(final Object[] array, final int fromIndex, final int toIndex, final Object value) {
        try {
            Arrays.fill(array, fromIndex, toIndex, value);
        } catch (final Throwable t) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw t;
        }
    }

    public static void fill(final Object[] array, final Object value) {
        try {
            Arrays.fill(array, value);
        } catch (final Throwable t) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw t;
        }
    }

    public static void fill(final short[] array, final short value) {
        try {
            Arrays.fill(array, value);
        } catch (final Throwable t) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw t;
        }
    }

    @TruffleBoundary
    public static void fillRandomly(final byte[] bytes) {
        MiscUtils.getSecureRandom().nextBytes(bytes);
    }

    public static int indexOf(final long[] array, final long value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == value) {
                return i;
            }
        }
        return -1;
    }

    public static void replaceAll(final Object[] array, final UnmodifiableEconomicMap<Object, Object> objectsToReplacements) {
        for (int i = 0; i < array.length; i++) {
            final Object replacement = objectsToReplacements.get(array[i]);
            if (replacement != null) {
                array[i] = replacement;
            }
        }
    }

    public static byte[] swapOrderCopy(final byte[] bytes) {
        return swapOrderInPlace(copyOf(bytes, bytes.length));
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
    public static Object[] toArray(final AbstractCollection<AbstractSqueakObjectWithClassAndHash> list) {
        return list.toArray(new Object[0]);
    }

    @TruffleBoundary
    public static String toJoinedString(final CharSequence delimiter, final Object[] objects) {
        return String.join(delimiter, toStrings(objects));
    }

    @TruffleBoundary
    public static String[] toStrings(final Object[] objects) {
        final String[] strings = new String[objects.length];
        for (int i = 0; i < objects.length; i++) {
            strings[i] = String.valueOf(objects[i]);
        }
        return strings;
    }

    public static Object[] withAll(final int size, final Object element) {
        final Object[] array = new Object[size];
        fill(array, element);
        return array;
    }
}
