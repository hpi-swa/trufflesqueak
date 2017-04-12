package de.hpi.swa.trufflesqueak.model;

import de.hpi.swa.trufflesqueak.SqueakImage;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;

public class ImmediateCharacter extends BaseSqueakObject {
    private final int value;

    public ImmediateCharacter(int i) {
        value = i;
    }

    @Override
    public String toString() {
        return new String(new byte[]{(byte) value});
    }

    @Override
    public BaseSqueakObject getSqClass() {
        return SqueakImage.characterClass;
    }

    @Override
    public void become(BaseSqueakObject other) throws PrimitiveFailed {
        throw new PrimitiveFailed();
    }
}
