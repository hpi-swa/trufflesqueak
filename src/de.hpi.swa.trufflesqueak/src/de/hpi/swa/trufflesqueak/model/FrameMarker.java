/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.MaterializedFrame;

import de.hpi.swa.trufflesqueak.util.FrameAccess;

/**
 * TruffleSqueak FrameMarker.
 *
 * <pre>
 *              +---------------------------------+
 * sender    -> | FrameMarker: virtual sender     |
 *              | ContextObject: materialized     |
 *              | nil: terminated / top-level     |
 *              | null: not yet set               |
 *              +---------------------------------+
 * context   -> | ContextObject / null            |
 *              +---------------------------------+
 * </pre>
 */

public final class FrameMarker implements SenderChainLink {
    private Object sender;
    private ContextObject context;

    public FrameMarker() {
    }

    public FrameMarker(final Object sender, final ContextObject context) {
        this.sender = sender;
        this.context = context;
    }

    public Object getSender() {
        return sender;
    }

    public void setSender(final Object markerContextOrNil) {
        sender = markerContextOrNil;
    }

    public ContextObject getContext() {
        return context;
    }

    public void setContext(final ContextObject contextObject) {
        context = contextObject;
    }

    public SenderChainLink getNextLink() {
        return (SenderChainLink) sender;
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "FrameMarker@" + Integer.toHexString(System.identityHashCode(this));
    }

    public ContextObject getMaterializedContext() {
        if (context != null) {
            return context;
        } else {
            final MaterializedFrame targetFrame = FrameAccess.findFrameForMarker(this);
            final ContextObject theContext = FrameAccess.getContext(targetFrame);
            if (theContext != null) {
                assert theContext.getFrameMarker() == this;
                return theContext;
            } else {
                assert this == FrameAccess.getMarker(targetFrame) : "Frame does not match";
                final CompiledCodeObject code = FrameAccess.getCodeObject(targetFrame);
                return ContextObject.create(code.getSqueakClass().getImage(), targetFrame, code);
            }
        }
    }
}
