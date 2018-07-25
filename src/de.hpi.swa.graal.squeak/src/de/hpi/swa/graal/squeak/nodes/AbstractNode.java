package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.FrameMarker;

@ReportPolymorphism
@TypeSystemReference(SqueakTypes.class)
public abstract class AbstractNode extends Node {
    protected static final boolean isVirtualized(final VirtualFrame frame) {
        final Object contextOrMarker = frame.getValue(CompiledCodeObject.thisContextOrMarkerSlot);
        return !(contextOrMarker instanceof ContextObject) || !((ContextObject) contextOrMarker).isDirty();
    }

    protected static final boolean isFullyVirtualized(final VirtualFrame frame) {
        // true if slot holds FrameMarker or null (when entering code object)
        return !(getContextOrMarker(frame) instanceof ContextObject);
    }

    protected static final Object getContextOrMarker(final VirtualFrame frame) {
        return frame.getValue(CompiledCodeObject.thisContextOrMarkerSlot);
    }

    protected static final ContextObject getContext(final VirtualFrame frame) {
        return (ContextObject) getContextOrMarker(frame);
    }

    protected static final FrameMarker getFrameMarker(final VirtualFrame frame) {
        return (FrameMarker) getContextOrMarker(frame);
    }
}
