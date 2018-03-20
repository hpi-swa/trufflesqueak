package de.hpi.swa.trufflesqueak.model;

import java.math.BigInteger;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.SqueakImageChunk;

public class LargeIntegerObject extends NativeObject {
    @CompilationFinal public static final long SMALLINTEGER32_MIN = -0x40000000;
    @CompilationFinal public static final long SMALLINTEGER32_MAX = 0x3fffffff;
    @CompilationFinal public static final long SMALLINTEGER64_MIN = -0x1000000000000000L;
    @CompilationFinal public static final long SMALLINTEGER64_MAX = 0xfffffffffffffffL;
    @CompilationFinal public final static long MASK_32BIT = 0xffffffffL;
    @CompilationFinal public final static long MASK_64BIT = 0xffffffffffffffffL;

    @CompilationFinal private BigInteger integer;

    public LargeIntegerObject(SqueakImageContext img) {
        super(img, null, new NativeBytesStorage(0));
    }

    public LargeIntegerObject(SqueakImageContext img, BigInteger integer) {
        super(img, integer.compareTo(BigInteger.ZERO) >= 0 ? img.largePositiveIntegerClass : img.largeNegativeIntegerClass);
        this.integer = integer;
        byte[] byteArray;
        byte[] array = integer.abs().toByteArray();
        int size = (integer.bitLength() + 7) / 8;
        if (array.length > size) {
            byteArray = new byte[size];
            int offset = array.length - size;
            for (int i = 0; i < byteArray.length; i++) {
                byteArray[i] = array[offset + i];
            }
        } else {
            assert array.length == size;
            byteArray = array;
        }
        this.storage = new NativeBytesStorage(ArrayUtils.swapOrderInPlace(byteArray));
    }

    public LargeIntegerObject(SqueakImageContext img, ClassObject klass, byte[] bytes) {
        super(img, klass);
        this.storage = new NativeBytesStorage(bytes);
        derivedBigIntegerFromBytes();
    }

    public LargeIntegerObject(SqueakImageContext image, ClassObject klass, int size) {
        super(image, klass);
        this.storage = new NativeBytesStorage(size);
        integer = BigInteger.ZERO;
    }

    public LargeIntegerObject(LargeIntegerObject original) {
        super(original);
        integer = original.integer;
    }

    @Override
    public void fillin(SqueakImageChunk chunk) {
        super.fillin(chunk);
        derivedBigIntegerFromBytes();
    }

    @Override
    public void setNativeAt0(long index, long object) {
        super.setNativeAt0(index, object);
        derivedBigIntegerFromBytes();
    }

    private void derivedBigIntegerFromBytes() {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        byte[] bigEndianBytes = ArrayUtils.swapOrderCopy(storage.getBytes());
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
    public static int byteSize(BigInteger value) {
        return value.abs().toByteArray().length;
    }

    @Override
    @TruffleBoundary
    public String toString() {
        return integer.toString();
    }

    @Override
    public boolean equals(Object other) {
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
    protected final Object reduceIfPossible(BigInteger value) {
        if (value.bitLength() > Long.SIZE - 1) {
            return newFromBigInteger(value);
        } else {
            return value.longValue() & MASK_64BIT;
        }
    }

    @TruffleBoundary
    public final Object reduceIfPossible() {
        return reduceIfPossible(integer);
    }

    @TruffleBoundary
    public final long reduceToLong() throws ArithmeticException {
        return integer.longValueExact() & MASK_64BIT;
    }

    private final LargeIntegerObject newFromBigInteger(BigInteger value) {
        return newFromBigInteger(image, value);
    }

    private final static LargeIntegerObject newFromBigInteger(SqueakImageContext image, BigInteger value) {
        return new LargeIntegerObject(image, value);
    }

    @TruffleBoundary
    public static LargeIntegerObject valueOf(SqueakImageContext image, long a) {
        return newFromBigInteger(image, BigInteger.valueOf(a));
    }

    /*
     * Arithmetic Operations
     */

    @TruffleBoundary
    public Object add(LargeIntegerObject b) {
        return reduceIfPossible(integer.add(b.integer));
    }

    @TruffleBoundary
    public Object subtract(LargeIntegerObject b) {
        return reduceIfPossible(integer.subtract(b.integer));
    }

    @TruffleBoundary
    public Object multiply(LargeIntegerObject b) {
        return reduceIfPossible(integer.multiply(b.integer));
    }

    @TruffleBoundary
    public Object divide(LargeIntegerObject b) {
        return reduceIfPossible(integer.divide(b.integer));
    }

    @TruffleBoundary
    public Object floorDivide(LargeIntegerObject b) {
        return reduceIfPossible(floorDivide(integer, b.integer));
    }

    private static BigInteger floorDivide(BigInteger x, BigInteger y) {
        BigInteger r = x.divide(y);
        // if the signs are different and modulo not zero, round down
        if (x.signum() != y.signum() && !r.multiply(y).equals(x)) {
            r = r.subtract(BigInteger.ONE);
        }
        return r;
    }

    @TruffleBoundary
    public Object floorMod(LargeIntegerObject b) {
        return reduceIfPossible(integer.subtract(floorDivide(integer, b.integer).multiply(b.integer)));
    }

    @TruffleBoundary
    public LargeIntegerObject divideNoReduce(LargeIntegerObject b) {
        return newFromBigInteger(integer.divide(b.integer));
    }

    @TruffleBoundary
    public Object remainder(LargeIntegerObject b) {
        return reduceIfPossible(integer.remainder(b.integer));
    }

    @TruffleBoundary
    public LargeIntegerObject negateNoReduce() {
        return newFromBigInteger(integer.negate());
    }

    @TruffleBoundary
    public int compareTo(LargeIntegerObject b) {
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
    public Object and(LargeIntegerObject b) {
        return reduceIfPossible(integer.and(b.integer));
    }

    @TruffleBoundary
    public Object or(LargeIntegerObject b) {
        return reduceIfPossible(integer.or(b.integer));
    }

    @TruffleBoundary
    public Object xor(LargeIntegerObject b) {
        return reduceIfPossible(integer.xor(b.integer));
    }

    @TruffleBoundary
    public Object shiftLeft(int b) {
        return reduceIfPossible(integer.shiftLeft(b));
    }

    @TruffleBoundary
    public Object shiftRight(int b) {
        return reduceIfPossible(integer.shiftRight(b));
    }

    @Override
    public void setByte(int index, byte value) {
        super.setByte(index, value);
        derivedBigIntegerFromBytes();
    }

    public void setBytes(byte[] bytes) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        storage.setBytes(bytes);
        derivedBigIntegerFromBytes();
    }
}
