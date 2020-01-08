/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.model;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.image.reading.SqueakImageChunk;
import de.hpi.swa.graal.squeak.util.ArrayConversionUtils;
import de.hpi.swa.graal.squeak.util.UnsafeUtils;

@ExportLibrary(InteropLibrary.class)
public final class FloatObject extends AbstractSqueakObjectWithHash {
    public static final int PRECISION = 53;
    public static final int EMIN = Double.MIN_EXPONENT;
    public static final int EMAX = Double.MAX_EXPONENT;
    public static final int WORD_LENGTH = 2;

    private double doubleValue;

    public FloatObject(final SqueakImageContext image) {
        super(image);
    }

    private FloatObject(final FloatObject original) {
        super(original);
        doubleValue = original.doubleValue;
    }

    public FloatObject(final SqueakImageContext image, final double doubleValue) {
        super(image);
        this.doubleValue = doubleValue;
    }

    @Override
    public ClassObject getSqueakClass() {
        return image.floatClass;
    }

    @Override
    public void fillin(final SqueakImageChunk chunk) {
        // Nothing to do.
    }

    public static FloatObject valueOf(final SqueakImageContext image, final double value) {
        return new FloatObject(image, value);
    }

    public static Object newFrom(final SqueakImageChunk chunk) {
        assert chunk.getBytes().length == 2 * ArrayConversionUtils.INTEGER_BYTE_SIZE;
        final long lowValue = Integer.toUnsignedLong(UnsafeUtils.getInt(chunk.getBytes(), 0));
        final long highValue = Integer.toUnsignedLong(UnsafeUtils.getInt(chunk.getBytes(), 1));
        final double value = Double.longBitsToDouble(highValue << 32 | lowValue);
        return Double.isFinite(value) ? value : new FloatObject(chunk.getImage(), value);
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

    private void setWords(final int high, final int low) {
        setWords(Integer.toUnsignedLong(high), Integer.toUnsignedLong(low));
    }

    private void setWords(final long high, final long low) {
        doubleValue = Double.longBitsToDouble(high << 32 | low);
    }

    public void setBytes(final byte[] bytes) {
        assert bytes.length == WORD_LENGTH * 4;
        final int high = (bytes[3] & 0xff) << 24 | (bytes[2] & 0xff) << 16 | (bytes[1] & 0xff) << 8 | bytes[0] & 0xff;
        final int low = (bytes[7] & 0xff) << 24 | (bytes[6] & 0xff) << 16 | (bytes[5] & 0xff) << 8 | bytes[4] & 0xff;
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

    public boolean isFinite() {
        return Double.isFinite(doubleValue);
    }

    public boolean isNaN() {
        return Double.isNaN(doubleValue);
    }

    public boolean isPositive() {
        return doubleValue >= 0;
    }

    @Override
    public int instsize() {
        return 0;
    }

    @Override
    public int size() {
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

    public FloatObject shallowCopy() {
        return new FloatObject(this);
    }

    /*
     * INTEROPERABILITY
     */

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean isNumber() {
        return true;
    }

    @ExportMessage
    public boolean fitsInByte() {
        return (byte) doubleValue == doubleValue;
    }

    @ExportMessage
    public boolean fitsInShort() {
        return (short) doubleValue == doubleValue;
    }

    @ExportMessage
    public boolean fitsInInt() {
        return (int) doubleValue == doubleValue;
    }

    @ExportMessage
    public boolean fitsInLong() {
        return (long) doubleValue == doubleValue;
    }

    @ExportMessage
    public boolean fitsInFloat() {
        return (float) doubleValue == doubleValue;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean fitsInDouble() {
        return true;
    }

    @ExportMessage
    public byte asByte() {
        return (byte) doubleValue;
    }

    @ExportMessage
    public short asShort() {
        return (short) doubleValue;
    }

    @ExportMessage
    public int asInt() {
        return (int) doubleValue;
    }

    @ExportMessage
    public long asLong() {
        return (long) doubleValue;
    }

    @ExportMessage
    public float asFloat() {
        return (float) doubleValue;
    }

    @ExportMessage
    public double asDouble() {
        return doubleValue;
    }
}
