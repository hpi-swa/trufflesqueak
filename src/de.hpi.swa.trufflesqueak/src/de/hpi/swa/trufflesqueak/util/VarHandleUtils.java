/*
 * Copyright (c) 2022-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2022-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

public final class VarHandleUtils {
    private static final VarHandle DOUBLE = MethodHandles.byteArrayViewVarHandle(double[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle FLOAT = MethodHandles.byteArrayViewVarHandle(float[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle INT = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle INT_BIG_ENDIAN = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.BIG_ENDIAN);
    private static final VarHandle LONG = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle SHORT = MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.LITTLE_ENDIAN);

    private VarHandleUtils() {
    }

    public static double getDouble(final byte[] storage, final int index) {
        return getDoubleFromBytes(storage, index * Double.BYTES);
    }

    public static double getDoubleFromBytes(final byte[] storage, final int byteIndex) {
        return (double) DOUBLE.get(storage, byteIndex);
    }

    public static float getFloat(final byte[] storage, final int index) {
        return getFloatFromBytes(storage, index * Float.BYTES);
    }

    public static float getFloatFromBytes(final byte[] storage, final int byteIndex) {
        return (float) FLOAT.get(storage, byteIndex);
    }

    public static int getInt(final byte[] storage, final int index) {
        return getIntFromBytes(storage, index * Integer.BYTES);
    }

    public static int getIntFromBytes(final byte[] storage, final int byteIndex) {
        return (int) INT.get(storage, byteIndex);
    }

    public static int getIntReversed(final byte[] storage, final int index) {
        return (int) INT_BIG_ENDIAN.get(storage, index * Integer.BYTES);
    }

    public static long getLong(final byte[] storage, final int index) {
        return getLongFromBytes(storage, index * Long.BYTES);
    }

    public static long getLongFromBytes(final byte[] storage, final int byteIndex) {
        return (long) LONG.get(storage, byteIndex);
    }

    public static short getShort(final byte[] storage, final int index) {
        return getShortFromBytes(storage, index * Short.BYTES);
    }

    public static short getShortFromBytes(final byte[] storage, final int byteIndex) {
        return (short) SHORT.get(storage, byteIndex);
    }

    public static void putDouble(final byte[] storage, final int index, final double value) {
        putDoubleIntoBytes(storage, index * Double.BYTES, value);
    }

    public static void putDoubleIntoBytes(final byte[] storage, final int byteIndex, final double value) {
        DOUBLE.set(storage, byteIndex, value);
    }

    public static void putFloat(final byte[] storage, final int index, final float value) {
        putFloatIntoBytes(storage, index * Float.BYTES, value);
    }

    public static void putFloatIntoBytes(final byte[] storage, final int byteIndex, final float value) {
        FLOAT.set(storage, byteIndex, value);
    }

    public static void putInt(final byte[] storage, final int index, final int value) {
        putIntIntoBytes(storage, index * Integer.BYTES, value);
    }

    public static void putIntIntoBytes(final byte[] storage, final int byteIndex, final int value) {
        INT.set(storage, byteIndex, value);
    }

    public static void putIntReversed(final byte[] storage, final int index, final int value) {
        INT_BIG_ENDIAN.set(storage, index * Integer.BYTES, value);
    }

    public static void putLong(final byte[] storage, final int index, final long value) {
        putLongIntoBytes(storage, index * Long.BYTES, value);
    }

    public static void putLongIntoBytes(final byte[] storage, final int byteIndex, final long value) {
        LONG.set(storage, byteIndex, value);
    }

    public static void putShort(final byte[] storage, final int index, final short value) {
        putShortIntoBytes(storage, index * Short.BYTES, value);
    }

    public static void putShortIntoBytes(final byte[] storage, final int byteIndex, final short value) {
        SHORT.set(storage, byteIndex, value);
    }
}
