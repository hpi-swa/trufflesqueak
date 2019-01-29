package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.graal.squeak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public abstract class HandleNonLocalReturnNode extends AbstractNodeWithCode {
    @Child private TerminateContextNode terminateNode;
    @Child private AboutToReturnNode aboutToReturnNode;

    protected HandleNonLocalReturnNode(final CompiledCodeObject code) {
        super(code);
        terminateNode = TerminateContextNode.create(code);
        aboutToReturnNode = AboutToReturnNode.create(code);
    }

    public static HandleNonLocalReturnNode create(final CompiledCodeObject code) {
        return HandleNonLocalReturnNodeGen.create(code);
    }

    public abstract Object executeHandle(VirtualFrame frame, NonLocalReturn nlr);

    @Specialization(guards = {"hasModifiedSender(frame)"})
    protected final Object handleModifiedSender(final VirtualFrame frame, final NonLocalReturn nlr) {
        aboutToReturnNode.executeAboutToReturn(frame, nlr); // handle ensure: or ifCurtailed:
        final ContextObject newSender = FrameAccess.getSenderContext(frame); // sender has changed
        final ContextObject target = (ContextObject) nlr.getTargetContextOrMarker();
        terminateNode.executeTerminate(frame);
        throw new NonVirtualReturn(nlr.getReturnValue(), target, newSender);
    }

    @Fallback
    protected final Object handleVirtualized(final VirtualFrame frame, final NonLocalReturn nlr) {
        aboutToReturnNode.executeAboutToReturn(frame, nlr); // handle ensure: or ifCurtailed:
        terminateNode.executeTerminate(frame);
        throw nlr;
    }
}
