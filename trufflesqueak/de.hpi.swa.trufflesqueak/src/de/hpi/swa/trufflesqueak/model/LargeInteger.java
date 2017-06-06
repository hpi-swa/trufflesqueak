package de.hpi.swa.trufflesqueak.model;

import java.math.BigInteger;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.util.Chunk;

public class LargeInteger extends SqueakObject {
    private BigInteger integer;

    public LargeInteger(SqueakImageContext img) {
        super(img);
    }

    public LargeInteger(SqueakImageContext img, ClassObject klass) {
        super(img, klass);
    }

    public LargeInteger(SqueakImageContext img, BigInteger i) {
        super(img);
        ClassObject liKlass = img.largePositiveIntegerClass;
        if (i.compareTo(BigInteger.ZERO) < 0) {
            liKlass = img.largeNegativeIntegerClass;
        }
        setSqClass(liKlass);
        integer = i;
    }

    public LargeInteger(SqueakImageContext img, ClassObject klass, byte[] bytes) {
        this(img, klass);
        setBytes(bytes);
    }

    @Override
    public void fillin(Chunk chunk) {
        super.fillin(chunk);
        byte[] bytes = chunk.getBytes();
        setBytes(bytes);
    }

    @Override
    public Object at0(int l) {
        return byteAt0(integer.abs(), l);
    }

    @Override
    public void atput0(int idx, Object object) {
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

    public static long byteAt0(BigInteger receiver, int idx) {
        byte[] byteArray = receiver.toByteArray();
        return byteArray[byteArray.length - idx - 1] & 0xFF;
    }

    public static byte[] byteAtPut0(BigInteger receiver, int idx, long value) {
        byte[] bytes = receiver.toByteArray();
        bytes[bytes.length - idx - 1] = (byte) value;
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

    public BigInteger unwrapBigInt() {
        return getValue();
    }

    public int unwrapInt() throws ArithmeticException {
        return getValue().intValueExact();
    }

    public long unwrapLong() throws ArithmeticException {
        return getValue().longValueExact();
    }

    @Override
    public BaseSqueakObject shallowCopy() {
        return new LargeInteger(image, integer);
    }
}
