package de.hpi.swa.graal.squeak.model;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;

public final class CharacterObject extends AbstractSqueakObject {
    private final int value;

    public static Object valueOf(final SqueakImageContext image, final int value) {
        if (value <= Character.MAX_VALUE) {
            return (char) value;
        } else {
            return new CharacterObject(image, value);
        }
    }

    protected CharacterObject(final SqueakImageContext image, final int value) {
        super(image, image.characterClass);
        this.value = value;
    }

    public long getValue() {
        return Integer.toUnsignedLong(value);
    }
}
