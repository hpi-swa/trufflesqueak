package de.hpi.swa.trufflesqueak.model;

import java.math.BigInteger;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.util.SqueakImageChunk;

public class LargeIntegerObject extends SqueakObject {
    @CompilationFinal public static final long SMALL_INTEGER_MIN = -0x40000000;
    @CompilationFinal public static final long SMALL_INTEGER_MAX = 0x3fffffff;

    @CompilationFinal private int size = -1;
    @CompilationFinal private BigInteger integer;

    public LargeIntegerObject(SqueakImageContext img) {
        super(img);
    }

    public LargeIntegerObject(SqueakImageContext img, BigInteger i) {
        super(img, i.compareTo(BigInteger.ZERO) >= 0 ? img.largePositiveIntegerClass : img.largeNegativeIntegerClass);
        integer = i;
        size = byteSize(integer);
    }

    public LargeIntegerObject(SqueakImageContext img, ClassObject klass, byte[] bytes) {
        super(img, klass);
        setBytes(bytes);
    }

    public LargeIntegerObject(SqueakImageContext image, ClassObject klass, int size) {
        super(image, klass);
        setBytes(new byte[size]);
    }

    @Override
    public void fillin(SqueakImageChunk chunk) {
        super.fillin(chunk);
        byte[] bytes = chunk.getBytes();
        setBytes(bytes);
    }

    @Override
    public Object at0(long index) {
        return byteAt0(index);
    }

    @Override
    public void atput0(long index, Object object) {
        assert index >= 0;
        if (index >= size) {
            throw new ArrayIndexOutOfBoundsException();
        }
        byte[] bytes = integer.toByteArray();
        if (index > bytes.length - 1) { // need to enlarge ByteArray
            assert size > bytes.length : "ByteArray must be smaller than size of LargeInteger";
            int offset = size - bytes.length;
            byte[] largerBytes = new byte[size];
            for (int i = 0; i < bytes.length; i++) {
                largerBytes[i + offset] = bytes[i];
            }
            bytes = largerBytes;
        }
        byte value = object instanceof Long ? ((Long) object).byteValue() : (byte) object;
        bytes[bytes.length - 1 - (int) index] = value;
        setBytesNative(bytes);
    }

    public void setBytes(byte[] bytes) {
        setBytesNative(swapOrder(bytes));
    }

    private void setBytesNative(byte[] bigEndianBytes) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        if (size < 0) {
            size = bigEndianBytes.length;
        }
        if (bigEndianBytes.length == 0) {
            integer = BigInteger.valueOf(0);
        } else {
            integer = new BigInteger(bigEndianBytes).and(BigInteger.valueOf(1).shiftLeft(bigEndianBytes.length * 8).subtract(BigInteger.valueOf(1)));
        }
    }

    private long byteAt0(long index) {
        assert index >= 0;
        if (size >= 0 && index >= size) {
            throw new ArrayIndexOutOfBoundsException("Tried to access LargeInteger with size: " + size + " at " + index);
        }
        byte[] byteArray = integer.toByteArray();
        try {
            return byteArray[byteArray.length - (int) index - 1] & 0xFF;
        } catch (ArrayIndexOutOfBoundsException e) {
            return 0;
        }
    }

    public byte[] getBytes() {
        byte[] nonZeroBytes = integer.toByteArray();
        byte[] bytes = new byte[size >= 0 ? size : nonZeroBytes.length];
        // the image expects little endian byte order
        for (int i = 0; i < nonZeroBytes.length; i++) {
            bytes[bytes.length - 1 - i] = nonZeroBytes[i];
        }
        return bytes;
    }

    public boolean isNegative() {
        return getSqClass() == image.largeNegativeIntegerClass;
    }

    private static byte[] swapOrder(byte[] bytes) {
        for (int i = 0; i < bytes.length / 2; i++) {
            byte b = bytes[i];
            bytes[i] = bytes[bytes.length - 1 - i];
            bytes[bytes.length - 1 - i] = b;
        }
        return bytes;
    }

    @Override
    public final int size() {
        if (size < 0) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            size = byteSize(integer);
        }
        return size;
    }

    @Override
    public final int instsize() {
        return 0;
    }

    public BigInteger getValue() {
        return integer;
    }

    @TruffleBoundary
    public int byteSize(BigInteger value) {
        return value.toByteArray().length;
    }

    @Override
    @TruffleBoundary
    public String toString() {
        return integer.toString();
    }

    @Override
    public boolean equals(Object b) {
        if (b instanceof LargeIntegerObject) {
            return integer.equals(((LargeIntegerObject) b).integer);
        } else {
            return super.equals(b);
        }
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public BaseSqueakObject shallowCopy() {
        return new LargeIntegerObject(image, integer);
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
}
