package de.hpi.swa.trufflesqueak.model;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

import de.hpi.swa.trufflesqueak.util.SqueakImageChunk;

public class NativeShortsStorage extends NativeObjectStorage {
    @CompilationFinal(dimensions = 1) protected short[] shorts;
    @CompilationFinal private static final long SHORT_MAX = (long) (Math.pow(2, Short.SIZE) - 1);

    public NativeShortsStorage(int size) {
        shorts = new short[size];
    }

    public NativeShortsStorage(short[] shorts) {
        this.shorts = shorts;
    }

    private NativeShortsStorage(NativeShortsStorage original) {
        this(Arrays.copyOf(original.shorts, original.shorts.length));
    }

    @Override
    public NativeObjectStorage shallowCopy() {
        return new NativeShortsStorage(this);
    }

    @Override
    public void fillin(SqueakImageChunk chunk) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        shorts = chunk.getShorts();
    }

    @Override
    public long getNativeAt0(long longIndex) {
        return Short.toUnsignedLong(shorts[(int) longIndex]);
    }

    @Override
    public void setNativeAt0(long longIndex, long value) {
        if (value < 0 || value > SHORT_MAX) { // check for overflow
            throw new IllegalArgumentException("Illegal value for ShortsObject: " + value);
        }
        shorts[(int) longIndex] = (short) value;
    }

    @Override
    public long shortAt0(long index) {
        return getNativeAt0(index);
    }

    @Override
    public void shortAtPut0(long index, long value) {
        setNativeAt0(index, value);
    }

    @Override
    public void fillWith(Object value) {
        if (value instanceof Long) {
            Arrays.fill(shorts, ((Long) value).shortValue());
        } else {
            Arrays.fill(shorts, (short) value);
        }
    }

    @Override
    public final int size() {
        return shorts.length;
    }

    @Override
    public byte[] getBytes() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(shorts.length * 4);
        ShortBuffer shortBuffer = byteBuffer.asShortBuffer();
        shortBuffer.put(shorts);
        return byteBuffer.array();
    }

    @Override
    public void setBytes(byte[] bytes) {
        final int size = bytes.length / getElementSize();
        CompilerDirectives.transferToInterpreterAndInvalidate();
        shorts = new short[size];
        for (int i = 0; i < shorts.length; i++) {
            shorts[i] = (short) (((bytes[i + 1]) << 8) | bytes[i]);
        }
    }

    @Override
    public byte getElementSize() {
        return 2;
    }
}
