/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import org.graalvm.collections.UnmodifiableEconomicMap;

import com.oracle.truffle.api.CompilerAsserts;

import de.hpi.swa.trufflesqueak.image.SqueakImageChunk;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.image.SqueakImageWriter;
import de.hpi.swa.trufflesqueak.util.VarHandleUtils;

public final class FloatObject extends AbstractSqueakObjectWithHash {
    public static final int PRECISION = 53;
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
    public void fillin(final SqueakImageChunk chunk) {
        // Nothing to do.
    }

    @Override
    public ClassObject getSqueakClass() {
        return getSqueakClass(SqueakImageContext.getSlow());
    }

    @Override
    public ClassObject getSqueakClass(final SqueakImageContext image) {
        return image.floatClass;
    }

    @Override
    protected AbstractSqueakObjectWithHash getForwardingPointer() {
        return this; // FloatObject cannot be forwarded
    }

    @Override
    public AbstractSqueakObjectWithHash resolveForwardingPointer() {
        return this; // FloatObject cannot be forwarded
    }

    @Override
    public void pointersBecomeOneWay(final UnmodifiableEconomicMap<Object, Object> fromToMap) {
        // Nothing to do
    }

    public static FloatObject valueOf(final SqueakImageContext image, final double value) {
        return new FloatObject(image, value);
    }

    public static Object newFrom(final SqueakImageChunk chunk) {
        assert chunk.getBytes().length == 2 * Integer.BYTES;
        final long lowValue = Integer.toUnsignedLong(VarHandleUtils.getInt(chunk.getBytes(), 0));
        final long highValue = Integer.toUnsignedLong(VarHandleUtils.getInt(chunk.getBytes(), 1));
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

    private void setWords(final long high, final long low) {
        doubleValue = Double.longBitsToDouble(high << 32 | low);
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

    public boolean isInfinite() {
        return Double.isInfinite(doubleValue);
    }

    public boolean isNaN() {
        return Double.isNaN(doubleValue);
    }

    public boolean isPositive() {
        return doubleValue >= 0;
    }

    public boolean isPositiveInfinity() {
        return doubleValue == Double.POSITIVE_INFINITY;
    }

    public boolean isNegativeInfinity() {
        return doubleValue == Double.NEGATIVE_INFINITY;
    }

    public boolean isZero() {
        return doubleValue == 0;
    }

    @Override
    public int getNumSlots() {
        return 1; /* FIXME: inconsistent with size (not counting in header for some reason) */
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
    public void write(final SqueakImageWriter writer) {
        writer.writeObjectHeader(getNumSlots(), getSqueakHash(), getSqueakClass(), 0);
        writer.writeLong(Double.doubleToRawLongBits(doubleValue));
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return Double.toString(doubleValue);
    }

    public FloatObject shallowCopy() {
        return new FloatObject(this);
    }
}
