/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.utilities.TriState;

@ExportLibrary(InteropLibrary.class)
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

    /*
     * INTEROPERABILITY
     */

    @ExportMessage
    protected static final class IsIdenticalOrUndefined {
        @Specialization
        protected static TriState doCharacterObject(final CharacterObject receiver, final CharacterObject other) {
            return TriState.valueOf(receiver.getValue() == other.getValue());
        }

        @Fallback
        @SuppressWarnings("unused")
        protected static TriState doOther(final CharacterObject receiver, final Object other) {
            return TriState.UNDEFINED;
        }
    }

    @ExportMessage
    protected static int identityHashCode(@SuppressWarnings("unused") final CharacterObject receiver) {
        return (int) receiver.getValue();
    }
}
