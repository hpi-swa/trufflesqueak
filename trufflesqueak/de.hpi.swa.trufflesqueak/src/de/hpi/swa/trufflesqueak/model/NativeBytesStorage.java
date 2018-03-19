package de.hpi.swa.trufflesqueak.model;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

import de.hpi.swa.trufflesqueak.util.SqueakImageChunk;

public class NativeBytesStorage extends NativeObjectStorage {
    @CompilationFinal(dimensions = 1) protected byte[] bytes;
    @CompilationFinal private static final long BYTE_MAX = (long) (Math.pow(2, Byte.SIZE) - 1);

    public NativeBytesStorage(int size) {
        bytes = new byte[size];
    }

    public NativeBytesStorage(byte[] bytes) {
        this.bytes = bytes;
    }

    protected NativeBytesStorage(NativeBytesStorage original) {
        this(Arrays.copyOf(original.bytes, original.bytes.length));
    }

    @Override
    public NativeObjectStorage shallowCopy() {
        return new NativeBytesStorage(this);
    }

    @Override
    public void fillin(SqueakImageChunk chunk) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        bytes = chunk.getBytes();
    }

    @Override
    public long getNativeAt0(long longIndex) {
        return Byte.toUnsignedLong(bytes[(int) longIndex]);
    }

    @Override
    public void setNativeAt0(long longIndex, long value) {
        if (value < 0 || value > BYTE_MAX) { // check for overflow
            throw new IllegalArgumentException("Illegal value for BytesObject: " + value);
        }
        bytes[(int) longIndex] = (byte) value;
    }

    @Override
    public long shortAt0(long index) {
        long offset = (index - 1) * 2;
        int byte0 = (byte) getNativeAt0(offset);
        int byte1 = (int) getNativeAt0(offset + 1) << 8;

        if ((byte1 & 0x8000) != 0) {
            byte1 = 0xffff0000 | byte1;
        }
        return byte1 | byte0;
    }

    @Override
    public void shortAtPut0(long index, long value) {
        Long byte0 = value & 0xff;
        Long byte1 = value & 0xff00;
        long offset = (index - 1) * 2;
        setNativeAt0(offset, byte0.byteValue());
        setNativeAt0(offset + 1, byte1.byteValue());
    }

    @Override
    public void fillWith(Object value) {
        if (value instanceof Long) {
            Arrays.fill(bytes, ((Long) value).byteValue());
        } else {
            Arrays.fill(bytes, (byte) value);
        }
    }

    @Override
    public final int size() {
        return bytes.length;
    }

    @Override
    public byte[] getBytes() {
        return bytes;
    }

    @Override
    public void setBytes(byte[] bytes) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        this.bytes = bytes;
    }

    @Override
    public void setByte(int index, byte value) {
        bytes[index] = value;
    }

    @Override
    public byte getElementSize() {
        return 1;
    }

// @Override
// public void setSqClass(ClassObject newCls) {
// if(newCls == image.largePositiveIntegerClass || this.getSqClass() ==
// image.largeNegativeIntegerClass)
// super.setSqClass(newCls);
// }
}
