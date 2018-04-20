package de.hpi.swa.graal.squeak.model;

import java.math.BigInteger;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.graal.squeak.SqueakImageContext;
import de.hpi.swa.graal.squeak.util.ArrayUtils;
import de.hpi.swa.graal.squeak.util.SqueakImageChunk;

public final class LargeIntegerObject extends NativeObject {
    @CompilationFinal public static final long SMALLINTEGER32_MIN = -0x40000000;
    @CompilationFinal public static final long SMALLINTEGER32_MAX = 0x3fffffff;
    @CompilationFinal public static final long SMALLINTEGER64_MIN = -0x1000000000000000L;
    @CompilationFinal public static final long SMALLINTEGER64_MAX = 0xfffffffffffffffL;
    @CompilationFinal public static final long MASK_32BIT = 0xffffffffL;
    @CompilationFinal public static final long MASK_64BIT = 0xffffffffffffffffL;

    @CompilationFinal private BigInteger integer;

    public LargeIntegerObject(final SqueakImageContext img) {
        super(img, null, new NativeBytesStorage(0));
    }

    public LargeIntegerObject(final SqueakImageContext img, final BigInteger integer) {
        super(img, integer.compareTo(BigInteger.ZERO) >= 0 ? img.largePositiveIntegerClass : img.largeNegativeIntegerClass);
        this.integer = integer;
        final byte[] byteArray;
        final byte[] array = integer.abs().toByteArray();
        final int size = (integer.bitLength() + 7) / 8;
        if (array.length > size) {
            byteArray = new byte[size];
            final int offset = array.length - size;
            for (int i = 0; i < byteArray.length; i++) {
                byteArray[i] = array[offset + i];
            }
        } else {
            assert array.length == size;
            byteArray = array;
        }
        this.storage = new NativeBytesStorage(ArrayUtils.swapOrderInPlace(byteArray));
    }

    public LargeIntegerObject(final SqueakImageContext img, final ClassObject klass, final byte[] bytes) {
        super(img, klass);
        this.storage = new NativeBytesStorage(bytes);
        derivedBigIntegerFromBytes();
    }

    public LargeIntegerObject(final SqueakImageContext image, final ClassObject klass, final int size) {
        super(image, klass);
        this.storage = new NativeBytesStorage(size);
        integer = BigInteger.ZERO;
    }

    public LargeIntegerObject(final LargeIntegerObject original) {
        super(original);
        integer = original.integer;
    }

    @Override
    public void fillin(final SqueakImageChunk chunk) {
        super.fillin(chunk);
        derivedBigIntegerFromBytes();
    }

    @Override
    public void setNativeAt0(final long index, final long object) {
        super.setNativeAt0(index, object);
        derivedBigIntegerFromBytes();
    }

    private void derivedBigIntegerFromBytes() {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        final byte[] bigEndianBytes = ArrayUtils.swapOrderCopy(storage.getBytes());
        if (bigEndianBytes.length == 0) {
            integer = BigInteger.ZERO;
        } else {
            integer = new BigInteger(bigEndianBytes).and(BigInteger.valueOf(1).shiftLeft(bigEndianBytes.length * 8).subtract(BigInteger.valueOf(1)));
        }
        if (isNegative()) {
            integer = integer.negate();
        }
    }

    public boolean isNegative() {
        return getSqClass() == image.largeNegativeIntegerClass;
    }

    public BigInteger getValue() {
        return integer;
    }

    @TruffleBoundary
    public static int byteSize(final BigInteger value) {
        return value.abs().toByteArray().length;
    }

    @Override
    @TruffleBoundary
    public String toString() {
        return integer.toString();
    }

    @Override
    public boolean equals(final Object other) {
        if (other instanceof LargeIntegerObject) {
            return integer.equals(((LargeIntegerObject) other).integer);
        } else {
            return super.equals(other);
        }
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public BaseSqueakObject shallowCopy() {
        return new LargeIntegerObject(this);
    }

    @TruffleBoundary
    protected Object reduceIfPossible(final BigInteger value) {
        if (value.bitLength() > Long.SIZE - 1) {
            return newFromBigInteger(value);
        } else {
            return value.longValue() & MASK_64BIT;
        }
    }

    @TruffleBoundary
    public Object reduceIfPossible() {
        return reduceIfPossible(integer);
    }

    @TruffleBoundary
    public long reduceToLong() throws ArithmeticException {
        return integer.longValueExact() & MASK_64BIT;
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
        return reduceIfPossible(integer.add(b.integer));
    }

    @TruffleBoundary
    public Object subtract(final LargeIntegerObject b) {
        return reduceIfPossible(integer.subtract(b.integer));
    }

    @TruffleBoundary
    public Object multiply(final LargeIntegerObject b) {
        return reduceIfPossible(integer.multiply(b.integer));
    }

    @TruffleBoundary
    public Object divide(final LargeIntegerObject b) {
        return reduceIfPossible(integer.divide(b.integer));
    }

    @TruffleBoundary
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

    @TruffleBoundary
    public Object floorMod(final LargeIntegerObject b) {
        return reduceIfPossible(integer.subtract(floorDivide(integer, b.integer).multiply(b.integer)));
    }

    @TruffleBoundary
    public LargeIntegerObject divideNoReduce(final LargeIntegerObject b) {
        return newFromBigInteger(integer.divide(b.integer));
    }

    @TruffleBoundary
    public Object remainder(final LargeIntegerObject b) {
        return reduceIfPossible(integer.remainder(b.integer));
    }

    @TruffleBoundary
    public LargeIntegerObject negateNoReduce() {
        return newFromBigInteger(integer.negate());
    }

    @TruffleBoundary
    public int compareTo(final LargeIntegerObject b) {
        return integer.compareTo(b.integer);
    }

    @TruffleBoundary
    public double doubleValue() {
        return integer.doubleValue();
    }

    @TruffleBoundary
    public boolean isZero() {
        return integer.compareTo(BigInteger.ZERO) == 0;
    }

    @TruffleBoundary
    public boolean isIntegralWhenDividedBy(final LargeIntegerObject other) {
        return integer.mod(other.integer).compareTo(BigInteger.ZERO) == 0;
    }

    /*
     * Bit Operations
     */

    @TruffleBoundary
    public Object and(final LargeIntegerObject b) {
        return reduceIfPossible(integer.and(b.integer));
    }

    @TruffleBoundary
    public Object or(final LargeIntegerObject b) {
        return reduceIfPossible(integer.or(b.integer));
    }

    @TruffleBoundary
    public Object xor(final LargeIntegerObject b) {
        return reduceIfPossible(integer.xor(b.integer));
    }

    @TruffleBoundary
    public Object shiftLeft(final int b) {
        return reduceIfPossible(integer.shiftLeft(b));
    }

    @TruffleBoundary
    public Object shiftRight(final int b) {
        return reduceIfPossible(integer.shiftRight(b));
    }

    @Override
    public void setByte(final int index, final byte value) {
        super.setByte(index, value);
        derivedBigIntegerFromBytes();
    }

    public void setBytes(final byte[] bytes) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        storage.setBytes(bytes);
        derivedBigIntegerFromBytes();
    }
}
