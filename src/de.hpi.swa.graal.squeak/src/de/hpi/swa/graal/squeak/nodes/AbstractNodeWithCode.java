package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.util.FrameAccess;

@ImportStatic(SqueakGuards.class)
@ReportPolymorphism
@TypeSystemReference(SqueakTypes.class)
public abstract class AbstractNodeWithCode extends Node {
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
        final ContextObject context = getContext(frame);
        return context != null ? context : FrameAccess.getMarker(frame, code);
    }
}
