package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.FrameMarker;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotReadNode;

@ReportPolymorphism
@TypeSystemReference(SqueakTypes.class)
public abstract class AbstractNode extends Node {
    @Child protected FrameSlotReadNode contextOrMarkerReadNode = FrameSlotReadNode.createForContextOrMarker();

    protected final boolean isVirtualized(final VirtualFrame frame) {
        final Object contextOrMarker = contextOrMarkerReadNode.executeRead(frame);
        return contextOrMarker instanceof FrameMarker || !((ContextObject) contextOrMarker).isDirty();
    }

    protected final boolean isFullyVirtualized(final VirtualFrame frame) {
        return contextOrMarkerReadNode.executeRead(frame) instanceof FrameMarker;
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
