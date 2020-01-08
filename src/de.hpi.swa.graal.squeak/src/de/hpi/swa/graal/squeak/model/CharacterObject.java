/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.model;

import com.oracle.truffle.api.profiles.ConditionProfile;

public final class CharacterObject extends AbstractSqueakObject {
    private final int value;

    private CharacterObject(final int value) {
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

    public static Object valueOf(final int value) {
        if (value <= Character.MAX_VALUE) {
            return (char) value;
        } else {
            return new CharacterObject(value);
        }
    }

    public static Object valueOf(final int value, final ConditionProfile isFiniteProfile) {
        if (isFiniteProfile.profile(value <= Character.MAX_VALUE)) {
            return (char) value;
        } else {
            return new CharacterObject(value);
        }
    }

    public long getValue() {
        return Integer.toUnsignedLong(value);
    }

    public long getValueRaw() {
        return value;
    }
}
