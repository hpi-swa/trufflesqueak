/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.trufflesqueak.image.SqueakImageChunk;
import de.hpi.swa.trufflesqueak.image.SqueakImageConstants;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.image.SqueakImageWriter;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.ObjectGraphUtils.ObjectTracer;

public final class LargeIntegerObject extends AbstractSqueakObjectWithClassAndHash {
    private static final BigInteger ONE_SHIFTED_BY_64 = BigInteger.ONE.shiftLeft(64);
    public static final BigInteger LONG_MIN_OVERFLOW_RESULT = BigInteger.valueOf(Long.MIN_VALUE).abs();
    @CompilationFinal(dimensions = 1) private static final byte[] LONG_MIN_OVERFLOW_RESULT_BYTES = toBytes(LONG_MIN_OVERFLOW_RESULT);

    private BigInteger integer;
    private int bitLength;
    private int exposedSize;

    public LargeIntegerObject(final SqueakImageContext image, final BigInteger integer) {
        super(image, integer.signum() >= 0 ? image.largePositiveIntegerClass : image.largeNegativeIntegerClass);
        this.integer = integer;
        bitLength = integer.bitLength();
        exposedSize = calculateExposedSize(integer);
        assert integer.signum() != 0 : "LargePositiveInteger>>isZero returns 'false'";
    }

    public LargeIntegerObject(final SqueakImageContext image, final long header, final ClassObject klass, final byte[] bytes) {
        super(header, klass);
        integer = new BigInteger(isPositive(image) ? 1 : -1, ArrayUtils.swapOrderInPlace(bytes));
        bitLength = integer.bitLength();
        exposedSize = bytes.length;
    }

    @TruffleBoundary
    public LargeIntegerObject(final SqueakImageContext image, final ClassObject klass, final byte[] bytes) {
        super(image, klass);
        integer = new BigInteger(isPositive(image) ? 1 : -1, ArrayUtils.swapOrderInPlace(bytes));
        bitLength = integer.bitLength();
        exposedSize = bytes.length;
    }

    public LargeIntegerObject(final SqueakImageContext image, final ClassObject klass, final int size) {
        super(image, klass);
        integer = BigInteger.ZERO;
        bitLength = 0;
        exposedSize = size;
    }

    private LargeIntegerObject(final LargeIntegerObject original) {
        super(original);
        integer = original.integer;
        bitLength = original.bitLength;
        exposedSize = original.exposedSize;
    }

    private static int calculateExposedSize(final BigInteger integer) {
        return (integer.abs().bitLength() + 7) / 8;
    }

    @Override
    public void fillin(final SqueakImageChunk chunk) {
        // Nothing to do.
    }

    @TruffleBoundary
    public static LargeIntegerObject createLongMinOverflowResult(final SqueakImageContext image) {
        return new LargeIntegerObject(image, LONG_MIN_OVERFLOW_RESULT);
    }

    public static byte[] getLongMinOverflowResultBytes() {
        return LONG_MIN_OVERFLOW_RESULT_BYTES;
    }

    private static byte[] toBytes(final BigInteger bigInteger) {
        final byte[] bigEndianBytes = toBigEndianBytes(bigInteger);
        final byte[] bytes = bigEndianBytes[0] != 0 ? bigEndianBytes : Arrays.copyOfRange(bigEndianBytes, 1, bigEndianBytes.length);
        return ArrayUtils.swapOrderInPlace(bytes);
    }

    @TruffleBoundary
    private static byte[] toBigEndianBytes(final BigInteger bigInteger) {
        return bigInteger.abs().toByteArray();
    }

    public long getNativeAt0(final long index) {
        return absShiftRightIntValue((int) index * Byte.SIZE) & 0xFF;
    }

    @TruffleBoundary
    private long absShiftRightIntValue(final int n) {
        return integer.abs().shiftRight(n).intValue();
    }

    @TruffleBoundary
    public void setNativeAt0(final SqueakImageContext image, final long index, final long value) {
        assert index < size() : "Illegal index: " + index;
        assert 0 <= value && value <= NativeObject.BYTE_MAX : "Illegal value for LargeIntegerObject: " + value;
        final byte[] bytes;
        final byte[] bigIntegerBytes = toBigEndianBytes(integer);
        final int offset = bigIntegerBytes[0] != 0 ? 0 : 1;
        final int bigIntegerBytesActualLength = bigIntegerBytes.length - offset;
        if (bigIntegerBytesActualLength <= index) {
            final int newLength = Math.min(size(), (int) index + 1);
            bytes = new byte[newLength];
            System.arraycopy(bigIntegerBytes, offset, bytes, newLength - bigIntegerBytesActualLength, bigIntegerBytesActualLength);
        } else {
            bytes = bigIntegerBytes;
        }
        bytes[bytes.length - 1 - (int) index] = (byte) value;
        integer = new BigInteger(isPositive(image) ? 1 : -1, bytes);
        bitLength = integer.bitLength();
    }

    @TruffleBoundary
    public byte[] getBytes() {
        return toBytes(integer);
    }

    @TruffleBoundary
    public void replaceInternalValue(final LargeIntegerObject other) {
        assert size() == other.size();
        integer = other.getSqueakClass() == getSqueakClass() ? other.integer : other.integer.negate();
        bitLength = integer.bitLength();
    }

    @TruffleBoundary
    public void setBytes(final SqueakImageContext image, final byte[] bytes) {
        assert size() == bytes.length;
        integer = new BigInteger(isPositive(image) ? 1 : -1, ArrayUtils.swapOrderCopy(bytes));
        bitLength = integer.bitLength();
    }

    @TruffleBoundary
    public void setBytes(final SqueakImageContext image, final LargeIntegerObject src, final int srcPos, final int destPos, final int length) {
        final byte[] srcBytes = toBigEndianBytes(src.integer);
        final byte[] bytes = getBigIntegerBytes(destPos, length);
        System.arraycopy(srcBytes, srcBytes.length - length - srcPos, bytes, bytes.length - length - destPos, length);
        integer = new BigInteger(isPositive(image) ? 1 : -1, bytes);
        bitLength = integer.bitLength();
    }

    @TruffleBoundary
    public void setBytes(final SqueakImageContext image, final byte[] srcBytes, final int srcPos, final int destPos, final int length) {
        final byte[] bytes = getBigIntegerBytes(destPos, length);
        // destination bytes are big-endian, source bytes are not
        for (int i = 0; i < length; i++) {
            bytes[bytes.length - 1 - (destPos + i)] = srcBytes[srcPos + i];
        }
        integer = new BigInteger(isPositive(image) ? 1 : -1, bytes);
        bitLength = integer.bitLength();
    }

    private byte[] getBigIntegerBytes(final int destPos, final int length) {
        final byte[] bigIntegerBytes = toBigEndianBytes(integer);
        final int offset = bigIntegerBytes[0] != 0 ? 0 : 1;
        final int bigIntegerBytesActualLength = bigIntegerBytes.length - offset;
        final byte[] bytes;
        if (bigIntegerBytesActualLength < destPos + length) {
            bytes = new byte[size()];
            System.arraycopy(bigIntegerBytes, offset, bytes, 0, bigIntegerBytesActualLength);
        } else {
            bytes = bigIntegerBytes;
        }
        return bytes;
    }

    @Override
    public int getNumSlots() {
        return (int) Math.ceil((double) exposedSize / 8);
    }

    @Override
    public int instsize() {
        return 0;
    }

    @Override
    public int size() {
        return exposedSize;
    }

    @Override
    @TruffleBoundary(transferToInterpreterOnException = false)
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        assert bitLength == integer.bitLength();
        if (bitLength < Long.SIZE) {
            return integer.longValue() + " - non-normalized " + getSqueakClass() + " of size " + exposedSize;
        } else if (exposedSize != calculateExposedSize(integer)) {
            return integer + " - non-normalized " + getSqueakClass() + " of size " + exposedSize;
        }
        return integer.toString();
    }

    public boolean equals(final LargeIntegerObject other) {
        return integer.equals(other.integer);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof final LargeIntegerObject o) {
            return equals(o);
        } else {
            return super.equals(other);
        }
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public void pointersBecomeOneWay(final boolean currentMarkingFlag, final Object[] from, final Object[] to) {
        // Nothing to do.
    }

    @Override
    public void tracePointers(final ObjectTracer objectTracer) {
        // Nothing to trace.
    }

    @Override
    public void write(final SqueakImageWriter writer) {
        final int formatOffset = getNumSlots() * SqueakImageConstants.WORD_SIZE - size();
        assert 0 <= formatOffset && formatOffset <= 7 : "too many odd bits (see instSpec)";
        if (writeHeader(writer, formatOffset)) {
            final byte[] bytes = getBytes();
            writer.writeBytes(bytes);
            final int offset = bytes.length % SqueakImageConstants.WORD_SIZE;
            if (offset > 0) {
                writer.writePadding(SqueakImageConstants.WORD_SIZE - offset);
            }
        }
    }

    public LargeIntegerObject shallowCopy() {
        return new LargeIntegerObject(this);
    }

    private Object reduceIfPossible(final BigInteger value) {
        return reduceIfPossible(getSqueakClass().getImage(), value);
    }

    private static Object reduceIfPossible(final SqueakImageContext image, final BigInteger value) {
        if (bitLength(value) < Long.SIZE) {
            return value.longValue();
        } else {
            return new LargeIntegerObject(image, value);
        }
    }

    @TruffleBoundary
    public Object reduceIfPossible() {
        if (bitLength < Long.SIZE) {
            return integer.longValue();
        } else {
            exposedSize = calculateExposedSize(integer);
            return this;
        }
    }

    @TruffleBoundary
    public long longValue() {
        return integer.longValue();
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public long longValueExact() throws ArithmeticException {
        return integer.longValueExact();
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public int intValueExact() throws ArithmeticException {
        return integer.intValueExact();
    }

    public boolean fitsIntoLong() {
        return bitLength < Long.SIZE;
    }

    public int bitLength() {
        return bitLength;
    }

    @TruffleBoundary
    private static int bitLength(final BigInteger integer) {
        return integer.bitLength();
    }

    private boolean isPositive(final SqueakImageContext image) {
        return getSqueakClass() == image.largePositiveIntegerClass;
    }

    public boolean isNegative(final SqueakImageContext image) {
        return getSqueakClass() == image.largeNegativeIntegerClass;
    }

    /*
     * Arithmetic Operations
     */

    // TODO: Find out when reduceIfPossible is really necessary
    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object add(final LargeIntegerObject b) {
        return reduceIfPossible(integer.add(b.integer));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object add(final long b) {
        return reduceIfPossible(integer.add(BigInteger.valueOf(b)));
    }

    public static Object add(final SqueakImageContext image, final long lhs, final long rhs) {
        /* Inlined version of Math.addExact(x, y) with large integer fallback. */
        final long result = lhs + rhs;
        // HD 2-12 Overflow iff both arguments have the opposite sign of the result
        if (((lhs ^ result) & (rhs ^ result)) < 0) {
            return addLarge(image, lhs, rhs);
        }
        return result;
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    private static LargeIntegerObject addLarge(final SqueakImageContext image, final long lhs, final long rhs) {
        return new LargeIntegerObject(image, BigInteger.valueOf(lhs).add(BigInteger.valueOf(rhs)));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object subtract(final LargeIntegerObject b) {
        return reduceIfPossible(integer.subtract(b.integer));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object subtract(final long b) {
        return reduceIfPossible(integer.subtract(BigInteger.valueOf(b)));
    }

    public static Object subtract(final SqueakImageContext image, final long lhs, final long rhs) {
        /* Inlined version of Math.subtractExact(x, y) with large integer fallback. */
        final long result = lhs - rhs;
        // HD 2-12 Overflow iff the arguments have different signs and
        // the sign of the result is different than the sign of x
        if (((lhs ^ rhs) & (lhs ^ result)) < 0) {
            return subtractLarge(image, lhs, rhs);
        }
        return result;
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    private static LargeIntegerObject subtractLarge(final SqueakImageContext image, final long lhs, final long rhs) {
        return new LargeIntegerObject(image, BigInteger.valueOf(lhs).subtract(BigInteger.valueOf(rhs)));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static Object subtract(final long a, final LargeIntegerObject b) {
        return b.reduceIfPossible(BigInteger.valueOf(a).subtract(b.integer));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object multiply(final LargeIntegerObject b) {
        return reduceIfPossible(integer.multiply(b.integer));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object multiply(final long b) {
        if (b == 0) {
            return 0L;
        }
        return reduceIfPossible(integer.multiply(BigInteger.valueOf(b)));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static Object multiply(final SqueakImageContext image, final long lhs, final long rhs) {
        /* Inlined version of Math.multiplyExact(x, y) with large integer fallback. */
        final long result = lhs * rhs;
        final long ax = Math.abs(lhs);
        final long ay = Math.abs(rhs);
        if ((ax | ay) >>> 31 != 0) {
            // Some bits greater than 2^31 that might cause overflow
            // Check the result using the divide operator
            // and check for the special case of Long.MIN_VALUE * -1
            if (rhs != 0 && result / rhs != lhs || lhs == Long.MIN_VALUE && rhs == -1) {
                return new LargeIntegerObject(image, BigInteger.valueOf(lhs).multiply(BigInteger.valueOf(rhs)));
            }
        }
        return result;
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object divide(final LargeIntegerObject b) {
        return reduceIfPossible(integer.divide(b.integer));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object divide(final long b) {
        return reduceIfPossible(integer.divide(BigInteger.valueOf(b)));
    }

    public static long divide(@SuppressWarnings("unused") final long a, final LargeIntegerObject b) {
        assert !b.fitsIntoLong() : "non-reduced large integer!";
        return 0L;
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object floorDivide(final LargeIntegerObject b) {
        return reduceIfPossible(floorDivide(integer, b.integer));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object floorDivide(final long b) {
        return reduceIfPossible(floorDivide(integer, BigInteger.valueOf(b)));
    }

    public static long floorDivide(final long a, final LargeIntegerObject b) {
        assert !b.fitsIntoLong() : "non-reduced large integer!";
        if (a != 0 && a < 0 ^ b.integer.signum() < 0) {
            return -1L;
        } else {
            return 0L;
        }
    }

    private static BigInteger floorDivide(final BigInteger x, final BigInteger y) {
        final BigInteger[] r = x.divideAndRemainder(y);
        // if the signs are different and modulo not zero, round down
        if (x.signum() != y.signum() && r[1].signum() != 0) {
            return r[0].subtract(BigInteger.ONE);
        } else {
            return r[0];
        }
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object floorMod(final LargeIntegerObject b) {
        return floorMod(integer, b.integer);
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object floorMod(final long b) {
        return floorMod(integer, BigInteger.valueOf(b));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object floorModReverseOrder(final long a) {
        return floorMod(BigInteger.valueOf(a), integer);
    }

    private Object floorMod(final BigInteger a, final BigInteger b) {
        return reduceIfPossible(a.subtract(floorDivide(a, b).multiply(b)));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public long remainder(final long other) {
        return integer.remainder(BigInteger.valueOf(other)).longValue();
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object remainder(final LargeIntegerObject b) {
        return reduceIfPossible(integer.remainder(b.integer));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public int compareTo(final LargeIntegerObject b) {
        return integer.compareTo(b.integer);
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public int compareTo(final long b) {
        if (bitLength < Long.SIZE) {
            return Long.compare(integer.longValue(), b);
        } else {
            return integer.signum();
        }
    }

    /** {@link BigInteger#signum()} does not need a {@link TruffleBoundary}. */
    public boolean isZero() {
        return integer.signum() == 0;
    }

    /** {@link BigInteger#signum()} does not need a {@link TruffleBoundary}. */
    public boolean isZeroOrPositive() {
        return integer.signum() >= 0;
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public boolean lessThanOrEqualTo(final long value) {
        if (bitLength < Long.SIZE) {
            return integer.longValue() <= value;
        } else {
            return integer.signum() < 0;
        }
    }

    public boolean lessThanOneShiftedBy64() {
        return bitLength < Long.SIZE + 1;
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public boolean inRange(final long minValue, final long maxValue) {
        if (bitLength < Long.SIZE) {
            final long longValueExact = integer.longValue();
            return minValue <= longValueExact && longValueExact <= maxValue;
        }
        return false;
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public boolean isIntegralWhenDividedBy(final LargeIntegerObject other) {
        return integer.remainder(other.integer).signum() == 0;
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public boolean isIntegralWhenDividedBy(final long other) {
        return integer.remainder(BigInteger.valueOf(other)).signum() == 0;
    }

    public boolean sameSign(final LargeIntegerObject other) {
        return getSqueakClass() == other.getSqueakClass();
    }

    public boolean differentSign(final SqueakImageContext image, final long other) {
        return isNegative(image) ^ other < 0;
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public long toSignedLong() {
        assert isPositive(SqueakImageContext.getSlow()) && bitLength <= Long.SIZE;
        if (bitLength == Long.SIZE) {
            return integer.subtract(ONE_SHIFTED_BY_64).longValue();
        } else {
            return integer.longValue();
        }
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static LargeIntegerObject toUnsigned(final SqueakImageContext image, final long value) {
        assert value < 0;
        return new LargeIntegerObject(image, BigInteger.valueOf(value).add(ONE_SHIFTED_BY_64));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static Object truncateExact(final SqueakImageContext image, final double value) {
        return reduceIfPossible(image, new BigDecimal(value).toBigInteger());
    }

    /*
     * Bit Operations
     */

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object and(final LargeIntegerObject b) {
        return reduceIfPossible(integer.and(b.integer));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object and(final long b) {
        return reduceIfPossible(integer.and(BigInteger.valueOf(b)));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object or(final LargeIntegerObject b) {
        return reduceIfPossible(integer.or(b.integer));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object or(final long b) {
        return reduceIfPossible(integer.or(BigInteger.valueOf(b)));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object xor(final LargeIntegerObject b) {
        return reduceIfPossible(integer.xor(b.integer));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object xor(final long b) {
        return reduceIfPossible(integer.xor(BigInteger.valueOf(b)));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object shiftLeft(final int b) {
        if (integer.signum() < 0 && b < 0) {
            return reduceIfPossible(integer.abs().shiftLeft(b).negate());
        }
        return reduceIfPossible(integer.shiftLeft(b));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static Object shiftLeftPositive(final SqueakImageContext image, final long a, final int b) {
        assert b >= 0 : "This method must be used with a positive 'b' argument";
        return reduceIfPossible(image, BigInteger.valueOf(a).shiftLeft(b));
    }

    public BigInteger getBigInteger() {
        return integer;
    }
}
