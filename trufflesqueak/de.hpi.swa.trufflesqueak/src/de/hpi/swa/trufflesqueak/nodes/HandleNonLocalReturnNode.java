package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.FrameMarker;

@ImportStatic(FrameAccess.class)
public abstract class HandleNonLocalReturnNode extends Node {
    @CompilationFinal protected final CompiledCodeObject code;
    @Child private TerminateContextNode terminateNode;
    @Child private AboutToReturnNode aboutToReturnNode;

    public static HandleNonLocalReturnNode create(CompiledCodeObject code) {
        return HandleNonLocalReturnNodeGen.create(code);
    }

    public HandleNonLocalReturnNode(CompiledCodeObject code) {
        this.code = code;
        terminateNode = TerminateContextNode.create(code);
        if (code instanceof CompiledMethodObject) {
            aboutToReturnNode = AboutToReturnNode.create((CompiledMethodObject) code);
        }
    }

    public abstract Object executeHandle(VirtualFrame frame, NonLocalReturn nlr);

    @Specialization(guards = "isVirtualized(frame)")
    protected Object handleVirtualized(VirtualFrame frame, NonLocalReturn nlr) {
        if (aboutToReturnNode != null && code.isUnwindMarked()) { // handle ensure: or ifCurtailed:
            aboutToReturnNode.executeAboutToReturn(frame, nlr);
        }
        terminateNode.executeTerminate(frame);
        FrameMarker frameMarker = (FrameMarker) FrameAccess.getContextOrMarker(frame);
        if (nlr.getTargetContext().getFrameMarker() == frameMarker) {
            return nlr.getReturnValue();
        } else {
            throw nlr;
        }
    }

    @Specialization(guards = "!isVirtualized(frame)")
    protected Object handle(VirtualFrame frame, NonLocalReturn nlr) {
        if (aboutToReturnNode != null && code.isUnwindMarked()) { // handle ensure: or ifCurtailed:
            aboutToReturnNode.executeAboutToReturn(frame, nlr);
        }
        ContextObject context = (ContextObject) FrameAccess.getContextOrMarker(frame);
        if (context.isDirty()) {
            ContextObject sender = (ContextObject) context.getSender(); // sender should not be nil
            terminateNode.executeTerminate(frame);
            throw new NonVirtualReturn(nlr.getReturnValue(), nlr.getTargetContext(), sender);
        } else {
            terminateNode.executeTerminate(frame);
            assert context != null; // TODO: currently assuming context is not virtualized
            if (nlr.getTargetContext() == context || nlr.getTargetContext().getFrameMarker() == context.getFrameMarker()) {
                return nlr.getReturnValue();
            } else {
                throw nlr;
            }
        }
    }
}
