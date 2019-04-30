package de.hpi.swa.graal.squeak.model;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.util.ArrayUtils;

@ExportLibrary(InteropLibrary.class)
public final class LargeIntegerObject extends AbstractSqueakObjectWithClassAndHash {
    private static final BigInteger ONE_SHIFTED_BY_64 = BigInteger.ONE.shiftLeft(64);
    private static final BigInteger ONE_HUNDRED_TWENTY_EIGHT = BigInteger.valueOf(128);
    private static final BigInteger LONG_MIN_OVERFLOW_RESULT = BigInteger.valueOf(Long.MIN_VALUE).abs();
    private static final byte[] LONG_MIN_OVERFLOW_RESULT_BYTES = bigIntegerToBytes(LONG_MIN_OVERFLOW_RESULT);

    private BigInteger integer;
    private final int exposedSize;

    public LargeIntegerObject(final SqueakImageContext image, final BigInteger integer) {
        super(image, integer.signum() >= 0 ? image.largePositiveIntegerClass : image.largeNegativeIntegerClass);
        this.integer = integer;
        exposedSize = byteLength(integer);
    }

    public LargeIntegerObject(final SqueakImageContext image, final long hash, final ClassObject klass, final byte[] bytes) {
        super(image, hash, klass);
        exposedSize = bytes.length;
        integer = new BigInteger(isPositive() ? 1 : -1, ArrayUtils.swapOrderCopy(bytes));
    }

    public LargeIntegerObject(final SqueakImageContext image, final ClassObject klass, final byte[] bytes) {
        super(image, klass);
        exposedSize = bytes.length;
        integer = new BigInteger(isPositive() ? 1 : -1, ArrayUtils.swapOrderCopy(bytes));
    }

    public LargeIntegerObject(final SqueakImageContext image, final ClassObject klass, final int size) {
        super(image, klass);
        exposedSize = size;
        integer = BigInteger.ZERO;
    }

    private LargeIntegerObject(final LargeIntegerObject original) {
        super(original.image, original.getSqueakClass());
        exposedSize = original.exposedSize;
        integer = original.integer;
    }

    public static LargeIntegerObject createLongMinOverflowResult(final SqueakImageContext image) {
        return new LargeIntegerObject(image, LONG_MIN_OVERFLOW_RESULT);
    }

    public static byte[] getLongMinOverflowResultBytes() {
        return LONG_MIN_OVERFLOW_RESULT_BYTES;
    }

    private static byte[] bigIntegerToBytes(final BigInteger bigInteger) {
        return ArrayUtils.swapOrderInPlace(bigIntegerToBigEndianBytes(bigInteger));
    }

    private byte[] bigIntegerToBigEndianBytes() {
        return bigIntegerToBigEndianBytes(integer);
    }

    private static byte[] bigIntegerToBigEndianBytes(final BigInteger bigInteger) {
        final byte[] bytes = bigInteger.abs().toByteArray();
        return bytes[0] != 0 ? bytes : Arrays.copyOfRange(bytes, 1, bytes.length);
    }

    public long getNativeAt0(final long index) {
        // TODO: File issue with toByteArray not working on negative numbers?
        final byte[] bytes = this.bigIntegerToBigEndianBytes();
        if (exposedSize - (int) index > bytes.length) {
            return 0L;
        }
        return Byte.toUnsignedLong(bytes[Math.max(bytes.length, exposedSize) - 1 - (int) index]);
    }

    public void setNativeAt0(final long index, final long value) {
        if (value < 0 || value > NativeObject.BYTE_MAX) { // check for overflow
            throw new IllegalArgumentException("Illegal value for LargeIntegerObject: " + value);
        }
        final byte[] bytes;
        final byte[] bigIntegerBytes = this.bigIntegerToBigEndianBytes();
        if (bigIntegerBytes.length <= index) {
            bytes = new byte[Math.min(exposedSize, (int) index + 1)];
            System.arraycopy(bigIntegerBytes, 0, bytes, bytes.length - bigIntegerBytes.length, bigIntegerBytes.length);
        } else {
            bytes = bigIntegerBytes;
        }
        bytes[bytes.length - 1 - (int) index] = (byte) value;
        integer = new BigInteger(isPositive() ? 1 : -1, bytes);
    }

    public byte[] getBytes() {
        return bigIntegerToBytes(integer);
    }

    public void replaceInternalValue(final LargeIntegerObject other) {
        assert exposedSize == other.exposedSize;
        integer = other.getSqueakClass() == getSqueakClass() ? other.integer : other.integer.negate();
    }

    public void setBytes(final byte[] bytes) {
        assert exposedSize == bytes.length;
        integer = new BigInteger(isPositive() ? 1 : -1, ArrayUtils.swapOrderCopy(bytes));
    }

    public void setBytes(final LargeIntegerObject src, final int srcPos, final int destPos, final int length) {
        final byte[] bytes;
        final byte[] srcBytes = src.bigIntegerToBigEndianBytes();
        final byte[] bigIntegerBytes = this.bigIntegerToBigEndianBytes();
        if (bigIntegerBytes.length < destPos + length) {
            bytes = new byte[exposedSize];
            System.arraycopy(bigIntegerBytes, 0, bytes, 0, bigIntegerBytes.length);
        } else {
            bytes = bigIntegerBytes;
        }
        for (int i = 0; i < length; i++) {
            bytes[bytes.length - 1 - (destPos + i)] = srcBytes[srcBytes.length - 1 - (srcPos + i)];
        }
        integer = new BigInteger(isPositive() ? 1 : -1, bytes);
    }

    public void setBytes(final byte[] srcBytes, final int srcPos, final int destPos, final int length) {
        // destination bytes are big-endian, source bytes are not
        final byte[] bytes;
        final byte[] bigIntegerBytes = this.bigIntegerToBigEndianBytes();
        if (bigIntegerBytes.length < destPos + length) {
            bytes = new byte[exposedSize];
            System.arraycopy(bigIntegerBytes, 0, bytes, 0, bigIntegerBytes.length);
        } else {
            bytes = bigIntegerBytes;
        }
        for (int i = 0; i < length; i++) {
            bytes[bytes.length - 1 - (destPos + i)] = srcBytes[srcPos + i];
        }
        integer = new BigInteger(isPositive() ? 1 : -1, bytes);
    }

    private static int byteLength(final BigInteger integer) {
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
        return exposedSize;
    }

    public void setInteger(final LargeIntegerObject other) {
        integer = other.integer;
    }

    @Override
    @TruffleBoundary(transferToInterpreterOnException = false)
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
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
        if (other instanceof LargeIntegerObject) {
            return equals((LargeIntegerObject) other);
        } else {
            return super.equals(other);
        }
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public LargeIntegerObject shallowCopy() {
        return new LargeIntegerObject(this);
    }

    private Object reduceIfPossible(final BigInteger value) {
        return reduceIfPossible(image, value);
    }

    private static Object reduceIfPossible(final SqueakImageContext image, final BigInteger value) {
        if (value.bitLength() < Long.SIZE) {
            assert value.longValueExact() <= Long.MAX_VALUE : "Should never throw an ArithmeticException";
            return value.longValue();
        } else {
            return new LargeIntegerObject(image, value);
        }
    }

    public Object reduceIfPossible() {
        return reduceIfPossible(integer);
    }

    public long longValue() {
        return integer.longValue();
    }

    public long longValueExact() throws ArithmeticException {
        return integer.longValueExact();
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    private byte byteValueExact() throws ArithmeticException {
        return integer.byteValueExact();
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    private short shortValueExact() throws ArithmeticException {
        return integer.shortValueExact();
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public int intValueExact() throws ArithmeticException {
        return integer.intValueExact();
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

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static LargeIntegerObject valueOf(final SqueakImageContext image, final long a) {
        return new LargeIntegerObject(image, BigInteger.valueOf(a));
    }

    public boolean isPositive() {
        return getSqueakClass() == image.largePositiveIntegerClass;
    }

    public boolean isNegative() {
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

    @TruffleBoundary(transferToInterpreterOnException = false)
    public LargeIntegerObject addNoReduce(final LargeIntegerObject b) {
        return new LargeIntegerObject(image, integer.add(b.integer));
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
    public LargeIntegerObject subtractNoReduce(final LargeIntegerObject b) {
        return new LargeIntegerObject(image, integer.subtract(b.integer));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object subtract(final long b) {
        return reduceIfPossible(integer.subtract(BigInteger.valueOf(b)));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static Object subtract(final SqueakImageContext image, final long a, final long b) {
        return reduceIfPossible(image, BigInteger.valueOf(a).subtract(BigInteger.valueOf(b)));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static Object subtract(final long a, final LargeIntegerObject b) {
        return reduceIfPossible(b.image, BigInteger.valueOf(a).subtract(b.integer));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object multiply(final LargeIntegerObject b) {
        return reduceIfPossible(integer.multiply(b.integer));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object multiply(final long b) {
        return reduceIfPossible(integer.multiply(BigInteger.valueOf(b)));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public LargeIntegerObject multiplyNoReduce(final LargeIntegerObject b) {
        return new LargeIntegerObject(image, integer.multiply(b.integer));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static Object multiply(final SqueakImageContext image, final long a, final long b) {
        return reduceIfPossible(image, BigInteger.valueOf(a).multiply(BigInteger.valueOf(b)));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object divide(final LargeIntegerObject b) {
        return reduceIfPossible(integer.divide(b.integer));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object divide(final long b) {
        return reduceIfPossible(integer.divide(BigInteger.valueOf(b)));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static Object divide(final long a, final LargeIntegerObject b) {
        return reduceIfPossible(b.image, BigInteger.valueOf(a).divide(b.integer));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object floorDivide(final LargeIntegerObject b) {
        return reduceIfPossible(floorDivide(integer, b.integer));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object floorDivide(final long b) {
        return reduceIfPossible(floorDivide(integer, BigInteger.valueOf(b)));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static Object floorDivide(final long a, final LargeIntegerObject b) {
        return reduceIfPossible(b.image, floorDivide(BigInteger.valueOf(a), b.integer));
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
    public Object floorMod(final long b) {
        final BigInteger bValue = BigInteger.valueOf(b);
        return reduceIfPossible(integer.subtract(floorDivide(integer, bValue).multiply(bValue)));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static Object floorMod(final long a, final LargeIntegerObject b) {
        final BigInteger aValue = BigInteger.valueOf(a);
        return reduceIfPossible(b.image, aValue.subtract(floorDivide(aValue, b.integer).multiply(b.integer)));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public LargeIntegerObject divideNoReduce(final LargeIntegerObject b) {
        return new LargeIntegerObject(image, integer.divide(b.integer));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object remainder(final LargeIntegerObject b) {
        return reduceIfPossible(integer.remainder(b.integer));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public LargeIntegerObject negate() {
        return new LargeIntegerObject(image, integer.negate());
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public int compareTo(final LargeIntegerObject b) {
        return integer.compareTo(b.integer);
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public int compareTo(final long b) {
        return integer.compareTo(BigInteger.valueOf(b));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public double doubleValue() {
        return integer.doubleValue();
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public boolean isZero() {
        return integer.signum() == 0;
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public boolean isZeroOrPositive() {
        return integer.signum() >= 0;
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
        return integer.remainder(other.integer).signum() == 0;
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public boolean isIntegralWhenDividedBy(final long other) {
        return integer.remainder(BigInteger.valueOf(other)).signum() == 0;
    }

    public boolean sameSign(final LargeIntegerObject other) {
        return getSqueakClass() == other.getSqueakClass();
    }

    public boolean sameSign(final long other) {
        return isPositive() && other >= 0 || isNegative() && other < 0;
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public LargeIntegerObject toSigned() {
        if (integer.shiftRight(56).compareTo(ONE_HUNDRED_TWENTY_EIGHT) >= 0) {
            return new LargeIntegerObject(image, integer.subtract(ONE_SHIFTED_BY_64));
        } else {
            return this;
        }
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public LargeIntegerObject toUnsigned() {
        if (isNegative()) {
            return new LargeIntegerObject(image, integer.add(ONE_SHIFTED_BY_64));
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
        return reduceIfPossible(integer.shiftLeft(b));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static Object shiftLeft(final SqueakImageContext image, final long a, final int b) {
        return reduceIfPossible(image, BigInteger.valueOf(a).shiftLeft(b));
    }

    public LargeIntegerObject shiftLeftNoReduce(final int b) {
        return new LargeIntegerObject(image, integer.shiftLeft(b));
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public Object shiftRight(final int b) {
        return reduceIfPossible(integer.shiftRight(b));
    }

    public BigInteger getBigInteger() {
        return integer;
    }

    /*
     * INTEROPERABILITY
     */

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean isNumber() {
        return fitsInLong() || fitsInDouble();
    }

    @ExportMessage
    public boolean fitsInByte() {
        return bitLength() < Byte.SIZE;
    }

    @ExportMessage
    public boolean fitsInShort() {
        return bitLength() < Short.SIZE;
    }

    @ExportMessage
    public boolean fitsInInt() {
        return bitLength() < Integer.SIZE;
    }

    @ExportMessage
    public boolean fitsInLong() {
        return bitLength() < Long.SIZE;
    }

    @ExportMessage
    @TruffleBoundary
    public boolean fitsInFloat() {
        if (bitLength() <= 24) { // 24 = size of float mantissa + 1
            return true;
        } else {
            final float floatValue = integer.floatValue();
            if (!Float.isFinite(floatValue)) {
                return false;
            }
            return new BigDecimal(floatValue).toBigIntegerExact().equals(integer);
        }
    }

    @ExportMessage
    @TruffleBoundary
    public boolean fitsInDouble() {
        if (bitLength() <= 53) { // 53 = size of double mantissa + 1
            return true;
        } else {
            final double doubleValue = doubleValue();
            if (!Double.isFinite(doubleValue)) {
                return false;
            }
            return new BigDecimal(doubleValue).toBigIntegerExact().equals(integer);
        }
    }

    @ExportMessage
    public byte asByte() throws UnsupportedMessageException {
        try {
            return byteValueExact();
        } catch (final ArithmeticException e) {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    public short asShort() throws UnsupportedMessageException {
        try {
            return shortValueExact();
        } catch (final ArithmeticException e) {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    public int asInt() throws UnsupportedMessageException {
        try {
            return intValueExact();
        } catch (final ArithmeticException e) {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    public long asLong() throws UnsupportedMessageException {
        try {
            return longValueExact();
        } catch (final ArithmeticException e) {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    @TruffleBoundary
    public float asFloat() throws UnsupportedMessageException {
        if (fitsInFloat()) {
            return integer.floatValue();
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    @TruffleBoundary
    public double asDouble() throws UnsupportedMessageException {
        if (fitsInDouble()) {
            return doubleValue();
        } else {
            throw UnsupportedMessageException.create();
        }
    }
}
