package de.hpi.swa.graal.squeak.model;

import java.math.BigInteger;
import java.util.Arrays;

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

    private byte[] bytes;
    private BigInteger integer;
    private boolean integerDirty = false;

    public LargeIntegerObject(final SqueakImageContext img, final BigInteger integer) {
        super(img, integer.compareTo(BigInteger.ZERO) >= 0 ? img.largePositiveIntegerClass : img.largeNegativeIntegerClass);
        this.bytes = derivedBytesFromBigInteger(integer);
        this.integer = integer;
    }

    public LargeIntegerObject(final SqueakImageContext img, final long hash, final ClassObject klass, final byte[] bytes) {
        super(img, hash, klass);
        this.bytes = bytes;
        this.integerDirty = true;
    }

    public LargeIntegerObject(final SqueakImageContext img, final ClassObject klass, final byte[] bytes) {
        super(img, klass);
        this.bytes = bytes;
        this.integerDirty = true;
    }

    public LargeIntegerObject(final SqueakImageContext image, final ClassObject klass, final int size) {
        super(image, klass);
        this.bytes = new byte[size];
        integer = BigInteger.ZERO;
    }

    public LargeIntegerObject(final LargeIntegerObject original) {
        super(original.image, original.getSqueakClass());
        bytes = original.bytes.clone();
        integer = original.integer;
    }

    public long getNativeAt0(final long index) {
        return Byte.toUnsignedLong(bytes[(int) index]);
    }

    public void setNativeAt0(final long index, final long value) {
        if (value < 0 || value > NativeObject.BYTE_MAX) { // check for overflow
            throw new IllegalArgumentException("Illegal value for LargeIntegerObject: " + value);
        }
        bytes[(int) index] = (byte) value;
        integerDirty = true;
    }

    public void setBytes(final byte[] bytes) {
        this.bytes = bytes;
        integerDirty = true;
    }

    @TruffleBoundary
    public BigInteger getBigInteger() {
        if (integerDirty) {
            integer = derivedBigIntegerFromBytes(bytes, isNegative());
            integerDirty = false;
        }
        return integer;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public int size() {
        return bytes.length;
    }

    private static BigInteger derivedBigIntegerFromBytes(final byte[] bytes, final boolean isNegative) {
        final byte[] bigEndianBytes = ArrayUtils.swapOrderCopy(bytes);
        if (bigEndianBytes.length == 0) {
            return BigInteger.ZERO;
        } else {
            if (isNegative) {
                return bigIntegerFromBigEndianBytes(bigEndianBytes).negate();
            } else {
                return bigIntegerFromBigEndianBytes(bigEndianBytes);
            }
        }
    }

    private static BigInteger bigIntegerFromBigEndianBytes(final byte[] bigEndianBytes) {
        return new BigInteger(bigEndianBytes).and(BigInteger.valueOf(1).shiftLeft(bigEndianBytes.length * 8).subtract(BigInteger.valueOf(1)));
    }

    private static byte[] derivedBytesFromBigInteger(final BigInteger integer) {
        final byte[] array = integer.abs().toByteArray();
        final int length = array.length;
        if (length <= 1) {
            return array;
        }
        int skipped = 0;
        for (int i = 0; i < length; i++) {
            if (array[i] == 0) {
                skipped++;
                continue;
            } else {
                break;
            }
        }
        return ArrayUtils.swapOrderInPlace(Arrays.copyOfRange(array, skipped, length));
    }

    public boolean isNegative() {
        return getSqueakClass() == image.largeNegativeIntegerClass;
    }

    @Override
    @TruffleBoundary
    public String toString() {
        return getBigInteger().toString();
    }

    @Override
    public boolean equals(final Object other) {
        if (other instanceof LargeIntegerObject) {
            return Arrays.equals(bytes, ((LargeIntegerObject) other).bytes);
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

    @TruffleBoundary
    private Object reduceIfPossible(final BigInteger value) {
        if (value.bitLength() > Long.SIZE - 1) {
            return newFromBigInteger(value);
        } else {
            return value.longValue() & MASK_64BIT;
        }
    }

    @TruffleBoundary
    public Object reduceIfPossible() {
        return reduceIfPossible(getBigInteger());
    }

    @TruffleBoundary
    public long longValue() {
        return getBigInteger().longValue();
    }

    @TruffleBoundary
    public long longValueExact() throws ArithmeticException {
        return getBigInteger().longValueExact();
    }

    public boolean fitsIntoLong() {
        return bitLength() <= 63;
    }

    @TruffleBoundary
    public int bitLength() {
        return getBigInteger().bitLength();
    }

    private LargeIntegerObject newFromBigInteger(final BigInteger value) {
        return newFromBigInteger(image, value);
    }

    private static LargeIntegerObject newFromBigInteger(final SqueakImageContext image, final BigInteger value) {
        return new LargeIntegerObject(image, value);
    }

    @TruffleBoundary
    public static LargeIntegerObject valueOf(final SqueakImageContext image, final long a) {
        return newFromBigInteger(image, BigInteger.valueOf(a));
    }

    /*
     * Arithmetic Operations
     */

    @TruffleBoundary
    public Object add(final LargeIntegerObject b) {
        return reduceIfPossible(getBigInteger().add(b.getBigInteger()));
    }

    @TruffleBoundary
    public Object subtract(final LargeIntegerObject b) {
        return reduceIfPossible(getBigInteger().subtract(b.getBigInteger()));
    }

    @TruffleBoundary
    public Object multiply(final LargeIntegerObject b) {
        return reduceIfPossible(getBigInteger().multiply(b.getBigInteger()));
    }

    @TruffleBoundary
    public Object divide(final LargeIntegerObject b) {
        return reduceIfPossible(getBigInteger().divide(b.getBigInteger()));
    }

    @TruffleBoundary
    public Object floorDivide(final LargeIntegerObject b) {
        return reduceIfPossible(floorDivide(getBigInteger(), b.getBigInteger()));
    }

    private static BigInteger floorDivide(final BigInteger x, final BigInteger y) {
        BigInteger r = x.divide(y);
        // if the signs are different and modulo not zero, round down
        if (x.signum() != y.signum() && !r.multiply(y).equals(x)) {
            r = r.subtract(BigInteger.ONE);
        }
        return r;
    }

    @TruffleBoundary
    public Object floorMod(final LargeIntegerObject b) {
        return reduceIfPossible(getBigInteger().subtract(floorDivide(getBigInteger(), b.getBigInteger()).multiply(b.getBigInteger())));
    }

    @TruffleBoundary
    public LargeIntegerObject divideNoReduce(final LargeIntegerObject b) {
        return newFromBigInteger(getBigInteger().divide(b.getBigInteger()));
    }

    @TruffleBoundary
    public Object remainder(final LargeIntegerObject b) {
        return reduceIfPossible(getBigInteger().remainder(b.getBigInteger()));
    }

    @TruffleBoundary
    public LargeIntegerObject negateNoReduce() {
        return newFromBigInteger(getBigInteger().negate());
    }

    @TruffleBoundary
    public int compareTo(final LargeIntegerObject b) {
        return getBigInteger().compareTo(b.getBigInteger());
    }

    @TruffleBoundary
    public double doubleValue() {
        return getBigInteger().doubleValue();
    }

    @TruffleBoundary
    public boolean isZero() {
        return getBigInteger().compareTo(BigInteger.ZERO) == 0;
    }

    @TruffleBoundary
    public boolean isZeroOrPositive() {
        return getBigInteger().compareTo(BigInteger.ZERO) >= 0;
    }

    @TruffleBoundary
    public boolean lessThanOrEqualTo(final long value) {
        try {
            return getBigInteger().longValueExact() <= value;
        } catch (ArithmeticException e) {
            return false;
        }
    }

    @TruffleBoundary
    public boolean inRange(final long minValue, final long maxValue) {
        final long longValueExact = getBigInteger().longValueExact();
        return minValue <= longValueExact && longValueExact <= maxValue;
    }

    @TruffleBoundary
    public boolean isIntegralWhenDividedBy(final LargeIntegerObject other) {
        return getBigInteger().mod(other.getBigInteger().abs()).compareTo(BigInteger.ZERO) == 0;
    }

    /*
     * Bit Operations
     */

    @TruffleBoundary
    public Object and(final LargeIntegerObject b) {
        return reduceIfPossible(getBigInteger().and(b.getBigInteger()));
    }

    @TruffleBoundary
    public Object or(final LargeIntegerObject b) {
        return reduceIfPossible(getBigInteger().or(b.getBigInteger()));
    }

    @TruffleBoundary
    public Object xor(final LargeIntegerObject b) {
        return reduceIfPossible(getBigInteger().xor(b.getBigInteger()));
    }

    @TruffleBoundary
    public Object shiftLeft(final int b) {
        return reduceIfPossible(getBigInteger().shiftLeft(b));
    }

    @TruffleBoundary
    public Object shiftRight(final int b) {
        return reduceIfPossible(getBigInteger().shiftRight(b));
    }
}
