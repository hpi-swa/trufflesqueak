package de.hpi.swa.trufflesqueak.model;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;

public class SmallInteger extends BaseSqueakObject {
    private final int value;

    public SmallInteger(int i) {
        value = i;
    }

    @Override
    public String toString() {
        return "" + value;
    }

    @Override
    public BaseSqueakObject getSqClass() {
        return image.smallIntegerClass;
    }

    @Override
    public void become(BaseSqueakObject other) throws PrimitiveFailed {
        throw new PrimitiveFailed();
    }
}
