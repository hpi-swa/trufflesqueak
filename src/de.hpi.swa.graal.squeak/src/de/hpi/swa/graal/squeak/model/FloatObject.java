package de.hpi.swa.graal.squeak.model;

import com.oracle.truffle.api.CompilerAsserts;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;

public final class FloatObject extends AbstractSqueakObject {
    public static final int PRECISION = 53;
    public static final int EMIN = -1022;
    public static final int EMAX = 1023;
    private static final int WORD_LENGTH = 2;

    private double doubleValue;

    public static FloatObject newFromChunkWords(final SqueakImageContext image, final long hash, final int[] ints) {
        return new FloatObject(image, hash, ints[1], ints[0]);
    }

    public static FloatObject valueOf(final SqueakImageContext image, final double value) {
        return new FloatObject(image, value);
    }

    public FloatObject(final SqueakImageContext image) {
        super(image, image.floatClass);
    }

    public FloatObject(final FloatObject original) {
        super(original.image, original.getSqueakClass());
        doubleValue = original.doubleValue;
    }

    public FloatObject(final SqueakImageContext image, final double doubleValue) {
        this(image);
        this.doubleValue = doubleValue;
    }

    private FloatObject(final SqueakImageContext image, final long hash, final long high, final long low) {
        super(image, hash, image.floatClass);
        setWords(high, low);
    }

    public FloatObject(final SqueakImageContext image, final long high, final long low) {
        this(image);
        setWords(high, low);
    }

    public long getHigh() {
        return Integer.toUnsignedLong((int) (Double.doubleToRawLongBits(doubleValue) >> 32));
    }

    public long getLow() {
        return Integer.toUnsignedLong((int) Double.doubleToRawLongBits(doubleValue));
    }

    public void setHigh(final long value) {
        assert 0 <= value && value <= NativeObject.INTEGER_MAX;
        setWords(value, getLow());
    }

    public void setLow(final long value) {
        assert 0 <= value && value <= NativeObject.INTEGER_MAX;
        setWords(getHigh(), value);
    }

    private void setWords(final long high, final long low) {
        final long highMasked = high & LargeIntegerObject.MASK_32BIT;
        final long lowMasked = low & LargeIntegerObject.MASK_32BIT;
        this.doubleValue = Double.longBitsToDouble(((highMasked) << 32) | lowMasked);
    }

    public void setBytes(final byte[] bytes) {
        assert bytes.length == WORD_LENGTH * 4;
        final int high = ((bytes[3] & 0xff) << 24) | ((bytes[2] & 0xff) << 16) | ((bytes[1] & 0xff) << 8) | bytes[0] & 0xff;
        final int low = ((bytes[7] & 0xff) << 24) | ((bytes[6] & 0xff) << 16) | ((bytes[5] & 0xff) << 8) | bytes[4] & 0xff;
        setWords(high, low);
    }

    public byte[] getBytes() {
        return getBytes(doubleValue);
    }

    public static byte[] getBytes(final double value) {
        final long bits = Double.doubleToRawLongBits(value);
        return new byte[]{(byte) (bits >> 56), (byte) (bits >> 48), (byte) (bits >> 40), (byte) (bits >> 32),
                        (byte) (bits >> 24), (byte) (bits >> 16), (byte) (bits >> 8), (byte) bits};
    }

    public static int size() {
        return WORD_LENGTH;
    }

    public double getValue() {
        return doubleValue;
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "" + doubleValue;
    }

    public AbstractSqueakObject shallowCopy() {
        return new FloatObject(this);
    }
}
