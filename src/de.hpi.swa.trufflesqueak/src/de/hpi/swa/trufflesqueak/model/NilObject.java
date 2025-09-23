/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;

import de.hpi.swa.trufflesqueak.image.SqueakImageConstants;
import de.hpi.swa.trufflesqueak.image.SqueakImageWriter;
import de.hpi.swa.trufflesqueak.util.SenderChainLink;

public final class NilObject extends AbstractSqueakObject implements SenderChainLink {
    public static final NilObject SINGLETON = new NilObject();
    public static final long SQUEAK_HASH = 1L;

    private NilObject() {
    }

    public static AbstractSqueakObject nullToNil(final AbstractSqueakObject object) {
        return object == null ? SINGLETON : object;
    }

    public static AbstractSqueakObject nullToNil(final AbstractSqueakObject object, final InlinedConditionProfile profile, final Node node) {
        return profile.profile(node, object == null) ? SINGLETON : object;
    }

    public static Object nullToNil(final Object object) {
        return object == null ? SINGLETON : object;
    }

    public static Object nilToNull(final Object object) {
        return object == SINGLETON ? null : object;
    }

    @Override
    public long getOrCreateSqueakHash() {
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

    /** SenderChainLink methods. */
    public ContextObject getContext() {
        return null;
    }

    public SenderChainLink getNextLink() {
        return null;
    }

    @Override
    public String toString() {
        return "nil";
    }

    public void write(final SqueakImageWriter writer) {
        writer.writeObjectHeader(instsize() + size(), getOrCreateSqueakHash(), writer.getImage().nilClass, 0);
        writer.writePadding(SqueakImageConstants.WORD_SIZE); /* Write alignment word. */
    }
}
