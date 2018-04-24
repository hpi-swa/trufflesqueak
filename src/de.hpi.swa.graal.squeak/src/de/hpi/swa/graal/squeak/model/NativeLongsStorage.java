package de.hpi.swa.graal.squeak.model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

import de.hpi.swa.graal.squeak.exceptions.SqueakException;
import de.hpi.swa.graal.squeak.util.AbstractImageChunk;

public class NativeLongsStorage extends AbstractNativeObjectStorage {
    @CompilationFinal(dimensions = 1) protected long[] longs;

    public NativeLongsStorage(final int size) {
        longs = new long[size];
    }

    public NativeLongsStorage(final long[] longs) {
        this.longs = longs;
    }

    private NativeLongsStorage(final NativeLongsStorage original) {
        this(Arrays.copyOf(original.longs, original.longs.length));
    }

    @Override
    public AbstractNativeObjectStorage shallowCopy() {
        return new NativeLongsStorage(this);
    }

    @Override
    public void fillin(final AbstractImageChunk chunk) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        longs = chunk.getLongs();
    }

    @Override
    public long getNativeAt0(final long longIndex) {
        return longs[(int) longIndex];
    }

    @Override
    public void setNativeAt0(final long longIndex, final long value) {
        longs[(int) longIndex] = (int) value;
    }

    @Override
    public long shortAt0(final long index) {
        throw new SqueakException("Not yet implemented: shortAt0"); // TODO: implement
    }

    @Override
    public void shortAtPut0(final long longIndex, final long value) {
        throw new SqueakException("Not yet implemented: shortAtPut0"); // TODO: implement
    }

    @Override
    public void fillWith(final Object value) {
        Arrays.fill(longs, (long) value);
    }

    @Override
    public final int size() {
        return longs.length;
    }

    @Override
    public byte[] getBytes() {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(longs.length * 4);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        final LongBuffer longBuffer = byteBuffer.asLongBuffer();
        longBuffer.put(longs);
        return byteBuffer.array();
    }

    @Override
    public void setBytes(final byte[] bytes) {
        final int size = bytes.length / getElementSize();
        CompilerDirectives.transferToInterpreterAndInvalidate();
        longs = new long[size];
        for (int i = 0; i < longs.length; i++) {
            //@formatter:off
            longs[i] = (((long) bytes[i    ]) << 56) | (((long) bytes[i + 1]) << 48) | (((long) bytes[i + 2]) << 40) | (((long) bytes[i + 3]) << 32)
                     | (((long) bytes[i + 4]) << 24) | (((long) bytes[i + 5]) << 16) | (((long) bytes[i + 6]) << 8)  | bytes[i + 7];
            //@formatter:on
        }
    }

    @Override
    public byte getElementSize() {
        return 4;
    }
}
