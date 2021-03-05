/*
 * Copyright (c) 2017-2021 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.trufflesqueak.exceptions.RespecializeException;

public final class CharacterObject extends AbstractSqueakObject {
    private final long value;

    private CharacterObject(final long value) {
        assert value > Character.MAX_VALUE : "CharacterObject should only be used for non-primitive chars.";
        this.value = value;
    }

    @Override
    public int getNumSlots() {
        return 0;
    }

    @Override
    public int instsize() {
        return 0;
    }

    @Override
    public int size() {
        return 0;
    }

    public static Object valueOf(final long value) {
        if (value <= Character.MAX_VALUE) {
            return (char) value;
        } else {
            return new CharacterObject(value);
        }
    }

    public static char valueExactOf(final long value) throws RespecializeException {
        if (value <= Character.MAX_VALUE) {
            return (char) value;
        } else {
            throw RespecializeException.transferToInterpreterInvalidateAndThrow();
        }
    }

    public static Object valueOf(final long value, final ConditionProfile isImmediateProfile) {
        if (isImmediateProfile.profile(value <= Character.MAX_VALUE)) {
            return (char) value;
        } else {
            return new CharacterObject(value);
        }
    }

    @Override
    public long getSqueakHash() {
        return getValue();
    }

    public long getValue() {
        return value;
    }
}
