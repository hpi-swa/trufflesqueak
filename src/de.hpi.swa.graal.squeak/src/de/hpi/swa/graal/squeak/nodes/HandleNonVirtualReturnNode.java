package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.graal.squeak.model.FrameMarker;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public abstract class HandleNonVirtualReturnNode extends Node {

    public static HandleNonVirtualReturnNode create() {
        return HandleNonVirtualReturnNodeGen.create();
    }

    public abstract Object executeHandle(VirtualFrame frame, NonVirtualReturn nvr);

    // TODO: split specializations

    @Specialization(guards = "hasVirtualSender(frame)")
    protected static final Object handleVirtualized(final VirtualFrame frame, final NonVirtualReturn nvr) {
        if (FrameAccess.getSender(frame) == nvr.getTargetContext().getFrameMarker()) {
            return nvr.getReturnValue();
        }
        throw nvr;
    }

    @Fallback
    protected static final Object handle(final VirtualFrame frame, final NonVirtualReturn nvr) {
        if (FrameAccess.getSender(frame) == nvr.getTargetContext()) {
            return nvr.getReturnValue();
        }
        throw nvr;
    }

    protected static final boolean hasVirtualSender(final VirtualFrame frame) {
        return FrameAccess.getSender(frame) instanceof FrameMarker;
    }
}
