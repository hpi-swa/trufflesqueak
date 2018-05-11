package de.hpi.swa.graal.squeak.model;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

import de.hpi.swa.graal.squeak.image.AbstractImageChunk;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.storages.NativeWordsStorage;

public final class FloatObject extends AbstractSqueakObject {
    @CompilationFinal public static final int PRECISION = 53;
    @CompilationFinal public static final int EMIN = -1022;
    @CompilationFinal public static final int EMAX = 1023;
    @CompilationFinal private static final int WORD_LENGTH = 2;

    @CompilationFinal private double doubleValue;

    public static FloatObject valueOf(final SqueakImageContext image, final double value) {
        return new FloatObject(image, value);
    }

    public FloatObject(final SqueakImageContext image) {
        super(image, image.floatClass);
    }

    public FloatObject(final FloatObject original) {
        super(original.image, original.getSqClass());
        doubleValue = original.doubleValue;
    }

    public FloatObject(final SqueakImageContext image, final double doubleValue) {
        this(image);
        this.doubleValue = doubleValue;
    }

    public FloatObject(final SqueakImageContext image, final long high, final long low) {
        this(image);
        setWords(high, low);
    }

    @Override
    public void fillin(final AbstractImageChunk chunk) {
        super.fillin(chunk);
        final int[] words = chunk.getWords();
        assert words.length == WORD_LENGTH;
        setWords(words[1], words[0]);
    }

    public long getNativeAt0(final long index) {
        final Long bits = Double.doubleToRawLongBits(doubleValue);
        if (index == 0) {
            return Integer.toUnsignedLong((int) (bits >> 32));
        } else if (index == 1) {
            return Integer.toUnsignedLong(bits.intValue());
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    public void setNativeAt0(final long index, final long value) {
        if (value < 0 || value > NativeWordsStorage.INTEGER_MAX) { // check for overflow
            throw new IllegalArgumentException("Illegal value for FloatObject: " + value);
        }
        if (index == 0) {
            setWords(value, getNativeAt0(1));
        } else if (index == 1) {
            setWords(getNativeAt0(0), value);
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    private void setWords(final long high, final long low) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        final long highMasked = high & LargeIntegerObject.MASK_32BIT;
        final long lowMasked = low & LargeIntegerObject.MASK_32BIT;
        this.doubleValue = Double.longBitsToDouble(((highMasked) << 32) | lowMasked);
    }

    public void setBytes(final byte[] bytes) {
        final int[] ints = bytesToInts(bytes);
        setWords(ints[0], ints[1]);
    }

    public byte[] getBytes() {
        final long bits = Double.doubleToRawLongBits(doubleValue);
        final byte[] bytes = new byte[WORD_LENGTH * 4];
        bytes[0] = (byte) (bits >> 56);
        bytes[1] = (byte) (bits >> 48);
        bytes[2] = (byte) (bits >> 40);
        bytes[3] = (byte) (bits >> 32);
        bytes[4] = (byte) (bits >> 24);
        bytes[5] = (byte) (bits >> 16);
        bytes[6] = (byte) (bits >> 8);
        bytes[7] = (byte) bits;
        return bytes;
    }

    public static int size() {
        return WORD_LENGTH;
    }

    public double getValue() {
        return doubleValue;
    }

    @Override
    public String toString() {
        return "" + doubleValue;
    }

    public AbstractSqueakObject shallowCopy() {
        return new FloatObject(this);
    }

    private static int[] bytesToInts(final byte[] bytes) {
        assert bytes.length == WORD_LENGTH * 4;
        final int[] ints = new int[2];
        ints[0] = ((bytes[3] & 0xff) << 24) | ((bytes[2] & 0xff) << 16) | ((bytes[1] & 0xff) << 8) | bytes[0] & 0xff;
        ints[1] = ((bytes[7] & 0xff) << 24) | ((bytes[6] & 0xff) << 16) | ((bytes[5] & 0xff) << 8) | bytes[4] & 0xff;
        return ints;
    }

    public static FloatObject bytesAsFloatObject(final SqueakImageContext image, final byte[] bytes) {
        final int[] ints = bytesToInts(bytes);
        return new FloatObject(image, ints[1], ints[0]);
    }
}
