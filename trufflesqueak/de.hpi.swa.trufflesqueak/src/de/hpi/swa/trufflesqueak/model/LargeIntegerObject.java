package de.hpi.swa.trufflesqueak.model;

import java.math.BigInteger;
import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.util.SqueakImageChunk;

public class LargeIntegerObject extends BytesObject {
    @CompilationFinal public static final long SMALL_INTEGER_MIN = -0x40000000;
    @CompilationFinal public static final long SMALL_INTEGER_MAX = 0x3fffffff;

    @CompilationFinal private BigInteger integer;

    public LargeIntegerObject(SqueakImageContext img) {
        super(img);
    }

    public LargeIntegerObject(SqueakImageContext img, BigInteger integer) {
        super(img, integer.compareTo(BigInteger.ZERO) >= 0 ? img.largePositiveIntegerClass : img.largeNegativeIntegerClass);
        this.integer = integer;
        this.bytes = swapInPlaceOrder(integer.abs().toByteArray());
    }

    public LargeIntegerObject(SqueakImageContext img, ClassObject klass, byte[] bytes) {
        super(img, klass, bytes);
        derivedBigIntegerFromBytes();
    }

    public LargeIntegerObject(SqueakImageContext image, ClassObject klass, int size) {
        super(image, klass, size);
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
        byte[] bigEndianBytes = swapOrder(bytes);
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

    private static byte[] swapOrder(byte[] bytes) {
        return swapInPlaceOrder(Arrays.copyOf(bytes, bytes.length));
    }

    private static byte[] swapInPlaceOrder(byte[] bytes) {
        for (int i = 0; i < bytes.length / 2; i++) {
            byte b = bytes[i];
            bytes[i] = bytes[bytes.length - 1 - i];
            bytes[bytes.length - 1 - i] = b;
        }
        return bytes;
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
            LargeIntegerObject otherLargeInteger = (LargeIntegerObject) other;
            return integer.equals(otherLargeInteger.integer) && getSqClass() == otherLargeInteger.getSqClass();
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
            return value.longValue() & 0xffffffffffffffffL;
        }
    }

    @TruffleBoundary
    public final Object reduceIfPossible() {
        return reduceIfPossible(integer);
    }

    @TruffleBoundary
    public final long reduceToLong() throws ArithmeticException {
        return integer.longValueExact() & 0xffffffffffffffffL;
    }

    private final LargeIntegerObject newFromBigInteger(BigInteger value) {
        return newFromBigInteger(image, value);
    }

    private final static LargeIntegerObject newFromBigInteger(SqueakImageContext image, BigInteger value) {
        return new LargeIntegerObject(image, value);
    }

    @TruffleBoundary
    public static LargeIntegerObject valueOf(CompiledCodeObject code, long a) {
        return newFromBigInteger(code.image, BigInteger.valueOf(a));
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
    public Object mod(LargeIntegerObject b) {
        return reduceIfPossible(integer.mod(b.integer.abs()));
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
        this.bytes = bytes;
        derivedBigIntegerFromBytes();
    }
}
