package de.hpi.swa.trufflesqueak.model;

import de.hpi.swa.trufflesqueak.SqueakImage;

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
}
