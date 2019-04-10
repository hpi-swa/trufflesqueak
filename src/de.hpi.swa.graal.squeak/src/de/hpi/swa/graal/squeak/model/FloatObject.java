package de.hpi.swa.graal.squeak.model;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;

@ExportLibrary(InteropLibrary.class)
public final class FloatObject extends AbstractSqueakObject {
    public static final int PRECISION = 53;
    public static final int EMIN = -1022;
    public static final int EMAX = 1023;
    public static final int WORD_LENGTH = 2;

    private double doubleValue;

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

    private FloatObject(final SqueakImageContext image, final long hash, final int high, final int low) {
        super(image, hash, image.floatClass);
        setWords(high, low);
    }

    public static FloatObject newFromChunkWords(final SqueakImageContext image, final long hash, final int[] ints) {
        return new FloatObject(image, hash, ints[1], ints[0]);
    }

    public static FloatObject valueOf(final SqueakImageContext image, final double value) {
        return new FloatObject(image, value);
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

    public AbstractSqueakObject shallowCopy() {
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

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean fitsInByte() {
        return false; // TODO
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean fitsInShort() {
        return false; // TODO
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean fitsInInt() {
        return false; // TODO
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean fitsInLong() {
        return false; // TODO
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean fitsInFloat() {
        return false; // TODO
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean fitsInDouble() {
        return true;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public byte asByte() throws UnsupportedMessageException { // TODO
        CompilerDirectives.transferToInterpreter();
        throw UnsupportedMessageException.create();
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public short asShort() throws UnsupportedMessageException { // TODO
        CompilerDirectives.transferToInterpreter();
        throw UnsupportedMessageException.create();
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public int asInt() throws UnsupportedMessageException { // TODO
        CompilerDirectives.transferToInterpreter();
        throw UnsupportedMessageException.create();
    }

    @ExportMessage
    public long asLong() {
        return (long) doubleValue;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public float asFloat() throws UnsupportedMessageException {
        CompilerDirectives.transferToInterpreter();
        throw UnsupportedMessageException.create();
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public double asDouble() {
        return doubleValue;
    }

}
