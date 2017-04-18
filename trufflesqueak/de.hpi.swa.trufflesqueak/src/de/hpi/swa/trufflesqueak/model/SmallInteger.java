package de.hpi.swa.trufflesqueak.model;

public class SmallInteger extends BaseSqueakObject {
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
}
