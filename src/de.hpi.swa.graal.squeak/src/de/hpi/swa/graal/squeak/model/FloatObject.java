package de.hpi.swa.graal.squeak.model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

import de.hpi.swa.graal.squeak.exceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.AbstractImageChunk;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.storages.AbstractNativeObjectStorage;
import de.hpi.swa.graal.squeak.model.storages.NativeWordsStorage;

public final class FloatObject extends AbstractSqueakObject {
    @CompilationFinal private AbstractNativeObjectStorage storage;

    @CompilationFinal public static final int PRECISION = 53;
    @CompilationFinal public static final int EMIN = -1022;
    @CompilationFinal public static final int EMAX = 1023;

    @CompilationFinal private double value;

    public static FloatObject valueOf(final SqueakImageContext image, final double value) {
        return new FloatObject(image, value);
    }

    public FloatObject(final SqueakImageContext image) {
        super(image, image.floatClass);
        storage = new NativeWordsStorage(2);
    }

    public FloatObject(final FloatObject original) {
        super(original.image, original.getSqClass());
        storage = original.storage.shallowCopy();
        value = original.value;
    }

    public FloatObject(final SqueakImageContext image, final double value) {
        this(image);
        final long doubleBits = Double.doubleToLongBits(value);
        final long high = doubleBits >> 32;
        final long low = doubleBits & LargeIntegerObject.MASK_32BIT;
        setWords(high, low);
        assert this.value == value || Double.isNaN(value);
    }

    public FloatObject(final SqueakImageContext image, final long high, final long low) {
        this(image);
        setWords(high, low);
    }

    @Override
    public void fillin(final AbstractImageChunk chunk) {
        super.fillin(chunk);
        final int[] words = chunk.getWords();
        assert words.length == 2;
        setWords(words[1], words[0]);
    }

    public Object at0(final long index) {
        return storage.getNativeAt0(index);
    }

    private void basicAtPut0(final long index, final Object object) {
        if (object instanceof LargeIntegerObject) {
            final long longValue;
            try {
                longValue = ((LargeIntegerObject) object).reduceToLong();
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException(e.toString());
            }
            storage.setNativeAt0(index, longValue);
        } else {
            storage.setNativeAt0(index, (long) object);
        }
    }

    public void atput0(final long index, final Object object) {
        basicAtPut0(index, object);
        final Long doubleBits = Double.doubleToLongBits(value);
        if (index == 0) {
            setWords((long) object, doubleBits.intValue());
        } else if (index == 1) {
            setWords(doubleBits >> 32, ((Long) object).intValue());
        } else {
            throw new SqueakException("FloatObject only has two slots");
        }
    }

    public long getNativeAt0(final long index) {
        return storage.getNativeAt0(index);
    }

    private void setWords(final long high, final long low) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        final long highMasked = high & LargeIntegerObject.MASK_32BIT;
        final long lowMasked = low & LargeIntegerObject.MASK_32BIT;
        basicAtPut0(0, highMasked);
        basicAtPut0(1, lowMasked);
        this.value = Double.longBitsToDouble((highMasked << 32) | lowMasked);
    }

    public byte[] getBytes() {
        return storage.getBytes();
    }

    public static int size() {
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
    public boolean equals(final Object b) {
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

    public AbstractSqueakObject shallowCopy() {
        return new FloatObject(this);
    }

    public static FloatObject bytesAsFloatObject(final SqueakImageContext image, final byte[] bytes) {
        final ByteBuffer buf = ByteBuffer.allocate(8); // 2 * 32 bit
        buf.order(ByteOrder.nativeOrder());
        buf.put(bytes);
        buf.rewind();
        final long low = Integer.toUnsignedLong(buf.asIntBuffer().get(0));
        final long high = Integer.toUnsignedLong(buf.asIntBuffer().get(1));
        return new FloatObject(image, high, low);
    }
}
