/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.MaterializedFrame;

import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class FrameMarker {
    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "FrameMarker@" + Integer.toHexString(System.identityHashCode(this));
    }

    public ContextObject getMaterializedContext() {
        final MaterializedFrame targetFrame = FrameAccess.findFrameForMarker(this);
        final CompiledCodeObject code = FrameAccess.getMethodOrBlock(targetFrame);
        final Object contextOrNil = FrameAccess.getContextOrNil(targetFrame, code);
        if (contextOrNil != NilObject.SINGLETON) {
            assert ((ContextObject) contextOrNil).getFrameMarkerOrNil() == this;
            return (ContextObject) contextOrNil;
        } else {
            assert this == FrameAccess.getMarker(targetFrame, code) : "Frame does not match";
            return ContextObject.create(code.getSqueakClass().getImage(), targetFrame, code);
        }
    }
}
