package de.hpi.swa.trufflesqueak.model;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveExceptions;
import de.hpi.swa.trufflesqueak.exceptions.SqueakException;
import de.hpi.swa.trufflesqueak.util.SqueakImageChunk;

public class WordsObject extends NativeObject {
    @CompilationFinal(dimensions = 1) private int[] ints;
    @CompilationFinal private static final long INTEGER_MAX = (long) (Math.pow(2, Integer.SIZE) - 1);

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
        long word = (int) at0(wordIndex);
        if ((index - 1) % 2 == 0) {
            word = (word & 0xffff0000) | (value & 0xffff);
        } else {
            word = (value << 16) | (word & 0xffff);
        }
        atput0(wordIndex, word);
    }

    public int getInt(int index) {
        return ints[index];
    }

    public void setInt(int index, int value) {
        ints[index] = value;
    }

    @Override
    public boolean become(BaseSqueakObject other) {
        if (!(other instanceof WordsObject)) {
            throw new PrimitiveExceptions.PrimitiveFailed();
        }
        if (!super.become(other)) {
            throw new SqueakException("Should not fail");
        }
        CompilerDirectives.transferToInterpreterAndInvalidate();
        WordsObject otherWordsObject = ((WordsObject) other);
        int[] otherWords = otherWordsObject.ints;
        otherWordsObject.ints = this.ints;
        this.ints = otherWords;
        return true;
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
}
