package de.hpi.swa.trufflesqueak.model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.util.SqueakImageChunk;

public class WordsObject extends NativeObject {
    @CompilationFinal(dimensions = 1) private int[] ints;

    public WordsObject(SqueakImageContext image) {
        super(image);
    }

    public WordsObject(SqueakImageContext image, ClassObject classObject, int size) {
        super(image, classObject);
        ints = new int[size];
    }

    public WordsObject(SqueakImageContext image, ClassObject classObject, int[] ints) {
        super(image, classObject);
        this.ints = ints;
    }

    private WordsObject(WordsObject original) {
        this(original.image, original.getSqClass(), Arrays.copyOf(original.ints, original.ints.length));
    }

    @Override
    public BaseSqueakObject shallowCopy() {
        return new WordsObject(this);
    }

    @Override
    public void fillin(SqueakImageChunk chunk) {
        super.fillin(chunk);
        CompilerDirectives.transferToInterpreterAndInvalidate();
        ints = chunk.getWords();
    }

    @Override
    public long getNativeAt0(long index) {
        return Integer.toUnsignedLong(ints[(int) index]);
    }

    @Override
    public void setNativeAt0(long longIndex, long value) {
        ints[(int) longIndex] = (int) value;
    }

    public int getInt(long index) {
        return ints[(int) index];
    }

    public void setInt(long index, int value) {
        ints[(int) index] = value;
    }

    @Override
    public boolean become(BaseSqueakObject other) {
        if (other instanceof WordsObject) {
            if (super.become(other)) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                WordsObject otherWordsObject = ((WordsObject) other);
                int[] otherWords = otherWordsObject.ints;
                otherWordsObject.ints = this.ints;
                this.ints = otherWords;
                return true;
            }
        }
        return false;
    }

    @Override
    public final int size() {
        return ints.length;
    }

    @Override
    public byte[] getBytes() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(ints.length * 4);
        IntBuffer intBuffer = byteBuffer.asIntBuffer();
        intBuffer.put(ints);
        return byteBuffer.array();
    }

    public int[] getWords() {
        return ints;
    }

    @Override
    public byte getElementSize() {
        return 4;
    }

    public static double bytesAsFloatObject(byte[] someBytes) {
        ByteBuffer buf = ByteBuffer.allocate(8); // 2 * 32 bit
        buf.order(ByteOrder.nativeOrder());
        buf.put(someBytes);
        buf.rewind();
        long low = Integer.toUnsignedLong(buf.asIntBuffer().get(0));
        long high = Integer.toUnsignedLong(buf.asIntBuffer().get(1));
        return Double.longBitsToDouble(high << 32 | low);
    }

    public static double newFloatObject(int size) {
        byte[] someBytes = new byte[size];
        for (int i = 0; i < someBytes.length; i++) {
            someBytes[i] = 127; // max byte value to ensure double has enough slots
        }
        return bytesAsFloatObject(someBytes);
    }
}
