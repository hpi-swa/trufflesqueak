package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.graal.squeak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;

public abstract class HandleNonLocalReturnNode extends AbstractNodeWithCode {
    @Child private TerminateContextNode terminateNode = TerminateContextNode.create();
    @Child private AboutToReturnNode aboutToReturnNode;

    public static HandleNonLocalReturnNode create(final CompiledCodeObject code) {
        return HandleNonLocalReturnNodeGen.create(code);
    }

    public HandleNonLocalReturnNode(final CompiledCodeObject code) {
        super(code);
        if (code instanceof CompiledMethodObject) {
            aboutToReturnNode = AboutToReturnNode.create((CompiledMethodObject) code);
        }
    }

    public abstract Object executeHandle(VirtualFrame frame, NonLocalReturn nlr);

    @Specialization(guards = "isVirtualized(frame)")
    protected Object handleVirtualized(final VirtualFrame frame, final NonLocalReturn nlr) {
        if (aboutToReturnNode != null && code.isUnwindMarked()) { // handle ensure: or ifCurtailed:
            aboutToReturnNode.executeAboutToReturn(frame, nlr);
        }
        terminateNode.executeTerminate(frame);
        if (nlr.getTargetContext().equals(getContextOrMarker(frame))) {
            nlr.setArrivedAtTargetContext();
        }
        throw nlr;
    }

    @Specialization(guards = "!isVirtualized(frame)")
    protected Object handle(final VirtualFrame frame, final NonLocalReturn nlr) {
        if (aboutToReturnNode != null && code.isUnwindMarked()) { // handle ensure: or ifCurtailed:
            aboutToReturnNode.executeAboutToReturn(frame, nlr);
        }
        final ContextObject context = getContext(frame);
        if (context.hasModifiedSender()) {
            final ContextObject newSender = context.getNotNilSender(); // sender has changed
            final ContextObject target = nlr.getTargetContext().getNotNilSender();
            terminateNode.executeTerminate(frame);
            throw new NonVirtualReturn(nlr.getReturnValue(), target, newSender);
        } else {
            terminateNode.executeTerminate(frame);
            if (context.equals(nlr.getTargetContext())) {
                nlr.setArrivedAtTargetContext();
            }
            throw nlr;
        }
    }
}
