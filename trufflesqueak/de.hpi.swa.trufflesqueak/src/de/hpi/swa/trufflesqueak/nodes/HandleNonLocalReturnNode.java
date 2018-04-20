package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.util.FrameMarker;

public abstract class HandleNonLocalReturnNode extends AbstractNodeWithCode {
    @Child private TerminateContextNode terminateNode;
    @Child private AboutToReturnNode aboutToReturnNode;

    public static HandleNonLocalReturnNode create(final CompiledCodeObject code) {
        return HandleNonLocalReturnNodeGen.create(code);
    }

    public HandleNonLocalReturnNode(final CompiledCodeObject code) {
        super(code);
        terminateNode = TerminateContextNode.create(code);
        if (code instanceof CompiledMethodObject) {
            aboutToReturnNode = AboutToReturnNode.create((CompiledMethodObject) code);
        }
    }

    public abstract Object executeHandle(VirtualFrame frame, NonLocalReturn nlr);

    @Specialization(guards = "isVirtualized(frame)")
    protected Object handleVirtualized(final VirtualFrame frame, final NonLocalReturn nlr) {
        CompilerDirectives.ensureVirtualizedHere(frame);
        if (aboutToReturnNode != null && code.isUnwindMarked()) { // handle ensure: or ifCurtailed:
            aboutToReturnNode.executeAboutToReturn(frame, nlr);
        }
        terminateNode.executeTerminate(frame);
        final FrameMarker frameMarker = getFrameMarker(frame);
        if (nlr.getTargetContext().getFrameMarker() == frameMarker) {
            return nlr.getReturnValue();
        } else {
            throw nlr;
        }
    }

    @Specialization(guards = "!isVirtualized(frame)")
    protected Object handle(final VirtualFrame frame, final NonLocalReturn nlr) {
        if (aboutToReturnNode != null && code.isUnwindMarked()) { // handle ensure: or ifCurtailed:
            aboutToReturnNode.executeAboutToReturn(frame, nlr);
        }
        final ContextObject context = getContext(frame);
        if (context.isDirty()) {
            final ContextObject sender = context.getNotNilSender(); // sender has changed
            final ContextObject target = nlr.getTargetContext().getNotNilSender();
            terminateNode.executeTerminate(frame);
            throw new NonVirtualReturn(nlr.getReturnValue(), target, sender);
        } else {
            terminateNode.executeTerminate(frame);
            if (nlr.getTargetContext().getFrameMarker() == context.getFrameMarker()) {
                nlr.setArrivedAtTargetContext();
            }
            throw nlr;
        }
    }
}
