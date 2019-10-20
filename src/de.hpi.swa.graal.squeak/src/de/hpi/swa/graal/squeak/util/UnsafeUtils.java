/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.util;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;

import de.hpi.swa.graal.squeak.model.NativeObject;
import sun.misc.Unsafe;

public final class UnsafeUtils {

    private static final Unsafe UNSAFE = initUnsafe();

    private static final long ARRAY_NATIVE_OBJECT_BASE_OFFSET;
    private static final long ARRAY_NATIVE_OBJECT_INDEX_SCALE;
    private static final long ARRAY_WEAK_REFERENCE_BASE_OFFSET;
    private static final long ARRAY_WEAK_REFERENCE_INDEX_SCALE;

    static {
        ARRAY_NATIVE_OBJECT_BASE_OFFSET = UNSAFE.arrayBaseOffset(NativeObject[].class);
        ARRAY_NATIVE_OBJECT_INDEX_SCALE = UNSAFE.arrayIndexScale(NativeObject[].class);
        ARRAY_WEAK_REFERENCE_BASE_OFFSET = UNSAFE.arrayBaseOffset(WeakReference[].class);
        ARRAY_WEAK_REFERENCE_INDEX_SCALE = UNSAFE.arrayIndexScale(WeakReference[].class);
    }

    private UnsafeUtils() {
    }

    public static long getAddress(final Class<?> javaClass, final String fieldName) {
        try {
            return UNSAFE.objectFieldOffset(javaClass.getField(fieldName));
        } catch (NoSuchFieldException | SecurityException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static boolean getBoolAt(final Object object, final long address) {
        return UNSAFE.getBoolean(object, address);
    }

    public static boolean getBoolFromLongs(final long[] array, final long index) {
        assert 0 <= index && index < array.length;
        return UNSAFE.getBoolean(array, Unsafe.ARRAY_LONG_BASE_OFFSET + index * Unsafe.ARRAY_LONG_INDEX_SCALE);
    }

    public static byte getByte(final Object storage, final long index) {
        assert storage instanceof byte[] && 0 <= index && index < ((byte[]) storage).length;
        return UNSAFE.getByte(storage, Unsafe.ARRAY_BYTE_BASE_OFFSET + index * Unsafe.ARRAY_BYTE_INDEX_SCALE);
    }

    public static char getChar(final Object storage, final long index) {
        assert storage instanceof char[] && 0 <= index && index < ((char[]) storage).length;
        return UNSAFE.getChar(storage, Unsafe.ARRAY_CHAR_BASE_OFFSET + index * Unsafe.ARRAY_CHAR_INDEX_SCALE);
    }

    public static char getCharAt(final Object object, final long address) {
        return UNSAFE.getChar(object, address);
    }

    public static char getCharFromLongs(final long[] array, final long index) {
        assert 0 <= index && index < array.length;
        return UNSAFE.getChar(array, Unsafe.ARRAY_LONG_BASE_OFFSET + index * Unsafe.ARRAY_LONG_INDEX_SCALE);
    }

    public static double getDouble(final Object storage, final long index) {
        assert storage instanceof double[] && 0 <= index && index < ((double[]) storage).length;
        return UNSAFE.getDouble(storage, Unsafe.ARRAY_DOUBLE_BASE_OFFSET + index * Unsafe.ARRAY_DOUBLE_INDEX_SCALE);
    }

    public static double getDoubleAt(final Object object, final long address) {
        return UNSAFE.getDouble(object, address);
    }

    public static double getDoubleFromLongs(final long[] array, final long index) {
        assert 0 <= index && index < array.length;
        return UNSAFE.getDouble(array, Unsafe.ARRAY_LONG_BASE_OFFSET + index * Unsafe.ARRAY_LONG_INDEX_SCALE);
    }

    public static int getInt(final byte[] bytes, final long index) {
        assert 0 <= index && index * ArrayConversionUtils.INTEGER_BYTE_SIZE < bytes.length;
        return UNSAFE.getInt(bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET + index * ArrayConversionUtils.INTEGER_BYTE_SIZE * Unsafe.ARRAY_BYTE_INDEX_SCALE);
    }

    public static int getInt(final Object storage, final long index) {
        assert storage instanceof int[] && 0 <= index && index < ((int[]) storage).length;
        return UNSAFE.getInt(storage, Unsafe.ARRAY_INT_BASE_OFFSET + index * Unsafe.ARRAY_INT_INDEX_SCALE);
    }

    public static int getIntAt(final Object object, final long address) {
        return UNSAFE.getInt(object, address);
    }

    public static int getIntFromBytes(final byte[] bytes, final long index) {
        assert 0 <= index && index <= bytes.length;
        return UNSAFE.getInt(bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET + index * Unsafe.ARRAY_BYTE_INDEX_SCALE);
    }

    public static int getIntReversed(final byte[] bytes, final long index) {
        return Integer.reverseBytes(getInt(bytes, index));
    }

    public static long getLong(final byte[] bytes, final long index) {
        assert 0 <= index && index * ArrayConversionUtils.LONG_BYTE_SIZE < bytes.length;
        return UNSAFE.getLong(bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET + index * ArrayConversionUtils.LONG_BYTE_SIZE * Unsafe.ARRAY_BYTE_INDEX_SCALE);
    }

    public static long getLong(final Object storage, final long index) {
        assert storage instanceof long[] && 0 <= index && index < ((long[]) storage).length;
        return UNSAFE.getLong(storage, Unsafe.ARRAY_LONG_BASE_OFFSET + index * Unsafe.ARRAY_LONG_INDEX_SCALE);
    }

    public static long getLongAt(final Object object, final long address) {
        return UNSAFE.getLong(object, address);
    }

    public static long getLongReversed(final byte[] bytes, final long index) {
        return Long.reverseBytes(getLong(bytes, index));
    }

    public static NativeObject getNativeObject(final Object storage, final long index) {
        assert storage instanceof NativeObject[] && 0 <= index && index < ((NativeObject[]) storage).length;
        return (NativeObject) UNSAFE.getObject(storage, ARRAY_NATIVE_OBJECT_BASE_OFFSET + index * ARRAY_NATIVE_OBJECT_INDEX_SCALE);
    }

    public static Object getObject(final Object storage, final long index) {
        assert storage.getClass() == Object[].class && 0 <= index && index < ((Object[]) storage).length;
        return UNSAFE.getObject(storage, Unsafe.ARRAY_OBJECT_BASE_OFFSET + index * Unsafe.ARRAY_OBJECT_INDEX_SCALE);
    }

    public static Object getObjectAt(final Object object, final long address) {
        return UNSAFE.getObject(object, address);
    }

    public static short getShort(final byte[] bytes, final long index) {
        assert 0 <= index && index * ArrayConversionUtils.SHORT_BYTE_SIZE < bytes.length;
        return UNSAFE.getShort(bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET + index * ArrayConversionUtils.SHORT_BYTE_SIZE * Unsafe.ARRAY_BYTE_INDEX_SCALE);
    }

    public static short getShort(final int[] ints, final long index) {
        assert 0 <= index && index / ArrayConversionUtils.SHORT_BYTE_SIZE < ints.length;
        return UNSAFE.getShort(ints, Unsafe.ARRAY_INT_BASE_OFFSET + index * Unsafe.ARRAY_SHORT_INDEX_SCALE);
    }

    public static short getShort(final Object storage, final long index) {
        assert storage instanceof short[] && 0 <= index && index < ((short[]) storage).length;
        return UNSAFE.getShort(storage, Unsafe.ARRAY_SHORT_BASE_OFFSET + index * Unsafe.ARRAY_SHORT_INDEX_SCALE);
    }

    public static short getShortFromBytes(final byte[] bytes, final long index) {
        assert 0 <= index && index <= bytes.length;
        return UNSAFE.getShort(bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET + index * Unsafe.ARRAY_BYTE_INDEX_SCALE);
    }

    public static short getShortReversed(final byte[] bytes, final long index) {
        return Short.reverseBytes(getShort(bytes, index));
    }

    public static WeakReference<?> getWeakReference(final WeakReference<?>[] storage, final long index) {
        assert 0 <= index && index < storage.length;
        return (WeakReference<?>) UNSAFE.getObject(storage, ARRAY_WEAK_REFERENCE_BASE_OFFSET + index * ARRAY_WEAK_REFERENCE_INDEX_SCALE);
    }

    public static Unsafe initUnsafe() {
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

    public static void putBoolAt(final Object object, final long address, final boolean value) {
        UNSAFE.putBoolean(object, address, value);
    }

    public static void putBoolIntoLongs(final long[] array, final long index, final boolean value) {
        assert 0 <= index && index < array.length;
        UNSAFE.putBoolean(array, Unsafe.ARRAY_LONG_BASE_OFFSET + index * Unsafe.ARRAY_LONG_INDEX_SCALE, value);
    }

    public static void putByte(final Object storage, final long index, final byte value) {
        assert storage instanceof byte[] && 0 <= index && index < ((byte[]) storage).length;
        UNSAFE.putByte(storage, Unsafe.ARRAY_BYTE_BASE_OFFSET + index * Unsafe.ARRAY_BYTE_INDEX_SCALE, value);
    }

    public static void putChar(final Object storage, final long index, final char value) {
        assert storage instanceof char[] && 0 <= index && index < ((char[]) storage).length;
        UNSAFE.putChar(storage, Unsafe.ARRAY_CHAR_BASE_OFFSET + index * Unsafe.ARRAY_CHAR_INDEX_SCALE, value);
    }

    public static void putCharAt(final Object object, final long address, final char value) {
        UNSAFE.putChar(object, address, value);
    }

    public static void putCharIntoLongs(final long[] array, final long index, final char value) {
        assert 0 <= index && index < array.length;
        UNSAFE.putChar(array, Unsafe.ARRAY_LONG_BASE_OFFSET + index * Unsafe.ARRAY_LONG_INDEX_SCALE, value);
    }

    public static double putDouble(final long[] array, final long index) {
        return UNSAFE.getDouble(array, Unsafe.ARRAY_LONG_BASE_OFFSET + index * Unsafe.ARRAY_LONG_INDEX_SCALE);
    }

    public static void putDouble(final Object storage, final long index, final double value) {
        assert storage instanceof double[] && 0 <= index && index < ((double[]) storage).length;
        UNSAFE.putDouble(storage, Unsafe.ARRAY_DOUBLE_BASE_OFFSET + index * Unsafe.ARRAY_DOUBLE_INDEX_SCALE, value);
    }

    public static void putDoubleAt(final Object object, final long address, final double value) {
        UNSAFE.putDouble(object, address, value);
    }

    public static void putDoubleIntoLongs(final long[] array, final long index, final double value) {
        UNSAFE.putDouble(array, Unsafe.ARRAY_LONG_BASE_OFFSET + index * Unsafe.ARRAY_LONG_INDEX_SCALE, value);
    }

    public static void putInt(final byte[] bytes, final long index, final int value) {
        assert 0 <= index && index * ArrayConversionUtils.SHORT_BYTE_SIZE < bytes.length;
        UNSAFE.putInt(bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET + index * ArrayConversionUtils.INTEGER_BYTE_SIZE * Unsafe.ARRAY_BYTE_INDEX_SCALE, value);
    }

    public static void putInt(final Object storage, final long index, final int value) {
        assert storage instanceof int[] && 0 <= index && index < ((int[]) storage).length;
        UNSAFE.putInt(storage, Unsafe.ARRAY_INT_BASE_OFFSET + index * Unsafe.ARRAY_INT_INDEX_SCALE, value);
    }

    public static void putIntAt(final Object object, final long address, final int value) {
        UNSAFE.putInt(object, address, value);
    }

    public static void putIntIntoBytes(final byte[] bytes, final long index, final int value) {
        assert 0 <= index && index <= bytes.length;
        UNSAFE.putInt(bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET + index * Unsafe.ARRAY_BYTE_INDEX_SCALE, value);
    }

    public static void putIntReversed(final byte[] bytes, final long index, final int value) {
        putInt(bytes, index, Integer.reverseBytes(value));
    }

    public static void putLong(final byte[] bytes, final long index, final long value) {
        assert 0 <= index && index * ArrayConversionUtils.INTEGER_BYTE_SIZE < bytes.length;
        UNSAFE.putLong(bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET + index * ArrayConversionUtils.LONG_BYTE_SIZE * Unsafe.ARRAY_BYTE_INDEX_SCALE, value);
    }

    public static void putLong(final Object storage, final long index, final long value) {
        assert storage instanceof long[] && 0 <= index && index < ((long[]) storage).length;
        UNSAFE.putLong(storage, Unsafe.ARRAY_LONG_BASE_OFFSET + index * Unsafe.ARRAY_LONG_INDEX_SCALE, value);
    }

    public static void putLongAt(final Object object, final long address, final long value) {
        UNSAFE.putLong(object, address, value);
    }

    public static void putLongIntoBytes(final byte[] bytes, final long index, final long value) {
        assert 0 <= index && index <= bytes.length;
        UNSAFE.putLong(bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET + index * Unsafe.ARRAY_BYTE_INDEX_SCALE, value);
    }

    public static void putLongReversed(final byte[] bytes, final long i, final long value) {
        putLong(bytes, i, Long.reverseBytes(value));
    }

    public static void putNativeObject(final Object storage, final long index, final NativeObject value) {
        assert storage instanceof NativeObject[] && 0 <= index && index < ((NativeObject[]) storage).length;
        UNSAFE.putObject(storage, ARRAY_NATIVE_OBJECT_BASE_OFFSET + index * ARRAY_NATIVE_OBJECT_INDEX_SCALE, value);
    }

    public static void putObject(final Object storage, final long index, final Object value) {
        assert storage.getClass() == Object[].class && 0 <= index && index < ((Object[]) storage).length;
        UNSAFE.putObject(storage, Unsafe.ARRAY_OBJECT_BASE_OFFSET + index * Unsafe.ARRAY_OBJECT_INDEX_SCALE, value);
    }

    public static void putObjectAt(final Object storage, final long address, final Object value) {
        UNSAFE.putObject(storage, address, value);
    }

    public static void putShort(final byte[] bytes, final long index, final short value) {
        assert 0 <= index && index * ArrayConversionUtils.SHORT_BYTE_SIZE < bytes.length;
        UNSAFE.putShort(bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET + index * ArrayConversionUtils.SHORT_BYTE_SIZE * Unsafe.ARRAY_BYTE_INDEX_SCALE, value);
    }

    public static void putShort(final int[] ints, final long index, final short value) {
        assert 0 <= index && index / ArrayConversionUtils.SHORT_BYTE_SIZE < ints.length;
        UNSAFE.putShort(ints, Unsafe.ARRAY_INT_BASE_OFFSET + index * Unsafe.ARRAY_SHORT_INDEX_SCALE, value);
    }

    public static void putShort(final Object storage, final long index, final short value) {
        assert storage instanceof short[] && 0 <= index && index < ((short[]) storage).length;
        UNSAFE.putShort(storage, Unsafe.ARRAY_SHORT_BASE_OFFSET + index * Unsafe.ARRAY_SHORT_INDEX_SCALE, value);
    }

    public static void putShortIntoBytes(final byte[] bytes, final long index, final short value) {
        assert 0 <= index && index <= bytes.length;
        UNSAFE.putShort(bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET + index * Unsafe.ARRAY_BYTE_INDEX_SCALE, value);
    }

    public static void putShortReversed(final byte[] bytes, final long index, final short value) {
        putShort(bytes, index, Short.reverseBytes(value));
    }

    public static void putWeakReference(final WeakReference<?>[] storage, final long index, final WeakReference<?> value) {
        assert 0 <= index && index < storage.length;
        UNSAFE.putObject(storage, ARRAY_WEAK_REFERENCE_BASE_OFFSET + index * ARRAY_WEAK_REFERENCE_INDEX_SCALE, value);
    }
}
