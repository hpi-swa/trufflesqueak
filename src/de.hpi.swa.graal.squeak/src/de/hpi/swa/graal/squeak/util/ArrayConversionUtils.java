package de.hpi.swa.graal.squeak.util;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

public final class ArrayConversionUtils {
    public static final int SHORT_BYTE_SIZE = 2;
    public static final int INTEGER_BYTE_SIZE = 4;
    public static final int LONG_BYTE_SIZE = 8;

    public static byte[] bytesFromInts(final int[] ints) {
        final int intsLength = ints.length;
        final byte[] bytes = new byte[intsLength * INTEGER_BYTE_SIZE];
        for (int i = 0; i < intsLength; i++) {
            final int offset = i * INTEGER_BYTE_SIZE;
            final int intValue = ints[i];
            bytes[offset] = (byte) (intValue >> 24);
            bytes[offset + 1] = (byte) (intValue >> 16);
            bytes[offset + 2] = (byte) (intValue >> 8);
            bytes[offset + 3] = (byte) intValue;
        }
        return bytes;
    }

    public static byte[] bytesFromIntsReversed(final int[] ints) {
        final int intsLength = ints.length;
        final byte[] bytes = new byte[intsLength * INTEGER_BYTE_SIZE];
        for (int i = 0; i < intsLength; i++) {
            final int offset = i * INTEGER_BYTE_SIZE;
            final int intValue = ints[i];
            bytes[offset + 3] = (byte) (intValue >> 24);
            bytes[offset + 2] = (byte) (intValue >> 16);
            bytes[offset + 1] = (byte) (intValue >> 8);
            bytes[offset] = (byte) intValue;
        }
        return bytes;
    }

    public static byte[] bytesFromLongs(final long[] longs) {
        final int longsLength = longs.length;
        final byte[] bytes = new byte[longsLength * LONG_BYTE_SIZE];
        for (int i = 0; i < longsLength; i++) {
            final int offset = i * LONG_BYTE_SIZE;
            final long longValue = longs[i];
            bytes[offset] = (byte) (longValue >> 56);
            bytes[offset + 1] = (byte) (longValue >> 48);
            bytes[offset + 2] = (byte) (longValue >> 40);
            bytes[offset + 3] = (byte) (longValue >> 32);
            bytes[offset + 4] = (byte) (longValue >> 24);
            bytes[offset + 5] = (byte) (longValue >> 16);
            bytes[offset + 6] = (byte) (longValue >> 8);
            bytes[offset + 7] = (byte) longValue;
        }
        return bytes;
    }

    public static byte[] bytesFromLongsReversed(final long[] longs) {
        final int longsLength = longs.length;
        final byte[] bytes = new byte[longsLength * LONG_BYTE_SIZE];
        for (int i = 0; i < longsLength; i++) {
            final int offset = i * LONG_BYTE_SIZE;
            final long longValue = longs[i];
            bytes[offset + 7] = (byte) (longValue >> 56);
            bytes[offset + 6] = (byte) (longValue >> 48);
            bytes[offset + 5] = (byte) (longValue >> 40);
            bytes[offset + 4] = (byte) (longValue >> 32);
            bytes[offset + 3] = (byte) (longValue >> 24);
            bytes[offset + 2] = (byte) (longValue >> 16);
            bytes[offset + 1] = (byte) (longValue >> 8);
            bytes[offset + 0] = (byte) longValue;
        }
        return bytes;
    }

    public static byte[] bytesFromShorts(final short[] shorts) {
        final int shortLength = shorts.length;
        final byte[] bytes = new byte[shortLength * SHORT_BYTE_SIZE];
        for (int i = 0; i < shortLength; i++) {
            final int offset = i * SHORT_BYTE_SIZE;
            final short shortValue = shorts[i];
            bytes[offset] = (byte) (shortValue >> 8);
            bytes[offset + 1] = (byte) shortValue;
        }
        return bytes;
    }

    public static byte[] bytesFromShortsReversed(final short[] shorts) {
        final int shortLength = shorts.length;
        final byte[] bytes = new byte[shortLength * SHORT_BYTE_SIZE];
        for (int i = 0; i < shortLength; i++) {
            final int offset = i * SHORT_BYTE_SIZE;
            final short shortValue = shorts[i];
            bytes[offset + 1] = (byte) (shortValue >> 8);
            bytes[offset] = (byte) shortValue;
        }
        return bytes;
    }

    public static int[] bytesToInts(final byte[] bytes) {
        final int length = bytes.length;
        final int[] ints = new int[length];
        for (int i = 0; i < length; i++) {
            ints[i] = bytes[i];
        }
        return ints;
    }

    @TruffleBoundary
    public static String bytesToString(final byte[] bytes) {
        return new String(bytes);
    }

    public static long[] bytesToUnsignedLongs(final byte[] bytes) {
        final int length = bytes.length;
        final long[] longs = new long[length];
        for (int i = 0; i < length; i++) {
            longs[i] = Byte.toUnsignedLong(bytes[i]);
        }
        return longs;
    }

    public static int[] intsFromBytes(final byte[] bytes) {
        final int size = bytes.length / INTEGER_BYTE_SIZE;
        final int[] ints = new int[size];
        for (int i = 0; i < ints.length; i++) {
            final int offset = i * 4;
            ints[i] = (bytes[offset + 0] & 0xFF) << 24 | (bytes[offset + 1] & 0xFF) << 16 | (bytes[offset + 2] & 0xFF) << 8 | bytes[offset + 3] & 0xFF;
        }
        return ints;
    }

    public static int[] intsFromBytesExact(final byte[] bytes) {
        final int byteSize = bytes.length;
        final int size = Math.floorDiv(byteSize + 3, INTEGER_BYTE_SIZE);
        final int[] ints = new int[size];
        for (int i = 0; i < size; i++) {
            final int offset = i * 4;
            if (offset < byteSize) {
                ints[i] |= (bytes[offset] & 0xFF) << 24;
            }
            if (offset + 1 < byteSize) {
                ints[i] |= (bytes[offset + 1] & 0xFF) << 16;
            }
            if (offset + 2 < byteSize) {
                ints[i] |= (bytes[offset + 2] & 0xFF) << 8;
            }
            if (offset + 3 < byteSize) {
                ints[i] |= bytes[offset + 3] & 0xFF;
            }
        }
        return ints;
    }

    public static int[] intsFromBytesReversed(final byte[] bytes) {
        final int size = bytes.length / INTEGER_BYTE_SIZE;
        final int[] ints = new int[size];
        for (int i = 0; i < size; i++) {
            final int offset = i * 4;
            ints[i] = (bytes[offset + 3] & 0xFF) << 24 | (bytes[offset + 2] & 0xFF) << 16 | (bytes[offset + 1] & 0xFF) << 8 | bytes[offset + 0] & 0xFF;
        }
        return ints;
    }

    public static int[] intsFromBytesReversedExact(final byte[] bytes) {
        final int byteSize = bytes.length;
        final int size = Math.floorDiv(byteSize + 3, INTEGER_BYTE_SIZE);
        final int[] ints = new int[size];
        for (int i = 0; i < size; i++) {
            final int offset = i * 4;
            if (offset + 3 < byteSize) {
                ints[i] |= (bytes[offset + 3] & 0xFF) << 24;
            }
            if (offset + 2 < byteSize) {
                ints[i] |= (bytes[offset + 2] & 0xFF) << 16;
            }
            if (offset + 1 < byteSize) {
                ints[i] |= (bytes[offset + 1] & 0xFF) << 8;
            }
            if (offset < byteSize) {
                ints[i] |= bytes[offset + 0] & 0xFF;
            }
        }
        return ints;
    }

    public static long[] intsToUnsignedLongs(final int[] ints) {
        final int length = ints.length;
        final long[] longs = new long[length];
        for (int i = 0; i < length; i++) {
            longs[i] = Integer.toUnsignedLong(ints[i]);
        }
        return longs;
    }

    public static int largeIntegerByteSizeForLong(final long longValue) {
        return LONG_BYTE_SIZE - Long.numberOfLeadingZeros(longValue) / LONG_BYTE_SIZE;
    }

    public static byte[] largeIntegerBytesFromLong(final long longValue) {
        assert longValue != Long.MIN_VALUE : "Cannot convert long to byte[] (Math.abs(Long.MIN_VALUE) overflows).";
        final long longValuePositive = Math.abs(longValue);
        final int numBytes = largeIntegerByteSizeForLong(longValuePositive);
        final byte[] bytes = new byte[numBytes];
        for (int i = 0; i < numBytes; i++) {
            bytes[i] = (byte) (longValuePositive >> LONG_BYTE_SIZE * i);
        }
        return bytes;
    }

    public static long[] longsFromBytes(final byte[] bytes) {
        final int size = bytes.length / LONG_BYTE_SIZE;
        final long[] longs = new long[size];
        for (int i = 0; i < size; i++) {
            final int offset = i * 8;
            longs[i] = (long) (bytes[offset] & 0xFF) << 56 | (long) (bytes[offset + 1] & 0xFF) << 48 | (long) (bytes[offset + 2] & 0xFF) << 40 | (long) (bytes[offset + 3] & 0xFF) << 32 |
                            (long) (bytes[offset + 4] & 0xFF) << 24 | (long) (bytes[offset + 5] & 0xFF) << 16 | (long) (bytes[offset + 6] & 0xFF) << 8 | bytes[offset + 7] & 0xFF;
        }
        return longs;
    }

    public static long[] longsFromBytesReversed(final byte[] bytes) {
        final int size = bytes.length / LONG_BYTE_SIZE;
        final long[] longs = new long[size];
        for (int i = 0; i < size; i++) {
            final int offset = i * 8;
            longs[i] = (long) (bytes[offset + 7] & 0xFF) << 56 | (long) (bytes[offset + 6] & 0xFF) << 48 | (long) (bytes[offset + 5] & 0xFF) << 40 | (long) (bytes[offset + 4] & 0xFF) << 32 |
                            (long) (bytes[offset + 3] & 0xFF) << 24 | (long) (bytes[offset + 2] & 0xFF) << 16 | (long) (bytes[offset + 1] & 0xFF) << 8 | bytes[offset + 0] & 0xFF;
        }
        return longs;
    }

    public static short[] shortsFromBytes(final byte[] bytes) {
        final int size = bytes.length / SHORT_BYTE_SIZE;
        final short[] shorts = new short[size];
        for (int i = 0; i < shorts.length; i++) {
            final int offset = i * 2;
            shorts[i] = (short) ((bytes[offset] & 0xFF) << 8 | bytes[offset + 1] & 0xFF);
        }
        return shorts;
    }

    public static short[] shortsFromBytesReversed(final byte[] bytes) {
        final int size = bytes.length / SHORT_BYTE_SIZE;
        final short[] shorts = new short[size];
        for (int i = 0; i < shorts.length; i++) {
            final int offset = i * 2;
            shorts[i] = (short) ((bytes[offset + 1] & 0xFF) << 8 | bytes[offset] & 0xFF);
        }
        return shorts;
    }

    @TruffleBoundary
    public static byte[] stringToBytes(final String value) {
        return value.getBytes();
    }
}
