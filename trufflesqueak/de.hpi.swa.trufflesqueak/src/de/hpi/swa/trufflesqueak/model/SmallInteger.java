package de.hpi.swa.trufflesqueak.model;

public class SmallInteger extends ImmutableObject {
    private final int value;

    public SmallInteger(int i) {
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

    public int getValue() {
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
    public int unwrapInt() {
        return getValue();
    }
}
