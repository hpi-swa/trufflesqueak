package de.hpi.swa.graal.squeak.model;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

import de.hpi.swa.graal.squeak.image.AbstractImageChunk;

public class NativeBytesStorage extends AbstractNativeObjectStorage {
    @CompilationFinal(dimensions = 1) protected byte[] bytes;
    @CompilationFinal private static final long BYTE_MAX = (long) (Math.pow(2, Byte.SIZE) - 1);

    public NativeBytesStorage(final int size) {
        bytes = new byte[size];
    }

    public NativeBytesStorage(final byte[] bytes) {
        this.bytes = bytes;
    }

    protected NativeBytesStorage(final NativeBytesStorage original) {
        this(Arrays.copyOf(original.bytes, original.bytes.length));
    }

    @Override
    public AbstractNativeObjectStorage shallowCopy() {
        return new NativeBytesStorage(this);
    }

    @Override
    public void fillin(final AbstractImageChunk chunk) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        bytes = chunk.getBytes();
    }

    @Override
    public long getNativeAt0(final long longIndex) {
        return Byte.toUnsignedLong(bytes[(int) longIndex]);
    }

    @Override
    public void setNativeAt0(final long longIndex, final long value) {
        if (value < 0 || value > BYTE_MAX) { // check for overflow
            throw new IllegalArgumentException("Illegal value for BytesObject: " + value);
        }
        bytes[(int) longIndex] = (byte) value;
    }

    @Override
    public long shortAt0(final long index) {
        final long offset = (index - 1) * 2;
        final int byte0 = (byte) getNativeAt0(offset);
        int byte1 = (int) getNativeAt0(offset + 1) << 8;

        if ((byte1 & 0x8000) != 0) {
            byte1 = 0xffff0000 | byte1;
        }
        return byte1 | byte0;
    }

    @Override
    public void shortAtPut0(final long index, final long value) {
        final Long byte0 = value & 0xff;
        final Long byte1 = value & 0xff00;
        final long offset = (index - 1) * 2;
        setNativeAt0(offset, byte0.byteValue());
        setNativeAt0(offset + 1, byte1.byteValue());
    }

    @Override
    public void fillWith(final Object value) {
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
    public void setBytes(final byte[] bytes) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        this.bytes = bytes;
    }

    @Override
    public void setByte(final int index, final byte value) {
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
