/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public abstract class AbstractNodeWithCode extends AbstractNode {
    protected final CompiledCodeObject code;

    protected AbstractNodeWithCode(final CompiledCodeObject code) {
        this.code = code;
    }

    protected AbstractNodeWithCode(final AbstractNodeWithCode original) {
        this(original.code);
    }

    protected final boolean hasModifiedSender(final VirtualFrame frame) {
        final ContextObject context = getContext(frame);
        return context != null && context.hasModifiedSender();
    }

    protected final boolean isVirtualized(final VirtualFrame frame) {
        return getContext(frame) == null;
    }

    protected final ContextObject getContext(final VirtualFrame frame) {
        return FrameAccess.getContext(frame, code);
    }

    protected final Object getContextOrMarker(final VirtualFrame frame) {
        return FrameAccess.getContextOrMarker(frame, code);
    }
}
