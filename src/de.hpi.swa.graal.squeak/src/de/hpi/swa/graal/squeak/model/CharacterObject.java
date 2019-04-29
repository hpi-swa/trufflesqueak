package de.hpi.swa.graal.squeak.model;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;

public final class CharacterObject extends AbstractSqueakObjectWithClassAndHash {
    private final int value;

    private CharacterObject(final SqueakImageContext image, final int value) {
        super(image, image.characterClass);
        assert value > Character.MAX_VALUE : "CharacterObject should only be used for non-primitive chars.";
        this.value = value;
    }

    @Override
    public int instsize() {
        return 0;
    }

    @Override
    public int size() {
        return 0;
    }

    public static Object valueOf(final SqueakImageContext image, final int value) {
        if (value <= Character.MAX_VALUE) {
            return (char) value;
        } else {
            return new CharacterObject(image, value);
        }
    }

    public long getValue() {
        return Integer.toUnsignedLong(value);
    }
}
