package de.hpi.swa.graal.squeak.model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

import de.hpi.swa.graal.squeak.image.AbstractImageChunk;

public class NativeShortsStorage extends AbstractNativeObjectStorage {
    @CompilationFinal(dimensions = 1) protected short[] shorts;
    @CompilationFinal private static final long SHORT_MAX = (long) (Math.pow(2, Short.SIZE) - 1);

    public NativeShortsStorage(final int size) {
        shorts = new short[size];
    }

    public NativeShortsStorage(final short[] shorts) {
        this.shorts = shorts;
    }

    private NativeShortsStorage(final NativeShortsStorage original) {
        this(Arrays.copyOf(original.shorts, original.shorts.length));
    }

    @Override
    public AbstractNativeObjectStorage shallowCopy() {
        return new NativeShortsStorage(this);
    }

    @Override
    public void fillin(final AbstractImageChunk chunk) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        shorts = chunk.getShorts();
    }

    @Override
    public long getNativeAt0(final long longIndex) {
        return Short.toUnsignedLong(shorts[(int) longIndex]);
    }

    @Override
    public void setNativeAt0(final long longIndex, final long value) {
        if (value < 0 || value > SHORT_MAX) { // check for overflow
            throw new IllegalArgumentException("Illegal value for ShortsObject: " + value);
        }
        shorts[(int) longIndex] = (short) value;
    }

    @Override
    public long shortAt0(final long index) {
        return getNativeAt0(index);
    }

    @Override
    public void shortAtPut0(final long index, final long value) {
        setNativeAt0(index, value);
    }

    @Override
    public void fillWith(final Object value) {
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
        final ByteBuffer byteBuffer = ByteBuffer.allocate(shorts.length * 4);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        final ShortBuffer shortBuffer = byteBuffer.asShortBuffer();
        shortBuffer.put(shorts);
        return byteBuffer.array();
    }

    @Override
    public void setBytes(final byte[] bytes) {
        final int size = bytes.length / getElementSize();
        CompilerDirectives.transferToInterpreterAndInvalidate();
        shorts = new short[size];
        for (int i = 0; i < shorts.length; i++) {
            shorts[i] = (short) (((bytes[i] & 0xFF) << 8) | (bytes[i + 1] & 0xFF));
        }
    }

    @Override
    public byte getElementSize() {
        return 2;
    }
}
