package de.hpi.swa.trufflesqueak.model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.exceptions.SqueakException;
import de.hpi.swa.trufflesqueak.util.SqueakImageChunk;

public class FloatObject extends NativeObject {
    @CompilationFinal public static final int PRECISION = 53;
    @CompilationFinal public static final int EMIN = -1022;
    @CompilationFinal public static final int EMAX = 1023;

    @CompilationFinal private double value;

    public static FloatObject valueOf(SqueakImageContext image, double value) {
        return new FloatObject(image, value);
    }

    public FloatObject(SqueakImageContext image) {
        super(image, image.floatClass, new NativeWordsStorage(2));
    }

    public FloatObject(FloatObject original) {
        super(original.image, original.getSqClass(), original.storage.shallowCopy());
        this.value = original.value;
    }

    public FloatObject(SqueakImageContext image, double value) {
        this(image);
        long doubleBits = Double.doubleToLongBits(value);
        long high = doubleBits >> 32;
        long low = doubleBits & LargeIntegerObject.MASK_32BIT;
        setWords(high, low);
        assert this.value == value || Double.isNaN(value);
    }

    public FloatObject(SqueakImageContext image, long high, long low) {
        this(image);
        setWords(high, low);
    }

    @Override
    public void fillin(SqueakImageChunk chunk) {
        super.fillin(chunk);
        int[] words = chunk.getWords();
        assert words.length == 2;
        setWords(words[1], words[0]);
    }

    @Override
    public Object at0(long index) {
        return super.at0(index);
    }

    @Override
    public void atput0(long index, Object object) {
        super.atput0(index, object);
        Long doubleBits = Double.doubleToLongBits(value);
        if (index == 0) {
            setWords((long) object, doubleBits.intValue());
        } else if (index == 1) {
            setWords(doubleBits >> 32, ((Long) object).intValue());
        } else {
            throw new SqueakException("FloatObject only has two slots");
        }
    }

    private void setWords(final long high, final long low) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        long highMasked = high & LargeIntegerObject.MASK_32BIT;
        long lowMasked = low & LargeIntegerObject.MASK_32BIT;
        super.atput0(0, highMasked);
        super.atput0(1, lowMasked);
        this.value = Double.longBitsToDouble((highMasked << 32) | lowMasked);
    }

    @Override
    public final int size() {
        return 2;
    }

    public double getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "" + value;
    }

    @Override
    public boolean equals(Object b) {
        if (b instanceof FloatObject) {
            return value == value;
        } else {
            return super.equals(b);
        }
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public BaseSqueakObject shallowCopy() {
        return new FloatObject(this);
    }

    public static FloatObject bytesAsFloatObject(SqueakImageContext image, byte[] bytes) {
        ByteBuffer buf = ByteBuffer.allocate(8); // 2 * 32 bit
        buf.order(ByteOrder.nativeOrder());
        buf.put(bytes);
        buf.rewind();
        long low = Integer.toUnsignedLong(buf.asIntBuffer().get(0));
        long high = Integer.toUnsignedLong(buf.asIntBuffer().get(1));
        return new FloatObject(image, high, low);
    }
}
