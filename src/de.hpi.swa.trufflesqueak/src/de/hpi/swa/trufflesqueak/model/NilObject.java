/*
 * Copyright (c) 2017-2021 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.trufflesqueak.image.SqueakImageConstants;
import de.hpi.swa.trufflesqueak.image.SqueakImageWriter;

public final class NilObject extends AbstractSqueakObject {
    public static final NilObject SINGLETON = new NilObject();
    public static final long SQUEAK_HASH = 1L;
    private static final int IDENTITY_HASH = System.identityHashCode(SINGLETON);

    private NilObject() {
    }

    public static AbstractSqueakObject nullToNil(final AbstractSqueakObject object) {
        return object == null ? SINGLETON : object;
    }

    public static AbstractSqueakObject nullToNil(final AbstractSqueakObject object, final ConditionProfile profile) {
        return profile.profile(object == null) ? SINGLETON : object;
    }

    public static Object nullToNil(final Object object) {
        return object == null ? SINGLETON : object;
    }

    public static Object nullToNil(final Object object, final ConditionProfile profile) {
        return profile.profile(object == null) ? SINGLETON : object;
    }

    @Override
    public long getSqueakHash() {
        return SQUEAK_HASH;
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

    @Override
    public String toString() {
        return "nil";
    }

    public void write(final SqueakImageWriter writer) {
        writer.writeObjectHeader(instsize() + size(), getSqueakHash(), writer.getImage().nilClass, 0);
        writer.writePadding(SqueakImageConstants.WORD_SIZE); /* Write alignment word. */
    }
}
