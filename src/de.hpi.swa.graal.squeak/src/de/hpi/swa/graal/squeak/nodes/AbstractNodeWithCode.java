package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.FrameMarker;

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

    protected final boolean isVirtualized(final VirtualFrame frame) {
        final Object contextOrMarker = FrameUtil.getObjectSafe(frame, code.thisContextOrMarkerSlot);
        return !(contextOrMarker instanceof ContextObject) || !((ContextObject) contextOrMarker).isDirty();
    }

    protected final boolean isFullyVirtualized(final VirtualFrame frame) {
        // true if slot holds FrameMarker or null (when entering code object)
        return !(getContextOrMarker(frame) instanceof ContextObject);
    }

    protected final Object getContextOrMarker(final VirtualFrame frame) {
        return FrameUtil.getObjectSafe(frame, code.thisContextOrMarkerSlot);
    }

    protected final ContextObject getContext(final VirtualFrame frame) {
        return (ContextObject) getContextOrMarker(frame);
    }

    protected final FrameMarker getFrameMarker(final VirtualFrame frame) {
        return (FrameMarker) getContextOrMarker(frame);
    }
}
