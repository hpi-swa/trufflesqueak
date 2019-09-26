/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.util;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

public final class UnsafeUtils {

    public static final Unsafe UNSAFE = initUnsafe();

    private UnsafeUtils() {
    }

    private static Unsafe initUnsafe() {
        try {
            // Fast path when we are trusted.
            return Unsafe.getUnsafe();
        } catch (final SecurityException se) {
            // Slow path when we are not trusted.
            try {
                final Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                theUnsafe.setAccessible(true);
                return (Unsafe) theUnsafe.get(Unsafe.class);
            } catch (final Exception e) {
                throw new RuntimeException("exception while trying to get Unsafe", e);
            }
        }
    }

    public static int getInt(final byte[] bytes, final long index) {
        assert 0 <= index && index * ArrayConversionUtils.INTEGER_BYTE_SIZE < bytes.length;
        return UnsafeUtils.UNSAFE.getInt(bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET + index * ArrayConversionUtils.INTEGER_BYTE_SIZE * Unsafe.ARRAY_BYTE_INDEX_SCALE);
    }

    public static int getIntReversed(final byte[] bytes, final long index) {
        return Integer.reverseBytes(getInt(bytes, index));
    }

    public static long getLong(final byte[] bytes, final long index) {
        assert 0 <= index && index * ArrayConversionUtils.LONG_BYTE_SIZE < bytes.length;
        return UnsafeUtils.UNSAFE.getLong(bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET + index * ArrayConversionUtils.LONG_BYTE_SIZE * Unsafe.ARRAY_BYTE_INDEX_SCALE);
    }

    public static long getLongReversed(final byte[] bytes, final long index) {
        return Long.reverseBytes(getLong(bytes, index));
    }

    public static short getShort(final byte[] bytes, final long index) {
        assert 0 <= index && index * ArrayConversionUtils.SHORT_BYTE_SIZE < bytes.length;
        return UnsafeUtils.UNSAFE.getShort(bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET + index * ArrayConversionUtils.SHORT_BYTE_SIZE * Unsafe.ARRAY_BYTE_INDEX_SCALE);
    }

    public static short getShort(final int[] ints, final long index) {
        assert 0 <= index && index / ArrayConversionUtils.SHORT_BYTE_SIZE < ints.length;
        return UNSAFE.getShort(ints, Unsafe.ARRAY_INT_BASE_OFFSET + index * Unsafe.ARRAY_SHORT_INDEX_SCALE);
    }

    public static short getShortReversed(final byte[] bytes, final long index) {
        return Short.reverseBytes(getShort(bytes, index));
    }

    public static void putInt(final byte[] bytes, final long index, final int value) {
        assert 0 <= index && index * ArrayConversionUtils.SHORT_BYTE_SIZE < bytes.length;
        UnsafeUtils.UNSAFE.putInt(bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET + index * ArrayConversionUtils.INTEGER_BYTE_SIZE * Unsafe.ARRAY_BYTE_INDEX_SCALE, value);
    }

    public static void putIntReversed(final byte[] bytes, final long index, final int value) {
        putInt(bytes, index, Integer.reverseBytes(value));
    }

    public static void putLong(final byte[] bytes, final long index, final long value) {
        assert 0 <= index && index * ArrayConversionUtils.INTEGER_BYTE_SIZE < bytes.length;
        UnsafeUtils.UNSAFE.putLong(bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET + index * ArrayConversionUtils.LONG_BYTE_SIZE * Unsafe.ARRAY_BYTE_INDEX_SCALE, value);
    }

    public static void putLongReversed(final byte[] bytes, final long i, final long value) {
        putLong(bytes, i, Long.reverseBytes(value));
    }

    public static void putShort(final byte[] bytes, final long index, final short value) {
        assert 0 <= index && index * ArrayConversionUtils.SHORT_BYTE_SIZE < bytes.length;
        UnsafeUtils.UNSAFE.putShort(bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET + index * ArrayConversionUtils.SHORT_BYTE_SIZE * Unsafe.ARRAY_BYTE_INDEX_SCALE, value);
    }

    public static void putShort(final int[] ints, final long index, final short value) {
        assert 0 <= index && index / ArrayConversionUtils.SHORT_BYTE_SIZE < ints.length;
        UNSAFE.putShort(ints, Unsafe.ARRAY_INT_BASE_OFFSET + index * Unsafe.ARRAY_SHORT_INDEX_SCALE, value);
    }

    public static void putShortReversed(final byte[] bytes, final long index, final short value) {
        putShort(bytes, index, Short.reverseBytes(value));
    }
}
