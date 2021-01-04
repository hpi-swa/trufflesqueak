/*
 * Copyright (c) 2017-2021 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.context.frame;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

/* Gets context or marker, lazily initializes the latter if necessary. */
public final class GetContextNode extends AbstractNode {
    @CompilationFinal private FrameSlot contextSlot;

    public static GetContextNode create() {
        return new GetContextNode();
    }

    public boolean hasModifiedSender(final VirtualFrame frame) {
        final ContextObject context = execute(frame);
        return context != null && context.hasModifiedSender();
    }

    public boolean hasContext(final VirtualFrame frame) {
        return execute(frame) == null;
    }

    public ContextObject execute(final VirtualFrame frame) {
        if (contextSlot == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            contextSlot = FrameAccess.findContextSlot(frame);
        }
        return FrameAccess.getContext(frame, contextSlot);
    }
}
