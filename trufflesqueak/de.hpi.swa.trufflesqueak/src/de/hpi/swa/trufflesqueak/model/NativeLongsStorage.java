package de.hpi.swa.trufflesqueak.model;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

import de.hpi.swa.trufflesqueak.exceptions.SqueakException;
import de.hpi.swa.trufflesqueak.util.SqueakImageChunk;

public class NativeLongsStorage extends NativeObjectStorage {
    @CompilationFinal(dimensions = 1) protected long[] longs;

    public NativeLongsStorage(int size) {
        longs = new long[size];
    }

    public NativeLongsStorage(long[] longs) {
        this.longs = longs;
    }

    private NativeLongsStorage(NativeLongsStorage original) {
        this(Arrays.copyOf(original.longs, original.longs.length));
    }

    @Override
    public NativeObjectStorage shallowCopy() {
        return new NativeLongsStorage(this);
    }

    @Override
    public void fillin(SqueakImageChunk chunk) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        longs = chunk.getLongs();
    }

    @Override
    public long getNativeAt0(long longIndex) {
        return longs[(int) longIndex];
    }

    @Override
    public void setNativeAt0(long longIndex, long value) {
        longs[(int) longIndex] = (int) value;
    }

    @Override
    public long shortAt0(long index) {
        throw new SqueakException("Not yet implemented: shortAt0"); // TODO: implement
    }

    @Override
    public void shortAtPut0(long longIndex, long value) {
        throw new SqueakException("Not yet implemented: shortAtPut0"); // TODO: implement
    }

    @Override
    public void fillWith(Object value) {
        Arrays.fill(longs, (long) value);
    }

    @Override
    public final int size() {
        return longs.length;
    }

    @Override
    public byte[] getBytes() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(longs.length * 4);
        LongBuffer longBuffer = byteBuffer.asLongBuffer();
        longBuffer.put(longs);
        return byteBuffer.array();
    }

    @Override
    public void setBytes(byte[] bytes) {
        final int size = bytes.length / getElementSize();
        CompilerDirectives.transferToInterpreterAndInvalidate();
        longs = new long[size];
        for (int i = 0; i < longs.length; i++) {
            //@formatter:off
            longs[i] = ((bytes[i + 7]) << 56) | ((bytes[i + 6]) << 48) | ((bytes[i + 5]) << 40) | ((bytes[i + 4]) << 32)
                     | ((bytes[i + 3]) << 24) | ((bytes[i + 2]) << 16) | ((bytes[i + 1]) << 8)  | bytes[i];
            //@formatter:on
        }
    }

    @Override
    public byte getElementSize() {
        return 4;
    }
}
