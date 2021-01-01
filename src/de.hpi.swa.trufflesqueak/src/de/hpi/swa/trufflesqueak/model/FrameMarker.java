/*
 * Copyright (c) 2017-2021 Software Architecture Group, Hasso Plattner Institute
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
        final ContextObject context = FrameAccess.getContext(targetFrame, code);
        if (context != null) {
            assert context.getFrameMarker() == this;
            return context;
        } else {
            assert this == FrameAccess.getMarker(targetFrame, code) : "Frame does not match";
            return ContextObject.create(code.getSqueakClass().getImage(), targetFrame, code);
        }
    }
}
