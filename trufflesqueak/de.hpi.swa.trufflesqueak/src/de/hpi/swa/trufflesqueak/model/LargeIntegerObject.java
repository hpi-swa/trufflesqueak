package de.hpi.swa.trufflesqueak.model;

import java.math.BigInteger;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.util.SqueakImageChunk;

public class LargeIntegerObject extends SqueakObject {
    private BigInteger integer;

    public LargeIntegerObject(SqueakImageContext img) {
        super(img);
    }

    public LargeIntegerObject(SqueakImageContext img, ClassObject klass) {
        super(img, klass);
    }

    public LargeIntegerObject(SqueakImageContext img, BigInteger i) {
        super(img);
        ClassObject liKlass = img.largePositiveIntegerClass;
        if (i.compareTo(BigInteger.ZERO) < 0) {
            liKlass = img.largeNegativeIntegerClass;
        }
        setSqClass(liKlass);
        integer = i;
    }

    public LargeIntegerObject(SqueakImageContext img, ClassObject klass, byte[] bytes) {
        this(img, klass);
        setBytes(bytes);
    }

    @Override
    public void fillin(SqueakImageChunk chunk) {
        super.fillin(chunk);
        byte[] bytes = chunk.getBytes();
        setBytes(bytes);
    }

    @Override
    public Object at0(long l) {
        return byteAt0(integer.abs(), l);
    }

    @Override
    public void atput0(long idx, Object object) {
        byte b = (byte) object;
        setBytesNative(byteAtPut0(integer, idx, b));
    }

    public void setBytes(byte[] bytes) {
        byte[] bigEndianBytes = new byte[bytes.length + 1];
        bigEndianBytes[0] = 0;
        for (int i = 0; i < bytes.length; i++) {
            bigEndianBytes[bytes.length - i] = bytes[i];
        }
        setBytesNative(bigEndianBytes);
    }

    private void setBytesNative(byte[] bigEndianBytes) {
        integer = new BigInteger(bigEndianBytes);
        if (isNegative()) {
            integer = integer.negate();
        }
    }

    public byte[] getBytes() {
        return getSqueakBytes(integer);
    }

    private boolean isNegative() {
        return getSqClass() == image.largeNegativeIntegerClass;
    }

    @Override
    public int size() {
        return byteSize(integer);
    }

    @Override
    public int instsize() {
        return 0;
    }

    public BigInteger getValue() {
        return integer;
    }

    public static long byteAt0(BigInteger receiver, long index) {
        byte[] byteArray = receiver.toByteArray();
        return byteArray[byteArray.length - (int) index - 1] & 0xFF;
    }

    public static byte[] byteAtPut0(BigInteger receiver, long index, long value) {
        byte[] bytes = receiver.toByteArray();
        bytes[bytes.length - (int) index - 1] = (byte) value;
        return bytes;
    }

    public static byte[] getSqueakBytes(BigInteger repl) {
        byte[] bytes;
        // squeak large integers are unsigned, hence the abs call
        bytes = repl.abs().toByteArray();
        // the image expects little endian byte order
        for (int i = 0; i < bytes.length / 2; i++) {
            byte b = bytes[i];
            bytes[i] = bytes[bytes.length - i - 1];
            bytes[bytes.length - i - 1] = b;
        }
        return bytes;
    }

    public static int byteSize(BigInteger i) {
        return (i.abs().bitLength() + 7) / 8;
    }

    @Override
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
            return value.longValue();
        }
    }

    @TruffleBoundary
    public final Object reduceIfPossible() {
        return reduceIfPossible(integer);
    }

    @TruffleBoundary
    public final long reduceToLong() {
        return integer.longValueExact();
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
    public int signum() {
        return integer.signum();
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
