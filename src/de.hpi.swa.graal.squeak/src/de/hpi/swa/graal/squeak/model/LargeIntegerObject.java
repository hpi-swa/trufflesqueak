package de.hpi.swa.graal.squeak.model;

import java.math.BigInteger;
import java.util.Arrays;

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
    private static final BigInteger ONE_SHIFTED_BY_64 = BigInteger.ONE.shiftLeft(64);
    private static final BigInteger ONE_HUNDRED_TWENTY_EIGHT = BigInteger.valueOf(128);
    private static final BigInteger LONG_MIN_OVERFLOW_RESULT = BigInteger.valueOf(Long.MIN_VALUE).abs();

    private BigInteger integer;

    private int exposedSize;

    public LargeIntegerObject(final SqueakImageContext image, final BigInteger integer) {
        super(image, integer.compareTo(BigInteger.ZERO) >= 0 ? image.largePositiveIntegerClass : image.largeNegativeIntegerClass);
        this.integer = integer;
        this.exposedSize = byteLength(integer);
    }

    public LargeIntegerObject(final SqueakImageContext image, final long hash, final ClassObject klass, final byte[] bytes) {
        super(image, hash, klass);
        this.exposedSize = bytes.length;
        this.integer = new BigInteger(klass == image.largeNegativeIntegerClass ? -1 : 1, ArrayUtils.swapOrderCopy(bytes));
    }

    public LargeIntegerObject(final SqueakImageContext image, final ClassObject klass, final byte[] bytes) {
        super(image, klass);
        this.exposedSize = bytes.length;
        this.integer = new BigInteger(klass == image.largeNegativeIntegerClass ? -1 : 1, ArrayUtils.swapOrderCopy(bytes));
    }

    public LargeIntegerObject(final SqueakImageContext image, final ClassObject klass, final int size) {
        super(image, klass);
        this.exposedSize = size;
        this.integer = BigInteger.ZERO;
    }

    public LargeIntegerObject(final LargeIntegerObject original) {
        super(original.image, original.getSqueakClass());
        this.exposedSize = original.exposedSize;
        this.integer = original.integer;
    }

    public static LargeIntegerObject createLongMinOverflowResult(final SqueakImageContext image) {
        return new LargeIntegerObject(image, LONG_MIN_OVERFLOW_RESULT);
    }

    public static byte[] getLongMinOverflowResultBytes() {
        return bigIntegerToBytes(LONG_MIN_OVERFLOW_RESULT);
    }

    public byte[] bigIntegerToBytes() {
        return bigIntegerToBytes(this.integer);
    }

    public static byte[] bigIntegerToBytes(final BigInteger bigInteger) {
        final byte[] bytes = bigInteger.abs().toByteArray();
        if (bytes[0] == 0) {
            return ArrayUtils.swapOrderInPlace(Arrays.copyOfRange(bytes, bytes.length - byteLength(bigInteger), byteLength(bigInteger)));
        } else {
            return ArrayUtils.swapOrderInPlace(bytes);
        }
    }

    public byte[] bigIntegerToBigEndianBytes() {
        return bigIntegerToBigEndianBytes(this.integer);
    }

    public static byte[] bigIntegerToBigEndianBytes(final BigInteger bigInteger) {
        final byte[] bytes = bigInteger.abs().toByteArray();
        return bytes[0] != 0 ? bytes : Arrays.copyOfRange(bytes, bytes.length - byteLength(bigInteger), byteLength(bigInteger));
    }

    public long getNativeAt0(final long index) {
        // TODO: File issue with toByteArray not working on negative numbers?
        final byte[] bytes = this.bigIntegerToBigEndianBytes();
        if (this.exposedSize - (int) index > bytes.length) {
            return 0L;
        }
        return Byte.toUnsignedLong(bytes[Math.max(bytes.length, this.exposedSize) - 1 - (int) index]);
    }

    public void setNativeAt0(final long index, final long value) {
        if (value < 0 || value > NativeObject.BYTE_MAX) { // check for overflow
            throw new IllegalArgumentException("Illegal value for LargeIntegerObject: " + value);
        }
        final byte[] bytes;
        final byte[] bigIntegerBytes = this.bigIntegerToBigEndianBytes();
        if (bigIntegerBytes.length <= index) {
            bytes = new byte[Math.min(this.exposedSize, (int) index + 1)];
            System.arraycopy(bigIntegerBytes, 0, bytes, bytes.length - bigIntegerBytes.length, bigIntegerBytes.length);
        } else {
            bytes = bigIntegerBytes;
        }
        bytes[bytes.length - 1 - (int) index] = (byte) value;
        this.integer = new BigInteger(this.getSqueakClass() == this.image.largeNegativeIntegerClass ? -1 : 1, bytes);
    }

    public byte[] getBytes() {
        return this.bigIntegerToBytes();
    }

    public void replaceInternalValue(final long original) {
        this.integer = this.getSqueakClass() == this.image.largeNegativeIntegerClass ^ original < 0 ? BigInteger.valueOf(-original) : BigInteger.valueOf(original);
    }

    public void replaceInternalMinValue() {
        this.integer = this.getSqueakClass() == this.image.largeNegativeIntegerClass ? LONG_MIN_OVERFLOW_RESULT.negate() : LONG_MIN_OVERFLOW_RESULT;
    }

    public void replaceInternalValue(final LargeIntegerObject original) {
        assert this.exposedSize == original.exposedSize;
        this.integer = original.getSqueakClass() == this.getSqueakClass() ? original.integer : original.integer.negate();
    }

    public void replaceInternalValue(final byte[] bytes) {
        assert this.exposedSize == bytes.length;
        this.integer = new BigInteger(this.getSqueakClass() == this.image.largeNegativeIntegerClass ? -1 : 1, ArrayUtils.swapOrderCopy(bytes));
    }

    public void setBytes(final byte[] bytes) {
        assert this.exposedSize == bytes.length;
        this.integer = new BigInteger(this.getSqueakClass() == this.image.largeNegativeIntegerClass ? -1 : 1, ArrayUtils.swapOrderCopy(bytes));
    }

    public void setBytes(final LargeIntegerObject src, final int srcPos, final int destPos, final int length) {
        final byte[] bytes;
        final byte[] srcBytes = src.bigIntegerToBigEndianBytes();
        final byte[] bigIntegerBytes = this.bigIntegerToBigEndianBytes();
        if (bigIntegerBytes.length < destPos + length) {
            bytes = new byte[this.exposedSize];
            System.arraycopy(bigIntegerBytes, 0, bytes, 0, bigIntegerBytes.length);
        } else {
            bytes = bigIntegerBytes;
        }
        for (int i = 0; i < length; i++) {
            bytes[bytes.length - 1 - (destPos + i)] = srcBytes[srcBytes.length - 1 - (srcPos + i)];
        }
        this.integer = new BigInteger(this.getSqueakClass() == this.image.largeNegativeIntegerClass ? -1 : 1, bytes);
    }

    public void setBytes(final byte[] srcBytes, final int srcPos, final int destPos, final int length) {
        // destination bytes are big-endian, source bytes are not
        final byte[] bytes;
        final byte[] bigIntegerBytes = this.bigIntegerToBigEndianBytes();
        if (bigIntegerBytes.length < destPos + length) {
            bytes = new byte[this.exposedSize];
            System.arraycopy(bigIntegerBytes, 0, bytes, 0, bigIntegerBytes.length);
        } else {
            bytes = bigIntegerBytes;
        }
        for (int i = 0; i < length; i++) {
            bytes[bytes.length - 1 - (destPos + i)] = srcBytes[srcPos + i];
        }
        this.integer = new BigInteger(this.getSqueakClass() == this.image.largeNegativeIntegerClass ? -1 : 1, bytes);
    }

    public static int byteLength(final BigInteger integer) {
        // TODO: BigInteger.valueOf(-1).bitLength() == 0 ?!
        // TODO: BigInteger.valueOf(0).bitLength() == 0 ?!
        final int bitLength;
        if (integer.signum() == -1) {
            bitLength = integer.negate().bitLength();
        } else if (integer.signum() == 1) {
            bitLength = integer.bitLength();
        } else {
            return 1;
        }

        return bitLength / 8 + (bitLength % 8 == 0 ? 0 : 1);
    }

    @Override
    public int instsize() {
        return 0;
    }

    @Override
    public int size() {
        return this.exposedSize;
    }

    public void setInteger(final LargeIntegerObject other) {
        this.integer = other.integer;
    }

    @Override
    @TruffleBoundary(transferToInterpreterOnException = false)
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return this.integer.toString();
    }

    public boolean hasSameValueAs(final LargeIntegerObject other) {
        return this.integer.equals(other.integer);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof LargeIntegerObject) {
            return this.hasSameValueAs((LargeIntegerObject) other);
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
        return LargeIntegerObject.reduceIfPossible(this.image, value);
    }

    public static Object reduceIfPossible(final SqueakImageContext image, final BigInteger value) {
        if (value.bitLength() > Long.SIZE - 1) {
            return new LargeIntegerObject(image, value);
        } else {
            return value.longValue() & MASK_64BIT;
        }
    }

    public Object reduceIfPossible() {
        return this.reduceIfPossible(this.integer);
    }

    public long longValue() {
        return this.integer.longValue();
    }

    public long longValueExact() throws ArithmeticException {
        return this.integer.longValueExact();
    }

    public int intValueExact() throws ArithmeticException {
        return this.integer.intValueExact();
    }

    public boolean fitsIntoLong() {
        return this.bitLength() < Long.SIZE;
    }

    public boolean fitsIntoInt() {
        return this.bitLength() < Integer.SIZE;
    }

    public int bitLength() {
        return this.integer.bitLength();
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static LargeIntegerObject valueOf(final SqueakImageContext image, final long a) {
        return new LargeIntegerObject(image, BigInteger.valueOf(a));
    }

    public boolean isPositive() {
        return this.getSqueakClass() == this.image.largePositiveIntegerClass;
    }

    public boolean sizeLessThanWordSize() {
        /**
         * See `InterpreterPrimitives>>#positiveMachineIntegerValueOf:`.
         */
        return this.size() <= this.image.flags.wordSize();
    }

    /*
     * Arithmetic Operations
     */

    // TODO: Find out when reduceIfPossible is really necessary
    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object add(final LargeIntegerObject b) {
        return this.reduceIfPossible(this.integer.add(b.integer));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object add(final long b) {
        return this.reduceIfPossible(this.integer.add(BigInteger.valueOf(b)));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static Object add(final SqueakImageContext image, final long a, final long b) {
        return reduceIfPossible(image, BigInteger.valueOf(a).add(BigInteger.valueOf(b)));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object subtract(final LargeIntegerObject b) {
        return this.reduceIfPossible(this.integer.subtract(b.integer));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object multiply(final LargeIntegerObject b) {
        return this.reduceIfPossible(this.integer.multiply(b.integer));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object divide(final LargeIntegerObject b) {
        return this.reduceIfPossible(this.integer.divide(b.integer));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object floorDivide(final LargeIntegerObject b) {
        return this.reduceIfPossible(floorDivide(this.integer, b.integer));
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
        return this.reduceIfPossible(this.integer.subtract(floorDivide(this.integer, b.integer).multiply(b.integer)));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public LargeIntegerObject divideNoReduce(final LargeIntegerObject b) {
        return new LargeIntegerObject(this.image, this.integer.divide(b.integer));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object remainder(final LargeIntegerObject b) {
        return this.reduceIfPossible(this.integer.remainder(b.integer));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public LargeIntegerObject negateNoReduce() {
        return new LargeIntegerObject(this.image, this.integer.negate());
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public int compareTo(final LargeIntegerObject b) {
        return this.integer.compareTo(b.integer);
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public int compareTo(final BigInteger b) {
        return this.integer.compareTo(b);
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public double doubleValue() {
        return this.integer.doubleValue();
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public boolean isZero() {
        return this.integer.equals(BigInteger.ZERO);
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public boolean isZeroOrPositive() {
        return this.integer.compareTo(BigInteger.ZERO) >= 0;
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public boolean lessThanOrEqualTo(final long value) {
        return this.fitsIntoLong() && this.integer.longValue() <= value; // TODO exact?
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public boolean lessThanOneShiftedBy64() {
        return this.integer.compareTo(ONE_SHIFTED_BY_64) < 0;
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public boolean inRange(final long minValue, final long maxValue) {
        final long longValueExact = this.integer.longValue(); // TODO exact
        return minValue <= longValueExact && longValueExact <= maxValue;
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public boolean isIntegralWhenDividedBy(final LargeIntegerObject other) {
        return this.integer.remainder(other.integer).compareTo(BigInteger.ZERO) == 0;
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public LargeIntegerObject toSigned() {
        if (this.integer.shiftRight(56).compareTo(ONE_HUNDRED_TWENTY_EIGHT) >= 0) {
            return new LargeIntegerObject(this.image, this.integer.subtract(ONE_SHIFTED_BY_64));
        } else {
            return this;
        }
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public LargeIntegerObject toUnsigned() {
        if (this.integer.compareTo(BigInteger.ZERO) < 0) {
            return new LargeIntegerObject(this.image, this.integer.add(ONE_SHIFTED_BY_64));
        } else {
            return this;
        }
    }

    /*
     * Bit Operations
     */

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object and(final LargeIntegerObject b) {
        return this.reduceIfPossible(this.integer.and(b.integer));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object or(final LargeIntegerObject b) {
        return this.reduceIfPossible(this.integer.or(b.integer));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object xor(final LargeIntegerObject b) {
        return this.reduceIfPossible(this.integer.xor(b.integer));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object shiftLeft(final int b) {
        return this.reduceIfPossible(this.integer.shiftLeft(b));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object shiftRight(final int b) {
        return this.reduceIfPossible(this.integer.shiftRight(b));
    }

    public boolean isNegative() {
        return this.integer.signum() == -1;
    }

    public BigInteger getBigInteger() {
        return this.integer;
    }
}
