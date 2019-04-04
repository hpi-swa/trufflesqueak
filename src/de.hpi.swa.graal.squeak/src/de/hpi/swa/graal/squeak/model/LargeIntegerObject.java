package de.hpi.swa.graal.squeak.model;

import java.math.BigInteger;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.util.ArrayUtils;

public final class LargeIntegerObject extends AbstractSqueakObject {

    public static final long SMALLINTEGER32_MIN = -0x40000000;
    public static final long SMALLINTEGER32_MAX = 0x3fffffff;
    public static final long SMALLINTEGER64_MIN = -0x1000000000000000L;
    public static final long SMALLINTEGER64_MAX = 0xfffffffffffffffL;
    public static final long MASK_32BIT = 0xffffffffL;
    public static final long MASK_64BIT = 0xffffffffffffffffL;
    private static final BigInteger ONE_HUNDRED_TWENTY_EIGHT = BigInteger.valueOf(128);
    private static final BigInteger LONG_MIN_OVERFLOW_RESULT = BigInteger.valueOf(Long.MIN_VALUE).abs();
    private static final BigInteger ONE_SHIFTED_BY_64 = BigInteger.ONE.shiftLeft(64);

    private BigInteger integer;

    private int exposedSize;

    public LargeIntegerObject(final SqueakImageContext image, final BigInteger integer) {
        super(image, integer.compareTo(BigInteger.valueOf(0)) >= 0 ? image.largePositiveIntegerClass : image.largeNegativeIntegerClass);
        this.integer = integer;
        exposedSize = byteLength(integer);
    }

    public LargeIntegerObject(final SqueakImageContext image, final long hash, final ClassObject klass, final byte[] bytes) {
        super(image, hash, klass);
        exposedSize = bytes.length;
        this.integer = new BigInteger(klass == image.largeNegativeIntegerClass ? -1 : 1, ArrayUtils.swapOrderCopy(bytes));
    }

    public LargeIntegerObject(final SqueakImageContext image, final ClassObject klass, final byte[] bytes) {
        super(image, klass);
        exposedSize = bytes.length;
        this.integer = new BigInteger(klass == image.largeNegativeIntegerClass ? -1 : 1, ArrayUtils.swapOrderCopy(bytes));
    }

    public LargeIntegerObject(final SqueakImageContext image, final ClassObject klass, final int size) {
        super(image, klass);
        this.integer = BigInteger.ZERO;
        exposedSize = size;
    }

    public LargeIntegerObject(final LargeIntegerObject original) {
        super(original.image, original.getSqueakClass());
        this.integer = original.integer;
        exposedSize = original.exposedSize;
    }

    public LargeIntegerObject(final ClassObject klass, final LargeIntegerObject original) {
        super(original.image, klass);
        this.integer = klass == image.largeNegativeIntegerClass ? original.integer.negate() : original.integer;
        exposedSize = original.exposedSize;
    }

    public static LargeIntegerObject createLongMinOverflowResult(final SqueakImageContext image) {
        return new LargeIntegerObject(image, LONG_MIN_OVERFLOW_RESULT);
    }

    public static byte[] getLongMinOverflowResultBytes() {
        return ArrayUtils.swapOrderCopy(toByteArray(LONG_MIN_OVERFLOW_RESULT));
    }

    public long getNativeAt0(final long index) {
        // TODO: File issue with toByteArray not working on negative numbers?
        final byte[] bytes = toByteArray();
        if (exposedSize - (int) index > bytes.length) {
            return 0L;
        }
        return Byte.toUnsignedLong(bytes[Math.max(bytes.length, exposedSize) - 1 - (int) index]);
    }

    public void setNativeAt0(final long index, final long value) {
        if (value < 0 || value > NativeObject.BYTE_MAX) { // check for overflow
            throw new IllegalArgumentException("Illegal value for LargeIntegerObject: " + value);
        }
        // Bytes are big-endian
        final byte[] bytes;
        final byte[] bigIntegerBytes = toByteArray();
        if (bigIntegerBytes.length <= index) {
            bytes = new byte[Math.min(exposedSize, (int) index + 1)];
            System.arraycopy(bigIntegerBytes, 0, bytes, bytes.length - bigIntegerBytes.length, bigIntegerBytes.length);
        } else {
            bytes = bigIntegerBytes;
        }
        bytes[bytes.length - 1 - (int) index] = (byte) value;
        this.integer = new BigInteger(this.getSqueakClass() == image.largeNegativeIntegerClass ? -1 : 1, bytes);
    }

    public byte[] getBytes() {
        return ArrayUtils.swapOrderCopy(toByteArray());
    }

    public void replaceInternalValue(final long original) {
        assert exposedSize == Long.BYTES;
        if (original == Long.MIN_VALUE) {
            integer = this.getSqueakClass() == image.largeNegativeIntegerClass ? BigInteger.valueOf(original) : BigInteger.valueOf(original).negate();
        } else {
            integer = this.getSqueakClass() == image.largeNegativeIntegerClass ^ original < 0 ? BigInteger.valueOf(-original) : BigInteger.valueOf(original);
        }
    }

    public void replaceInternalValue(final LargeIntegerObject original) {
        assert exposedSize == original.exposedSize;
        integer = original.getSqueakClass() == this.getSqueakClass() ? original.integer : original.integer.negate();
    }

    public void replaceInternalValue(final byte[] bytes) {
        assert exposedSize == bytes.length;
        integer = new BigInteger(this.getSqueakClass() == image.largeNegativeIntegerClass ? -1 : 1, ArrayUtils.swapOrderCopy(bytes));
    }

    public void setBytes(final byte[] bytes) {
        assert exposedSize == bytes.length;
        this.integer = new BigInteger(this.getSqueakClass() == image.largeNegativeIntegerClass ? -1 : 1, ArrayUtils.swapOrderCopy(bytes));
    }

    public void setBytes(final LargeIntegerObject src, final int srcPos, final int destPos, final int length) {
        // destination bytes are big-endian, source bytes are not
        final byte[] bytes;
        final byte[] srcBytes = src.toByteArray();
        final byte[] bigIntegerBytes = toByteArray();
        if (bigIntegerBytes.length < destPos + length) {
            bytes = new byte[exposedSize];
            System.arraycopy(bigIntegerBytes, 0, bytes, 0, bigIntegerBytes.length);
        } else {
            bytes = bigIntegerBytes;
        }
        for (int i = 0; i < length; i++) {
            bytes[bytes.length - 1 - (destPos + i)] = srcBytes[srcBytes.length - 1 - (srcPos + i)];
        }
        this.integer = new BigInteger(this.getSqueakClass() == image.largeNegativeIntegerClass ? -1 : 1, bytes);
    }

    public void setBytes(final byte[] srcBytes, final int srcPos, final int destPos, final int length) {
        // destination bytes are big-endian, source bytes are not
        final byte[] bytes;
        final byte[] bigIntegerBytes = toByteArray();
        if (bigIntegerBytes.length < destPos + length) {
            bytes = new byte[exposedSize];
            System.arraycopy(bigIntegerBytes, 0, bytes, 0, bigIntegerBytes.length);
        } else {
            bytes = bigIntegerBytes;
        }
        for (int i = 0; i < length; i++) {
            bytes[destPos + i] = srcBytes[srcBytes.length - 1 - (srcPos + i)];
        }
        this.integer = new BigInteger(this.getSqueakClass() == image.largeNegativeIntegerClass ? -1 : 1, bytes);
    }

    public static byte[] toByteArray(final BigInteger integer) {
        // BitInteger.toByteArray can contain leading zeroes (big endian)
        final byte[] result = new byte[byteLength(integer)];
        final byte[] leadingZeroes = integer.signum() == -1 ? integer.negate().toByteArray() : integer.toByteArray();
        System.arraycopy(leadingZeroes, leadingZeroes.length - result.length, result, 0, result.length);
        return result;
    }

    public byte[] toByteArray() {
        // BitInteger.toByteArray can contain leading zeroes (big endian)
        final byte[] result = new byte[byteLength(integer)];
        final byte[] leadingZeroes = this.getSqueakClass() == image.largeNegativeIntegerClass ? integer.negate().toByteArray() : integer.toByteArray();
        System.arraycopy(leadingZeroes, leadingZeroes.length - result.length, result, 0, result.length);
        return result;
    }

    public static int byteLength(final BigInteger integer) {
        // TODO: BigInteger.valueOf(-1).bitLength() == 0 ?!
        final int bitLength;
        if (integer.signum() == -1) {
            bitLength = integer.negate().bitLength();
        } else {
            bitLength = integer.bitLength();
        }
        return bitLength / 8 + (bitLength % 8 == 0 ? 0 : 1);
    }

    @Override
    public int instsize() {
        return 0;
    }

    @Override
    public int size() {
        return exposedSize;
    }

    public void setInteger(final LargeIntegerObject other) {
        this.integer = other.integer;
    }

    @Override
    @TruffleBoundary(transferToInterpreterOnException = false)
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return integer.toString();
    }

    public boolean hasSameValueAs(final LargeIntegerObject other) {
        return integer.equals(other.integer);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof LargeIntegerObject) {
            return hasSameValueAs((LargeIntegerObject) other);
        } else {
            return super.equals(other);
        }
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public AbstractSqueakObject shallowCopy() {
        return new LargeIntegerObject(this);
    }

    public Object reduceIfPossible(final BigInteger value) {
        return LargeIntegerObject.reduceIfPossible(image, value);
    }

    public static Object reduceIfPossible(final SqueakImageContext image, final BigInteger value) {
        if (value.bitLength() > Long.SIZE - 1) {
            return newFromBigInteger(image, value);
        } else {
            return value.longValue() & MASK_64BIT;
        }
    }

    public Object reduceIfPossible() {
        return reduceIfPossible(this.integer);
    }

    public long longValue() {
        return integer.longValue();
    }

    public long longValueExact() throws ArithmeticException {
        return integer.longValueExact(); // TODO: exact
    }

    public int intValueExact() throws ArithmeticException {
        return integer.intValueExact(); // TODO: exact
    }

    public boolean fitsIntoLong() {
        return bitLength() < Long.SIZE;
    }

    public boolean fitsIntoInt() {
        return bitLength() < Integer.SIZE;
    }

    public int bitLength() {
        return integer.bitLength();
    }

    private static LargeIntegerObject newFromBigInteger(final SqueakImageContext image, final BigInteger value) {
        return new LargeIntegerObject(image, value);
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static LargeIntegerObject valueOf(final SqueakImageContext image, final long a) {
        return newFromBigInteger(image, BigInteger.valueOf(a));
    }

    public boolean isPositive() {
        return getSqueakClass() == image.largePositiveIntegerClass;
    }

    public boolean sizeLessThanWordSize() {
        /**
         * See `InterpreterPrimitives>>#positiveMachineIntegerValueOf:`.
         */
        return size() <= image.flags.wordSize();
    }

    /*
     * Arithmetic Operations
     */

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object add(final LargeIntegerObject b) {
        return reduceIfPossible(integer.add(b.integer));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object add(final long b) {
        return reduceIfPossible(integer.add(BigInteger.valueOf(b)));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static Object add(final SqueakImageContext image, final long a, final long b) {
        return reduceIfPossible(image, BigInteger.valueOf(a).add(BigInteger.valueOf(b)));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object subtract(final LargeIntegerObject b) {
        return reduceIfPossible(integer.subtract(b.integer));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object multiply(final LargeIntegerObject b) {
        return reduceIfPossible(integer.multiply(b.integer));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object divide(final LargeIntegerObject b) {
        return reduceIfPossible(integer.divide(b.integer));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object floorDivide(final LargeIntegerObject b) {
        return reduceIfPossible(floorDivide(integer, b.integer));
    }

    private static BigInteger floorDivide(final BigInteger x, final BigInteger y) {
        BigInteger r = x.divide(y);
        // if the signs are different and modulo not zero, round down
        if (x.signum() != y.signum() && !r.multiply(y).equals(x)) {
            r = r.subtract(BigInteger.ONE);
        }
        return r;
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object floorMod(final LargeIntegerObject b) {
        return reduceIfPossible(integer.subtract(floorDivide(integer, b.integer).multiply(b.integer)));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public LargeIntegerObject divideNoReduce(final LargeIntegerObject b) {
        return newFromBigInteger(image, integer.divide(b.integer));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object remainder(final LargeIntegerObject b) {
        return reduceIfPossible(integer.remainder(b.integer));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public LargeIntegerObject negateNoReduce() {
        return newFromBigInteger(image, integer.negate());
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public int compareTo(final LargeIntegerObject b) {
        return integer.compareTo(b.integer);
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public int compareTo(final BigInteger b) {
        return integer.compareTo(b);
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public double doubleValue() {
        return integer.doubleValue();
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public boolean isZero() {
        return integer.equals(BigInteger.ZERO);
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public boolean isZeroOrPositive() {
        return integer.compareTo(BigInteger.ZERO) >= 0;
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public boolean lessThanOrEqualTo(final long value) {
        return fitsIntoLong() && integer.longValue() <= value; // TODO exact?
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public boolean lessThanOneShiftedBy64() {
        return integer.compareTo(ONE_SHIFTED_BY_64) < 0;
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public boolean inRange(final long minValue, final long maxValue) {
        final long longValueExact = integer.longValue(); // TODO exact
        return minValue <= longValueExact && longValueExact <= maxValue;
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public boolean isIntegralWhenDividedBy(final LargeIntegerObject other) {
        return integer.remainder(other.integer).compareTo(BigInteger.ZERO) == 0;
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public LargeIntegerObject toSigned() {
        if (integer.shiftRight(56).compareTo(ONE_HUNDRED_TWENTY_EIGHT) >= 0) {
            return newFromBigInteger(image, integer.subtract(ONE_SHIFTED_BY_64));
        } else {
            return this;
        }
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public LargeIntegerObject toUnsigned() {
        if (integer.compareTo(BigInteger.ZERO) < 0) {
            return newFromBigInteger(image, integer.add(ONE_SHIFTED_BY_64));
        } else {
            return this;
        }
    }

    /*
     * Bit Operations
     */

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object and(final LargeIntegerObject b) {
        return reduceIfPossible(integer.and(b.integer));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object or(final LargeIntegerObject b) {
        return reduceIfPossible(integer.or(b.integer));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object xor(final LargeIntegerObject b) {
        return reduceIfPossible(integer.xor(b.integer));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object shiftLeft(final int b) {
        return reduceIfPossible(integer.shiftLeft(b));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object shiftRight(final int b) {
        return reduceIfPossible(integer.shiftRight(b));
    }

    public boolean isNegative() {
        return this.integer.signum() == -1;
    }

    public BigInteger getBigInteger() {
        return integer;
    }
}
