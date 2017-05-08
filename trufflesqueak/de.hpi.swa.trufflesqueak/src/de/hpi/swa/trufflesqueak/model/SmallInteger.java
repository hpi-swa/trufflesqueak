package de.hpi.swa.trufflesqueak.model;

import de.hpi.swa.trufflesqueak.SqueakImageContext;

public class SmallInteger extends ImmutableObject {
    private final long value;

    public SmallInteger(SqueakImageContext image, long i) {
        super(image);
        value = i;
    }

    @Override
    public String toString() {
        return "" + getValue();
    }

    @Override
    public BaseSqueakObject getSqClass() {
        return image.smallIntegerClass;
    }

    public long getValue() {
        return value;
    }

    @Override
    public BaseSqueakObject at0(int idx) {
        throw new ArrayIndexOutOfBoundsException();
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public long unwrapInt() {
        return getValue();
    }
}
