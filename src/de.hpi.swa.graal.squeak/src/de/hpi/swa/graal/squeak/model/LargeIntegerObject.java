package de.hpi.swa.graal.squeak.model;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.util.ArrayUtils;
import de.hpi.swa.graal.squeak.util.BigInt;

public final class LargeIntegerObject extends AbstractSqueakObject {
    public static final long SMALLINTEGER32_MIN = -0x40000000;
    public static final long SMALLINTEGER32_MAX = 0x3fffffff;
    public static final long SMALLINTEGER64_MIN = -0x1000000000000000L;
    public static final long SMALLINTEGER64_MAX = 0xfffffffffffffffL;
    public static final long MASK_32BIT = 0xffffffffL;
    public static final long MASK_64BIT = 0xffffffffffffffffL;
    private static final BigInt ONE_HUNDRED_TWENTY_EIGHT = new BigInt(128);
    private static final BigInt LONG_MIN_OVERFLOW_RESULT = new BigInt(Long.MIN_VALUE); // abs?

    private BigInt integer;

    public LargeIntegerObject(final SqueakImageContext image, final BigInt integer) {
        super(image, integer.compareTo(new BigInt(0)) >= 0 ? image.largePositiveIntegerClass : image.largeNegativeIntegerClass);
        this.integer = integer;
    }

    public LargeIntegerObject(final SqueakImageContext image, final long hash, final ClassObject klass, final byte[] bytes) {
        super(image, hash, klass);
        this.integer = new BigInt(klass == image.largeNegativeIntegerClass ? -1 : 1, bytes, bytes.length);
    }

    public LargeIntegerObject(final SqueakImageContext image, final ClassObject klass, final byte[] bytes) {
        super(image, klass);
        this.integer = new BigInt(klass == image.largeNegativeIntegerClass ? -1 : 1, bytes, bytes.length);
    }

    public LargeIntegerObject(final SqueakImageContext image, final ClassObject klass, final int size) {
        super(image, klass);
        this.integer = new BigInt(0);
        if (klass == image.largeNegativeIntegerClass) {
            this.integer.setNegative();
        }
        this.integer.setSize(size);
    }

    public LargeIntegerObject(final LargeIntegerObject original) {
        super(original.image, original.getSqueakClass());
        this.integer = original.integer;
    }

    public static LargeIntegerObject createLongMinOverflowResult(final SqueakImageContext image) {
        return new LargeIntegerObject(image, LONG_MIN_OVERFLOW_RESULT);
    }

    public static byte[] getLongMinOverflowResultBytes() {
        return bigIntToBytes(LONG_MIN_OVERFLOW_RESULT);
    }

    public long getNativeAt0(final long index) {
        return Byte.toUnsignedLong(integer.getByteAt((int) index));
    }

    public void setNativeAt0(final long index, final long value) {
        if (value < 0 || value > NativeObject.BYTE_MAX) { // check for overflow
            throw new IllegalArgumentException("Illegal value for LargeIntegerObject: " + value);
        }
        integer.setByte((int) index, (byte) value);
    }

    public byte[] getBytes() {
        return integer.getBytes();
    }

    public void setBytes(final byte[] bytes) {
        integer.setBytes(bytes, 0, 0, bytes.length);
    }

    public void setBytes(final byte[] bytes, final int srcPos, final int destPos, final int length) {
        integer.setBytes(bytes, srcPos, destPos, length);
    }

    @Override
    public int instsize() {
        return 0;
    }

    @Override
    public int size() {
        return integer.length();
    }

    public void setInteger(final LargeIntegerObject other) {
        this.integer = other.integer;
    }

    public static byte[] bigIntToBytes(final BigInt bigInt) {
        final byte[] bytes = bigInt.getBytes(); // Todo: byteValue
        if (bytes[0] == 0) {
            return ArrayUtils.swapOrderInPlace(Arrays.copyOfRange(bytes, 1, bytes.length));
        } else {
            return ArrayUtils.swapOrderInPlace(bytes);
        }
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

    public static Object reduceIfPossible(final LargeIntegerObject value) {
        if (value.bitLength() > Long.SIZE - 1) {
            return value;
        } else {
            return value.longValue() & MASK_64BIT;
        }
    }

    public Object reduceIfPossible() {
        return reduceIfPossible(this);
    }

    public Object reduceIfPossible(final BigInt value) {
        return reduceIfPossible(newFromBigInt(image, value));
    }

    public long longValue() {
        return integer.longValue();
    }

    public long longValueExact() throws ArithmeticException {
        return integer.longValue(); // TODO: exact
    }

    public int intValueExact() throws ArithmeticException {
        return integer.intValue(); // TODO: exact
    }

    public boolean fitsIntoLong() {
        return bitLength() <= 63;
    }

    public boolean fitsIntoInt() {
        return bitLength() <= 31;
    }

    public int bitLength() {
        return integer.bitLength();
    }

    private static LargeIntegerObject newFromBigInt(final SqueakImageContext image, final BigInt value) {
        return new LargeIntegerObject(image, value);
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static LargeIntegerObject valueOf(final SqueakImageContext image, final long a) {
        return newFromBigInt(image, new BigInt(a));
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
        final BigInt value = integer.copy();
        value.add(b.integer);
        return reduceIfPossible(value);
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object subtract(final LargeIntegerObject b) {
        final BigInt value = integer.copy();
        value.sub(b.integer);
        return reduceIfPossible(value);
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object multiply(final LargeIntegerObject b) {
        final BigInt value = integer.copy();
        value.mul(b.integer);
        return reduceIfPossible(value);
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object divide(final LargeIntegerObject b) {
        final BigInt value = integer.copy();
        value.div(b.integer);
        return reduceIfPossible(value);
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object floorDivide(final LargeIntegerObject b) {
        return reduceIfPossible(floorDivide(integer, b.integer));
    }

    private BigInt floorDivide(final BigInt x, final BigInt y) {
        final BigInt r = x.copy();
        r.div(y);
        final BigInt test = r.copy();
        test.mul(y);
        // if the signs are different and modulo not zero, round down
        if (x.signum() != y.signum() && !test.equals(x)) {
            r.sub(1);
        }
        return r;
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object floorMod(final LargeIntegerObject b) {
        final BigInt value = integer.copy();
        value.sub(floorDivide(value, b.integer));
        value.mul(b.integer);
        return reduceIfPossible(value);
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public LargeIntegerObject divideNoReduce(final LargeIntegerObject b) {
        final BigInt value = integer.copy();
        value.div(b.integer);
        return newFromBigInt(image, value);
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object remainder(final LargeIntegerObject b) {
        final BigInt value = integer.copy();
        value.rem(b.integer);
        return reduceIfPossible(value);
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public LargeIntegerObject negateNoReduce() {
        final BigInt value = integer.copy();
        value.mul(-1);
        return newFromBigInt(image, value);
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public int compareTo(final LargeIntegerObject b) {
        return integer.compareTo(b.integer);
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public int compareTo(final BigInt b) {
        return integer.compareTo(b);
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public double doubleValue() {
        return integer.doubleValue();
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public boolean isZero() {
        return integer.isZero();
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public boolean isZeroOrPositive() {
        return integer.compareTo(new BigInt(0)) >= 0;
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public boolean lessThanOrEqualTo(final long value) {
        return fitsIntoLong() && integer.longValue() <= value; // TODO exact?
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public boolean lessThanOneShiftedBy64() {
        final BigInt oneshifted = new BigInt(1);
        oneshifted.shiftLeft(64);
        return integer.compareTo(oneshifted) < 0;
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public boolean inRange(final long minValue, final long maxValue) {
        final long longValueExact = integer.longValue(); // TODO exact
        return minValue <= longValueExact && longValueExact <= maxValue;
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public boolean isIntegralWhenDividedBy(final LargeIntegerObject other) {
        final BigInt value = integer.copy();
        value.rem(other.integer);
        return value.compareTo(new BigInt(0)) == 0;
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public LargeIntegerObject toSigned() {
        final BigInt oneshifted = new BigInt(1);
        oneshifted.shiftLeft(64);
        final BigInt shiftback = integer.copy();
        shiftback.shiftRight(56);
        if (shiftback.compareTo(ONE_HUNDRED_TWENTY_EIGHT) >= 0) {
            integer.sub(oneshifted);
        }
        return this;
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public LargeIntegerObject toUnsigned() {
        final BigInt oneshifted = new BigInt(1);
        oneshifted.shiftLeft(64);
        if (integer.compareTo(new BigInt(0)) < 0) {
            integer.add(oneshifted);
        }
        return this;
    }

    /*
     * Bit Operations
     */

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object and(final LargeIntegerObject b) {
        final BigInt value = integer.copy();
        value.and(b.integer);
        return reduceIfPossible(value);
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object or(final LargeIntegerObject b) {
        final BigInt value = integer.copy();
        value.or(b.integer);
        return reduceIfPossible(value);
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object xor(final LargeIntegerObject b) {
        final BigInt value = integer.copy();
        value.xor(b.integer);
        return reduceIfPossible(value);
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object shiftLeft(final int b) {
        final BigInt value = integer.copy();
        value.shiftLeft(b);
        return reduceIfPossible(value);
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object shiftRight(final int b) {
        final BigInt value = integer.copy();
        value.shiftRight(b);
        return reduceIfPossible(value);
    }

    public boolean isNegative() {
        return this.integer.signum() == -1;
    }
}
