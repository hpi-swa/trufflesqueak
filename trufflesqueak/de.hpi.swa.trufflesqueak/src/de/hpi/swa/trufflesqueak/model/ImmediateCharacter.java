package de.hpi.swa.trufflesqueak.model;

public class ImmediateCharacter extends ImmutableObject {
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
        return image.characterClass;
    }

    @Override
    public BaseSqueakObject at0(int idx) {
        if (idx == 0) {
            return new SmallInteger(value);
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public int unwrapInt() {
        return value;
    }
}
