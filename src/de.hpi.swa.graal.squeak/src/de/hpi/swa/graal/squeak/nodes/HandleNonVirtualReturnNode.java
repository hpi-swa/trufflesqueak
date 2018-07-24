package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.graal.squeak.model.FrameMarker;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameArgumentNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public abstract class HandleNonVirtualReturnNode extends Node {
    @Child private FrameArgumentNode senderNode = FrameArgumentNode.create(FrameAccess.SENDER_OR_SENDER_MARKER);

    public static HandleNonVirtualReturnNode create() {
        return HandleNonVirtualReturnNodeGen.create();
    }

    public abstract Object executeHandle(VirtualFrame frame, NonVirtualReturn nvr);

    @Specialization(guards = "hasVirtualSender(frame)")
    protected final Object handleVirtualized(final VirtualFrame frame, final NonVirtualReturn nvr) {
        if (senderNode.executeRead(frame) == nvr.getTargetContext().getFrameMarker()) {
            return nvr.getReturnValue();
        }
        throw nvr;
    }

    @Fallback
    protected final Object handle(final VirtualFrame frame, final NonVirtualReturn nvr) {
        if (senderNode.executeRead(frame) == nvr.getTargetContext()) {
            return nvr.getReturnValue();
        }
        throw nvr;
    }

    protected final boolean hasVirtualSender(final VirtualFrame frame) {
        return senderNode.executeRead(frame) instanceof FrameMarker;
    }
}
