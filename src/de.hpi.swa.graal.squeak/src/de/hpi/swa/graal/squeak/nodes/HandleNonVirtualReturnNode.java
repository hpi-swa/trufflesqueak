package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.FrameMarker;
import de.hpi.swa.graal.squeak.util.FrameAccess;

@ImportStatic(FrameAccess.class)
public abstract class HandleNonVirtualReturnNode extends Node {

    public static HandleNonVirtualReturnNode create() {
        return HandleNonVirtualReturnNodeGen.create();
    }

    public abstract Object executeHandle(VirtualFrame frame, NonVirtualReturn nvr);

    @Specialization(guards = {"hasVirtualSender(frame)", "hasMatchingMarker(frame, nvr)"})
    protected static final Object handleVirtualizedReturn(@SuppressWarnings("unused") final VirtualFrame frame, final NonVirtualReturn nvr) {
        return nvr.getReturnValue();
    }

    @Specialization(guards = {"hasVirtualSender(frame)", "!hasMatchingMarker(frame, nvr)"})
    protected static final Object handleVirtualizedThrow(@SuppressWarnings("unused") final VirtualFrame frame, final NonVirtualReturn nvr) {
        throw nvr;
    }

    @Specialization(guards = {"!hasVirtualSender(frame)", "hasMatchingContext(frame, nvr)"})
    protected static final Object handleReturn(@SuppressWarnings("unused") final VirtualFrame frame, final NonVirtualReturn nvr) {
        return nvr.getReturnValue();
    }

    @Specialization(guards = {"!hasVirtualSender(frame)", "!hasMatchingContext(frame, nvr)"})
    protected static final Object handleThrow(@SuppressWarnings("unused") final VirtualFrame frame, final NonVirtualReturn nvr) {
        throw nvr;
    }

    @Fallback
    protected static final Object doFail(@SuppressWarnings("unused") final NonVirtualReturn nvr) {
        throw new SqueakException("Should not happen");
    }

    protected static final boolean hasMatchingMarker(final VirtualFrame frame, final NonVirtualReturn nvr) {
        return FrameAccess.getSender(frame) == nvr.getTargetContext().getFrameMarker();
    }

    protected static final boolean hasMatchingContext(final VirtualFrame frame, final NonVirtualReturn nvr) {
        return FrameAccess.getSender(frame) == nvr.getTargetContext();
    }

    protected static final boolean hasVirtualSender(final VirtualFrame frame) {
        return FrameAccess.getSender(frame) instanceof FrameMarker;
    }
}
