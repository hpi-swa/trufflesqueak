package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.FrameMarker;

/**
 * This is the base class for Squeak bytecode evaluation.
 */
@TypeSystemReference(SqueakTypes.class)
public abstract class SqueakNodeWithCode extends SqueakNode {
    protected final CompiledCodeObject code;

    public SqueakNodeWithCode(final CompiledCodeObject code) {
        this.code = code;
    }

    protected final boolean isVirtualized(final VirtualFrame frame) {
        final Object contextOrMarker = frame.getValue(code.thisContextOrMarkerSlot);
        return contextOrMarker instanceof FrameMarker || !((ContextObject) contextOrMarker).isDirty();
    }

    protected final Object getContextOrMarker(final VirtualFrame frame) {
        return frame.getValue(code.thisContextOrMarkerSlot);
    }

    protected final ContextObject getContext(final VirtualFrame frame) {
        return (ContextObject) getContextOrMarker(frame);
    }

    protected final FrameMarker getFrameMarker(final VirtualFrame frame) {
        return (FrameMarker) getContextOrMarker(frame);
    }
}
