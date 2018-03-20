package de.hpi.swa.trufflesqueak.model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

import de.hpi.swa.trufflesqueak.util.SqueakImageChunk;

public class NativeWordsStorage extends AbstractNativeObjectStorage {
    @CompilationFinal(dimensions = 1) protected int[] ints;
    @CompilationFinal private static final long INTEGER_MAX = (long) (Math.pow(2, Integer.SIZE) - 1);

    public NativeWordsStorage(int size) {
        ints = new int[size];
    }

    public NativeWordsStorage(int[] ints) {
        this.ints = ints;
    }

    private NativeWordsStorage(NativeWordsStorage original) {
        this(Arrays.copyOf(original.ints, original.ints.length));
    }

    @Override
    public AbstractNativeObjectStorage shallowCopy() {
        return new NativeWordsStorage(this);
    }

    @Override
    public void fillin(SqueakImageChunk chunk) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        ints = chunk.getWords();
    }

    @Override
    public long getNativeAt0(long index) {
        return Integer.toUnsignedLong(ints[(int) index]);
    }

    @Override
    public void setNativeAt0(long longIndex, long value) {
        if (value < 0 || value > INTEGER_MAX) { // check for overflow
            throw new IllegalArgumentException("Illegal value for WordsObject: " + value);
        }
        ints[(int) longIndex] = (int) value;
    }

    @Override
    public long shortAt0(long index) {
        int word = ints[((int) index - 1) / 2];
        int shortValue;
        if ((index - 1) % 2 == 0) {
            shortValue = word & 0xffff;
        } else {
            shortValue = (word >> 16) & 0xffff;
        }
        if ((shortValue & 0x8000) != 0) {
            shortValue = 0xffff0000 | shortValue;
        }
        return shortValue;
    }

    @Override
    public void shortAtPut0(long index, long value) {
        long wordIndex = (index - 1) / 2;
        long word = (int) getNativeAt0(wordIndex);
        if ((index - 1) % 2 == 0) {
            word = (word & 0xffff0000) | (value & 0xffff);
        } else {
            word = (value << 16) | (word & 0xffff);
        }
        setNativeAt0(wordIndex, word);
    }

    @Override
    public int getInt(int index) {
        return ints[index];
    }

    @Override
    public void setInt(int index, int value) {
        ints[index] = value;
    }

    @Override
    public void fillWith(Object value) {
        if (value instanceof Long) {
            Arrays.fill(ints, ((Long) value).intValue());
        } else {
            Arrays.fill(ints, (byte) value);
        }
    }

    @Override
    public final int size() {
        return ints.length;
    }

    @Override
    public byte[] getBytes() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(ints.length * 4);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        IntBuffer intBuffer = byteBuffer.asIntBuffer();
        intBuffer.put(ints);
        return byteBuffer.array();
    }

    @Override
    public void setBytes(byte[] bytes) {
        final int size = bytes.length / getElementSize();
        CompilerDirectives.transferToInterpreterAndInvalidate();
        ints = new int[size];
        for (int i = 0; i < ints.length; i++) {
            ints[i] = ((bytes[i + 1]) << 24) | ((bytes[i + 2]) << 16) | ((bytes[i + 3]) << 8) | bytes[i];
        }
    }

    @Override
    public int[] getWords() {
        return ints;
    }

    @Override
    public byte getElementSize() {
        return 4;
    }
}
