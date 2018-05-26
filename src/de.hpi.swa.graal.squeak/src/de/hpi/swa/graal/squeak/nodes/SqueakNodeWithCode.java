package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.FrameMarker;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotReadNode;

/**
 * This is the base class for Squeak bytecode evaluation.
 */
@TypeSystemReference(SqueakTypes.class)
public abstract class SqueakNodeWithCode extends SqueakNode {
    @CompilationFinal protected final CompiledCodeObject code;
    @Child private FrameSlotReadNode contextOrMarkerReadNode = FrameSlotReadNode.createForContextOrMarker();

    public SqueakNodeWithCode(final CompiledCodeObject code) {
        this.code = code;
    }

    protected final boolean isVirtualized(final VirtualFrame frame) {
        final Object contextOrMarker = contextOrMarkerReadNode.executeRead(frame);
        return contextOrMarker instanceof FrameMarker || !((ContextObject) contextOrMarker).isDirty();
    }

    protected final Object getContextOrMarker(final VirtualFrame frame) {
        return contextOrMarkerReadNode.executeRead(frame);
    }

    protected final ContextObject getContext(final VirtualFrame frame) {
        return (ContextObject) contextOrMarkerReadNode.executeRead(frame);
    }

    protected final FrameMarker getFrameMarker(final VirtualFrame frame) {
        return (FrameMarker) contextOrMarkerReadNode.executeRead(frame);
    }
}
