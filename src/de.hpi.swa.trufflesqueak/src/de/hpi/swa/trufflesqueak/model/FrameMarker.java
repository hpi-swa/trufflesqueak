/*
 * Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2023 Oracle and/or its affiliates
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
        final ContextObject context = FrameAccess.getContext(targetFrame);
        if (context != null) {
            assert context.getFrameMarker() == this;
            return context;
        } else {
            assert this == FrameAccess.getMarker(targetFrame) : "Frame does not match";
            final CompiledCodeObject code = FrameAccess.getCodeObject(targetFrame);
            return ContextObject.create(code.getSqueakClass().getImage(), targetFrame, code);
        }
    }
}
