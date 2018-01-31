package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.Returns.LocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

@ImportStatic(FrameAccess.class)
public abstract class HandleLocalReturnNode extends AbstractNodeWithCode {
    @Child private TerminateContextNode terminateNode;

    public static HandleLocalReturnNode create(CompiledCodeObject code) {
        return HandleLocalReturnNodeGen.create(code);
    }

    public HandleLocalReturnNode(CompiledCodeObject code) {
        super(code);
        terminateNode = TerminateContextNode.create(code);
    }

    public abstract Object executeHandle(VirtualFrame frame, LocalReturn lr);

    @Specialization(guards = "isVirtualized(frame, code)")
    protected Object handleVirtualized(VirtualFrame frame, LocalReturn lr) {
        terminateNode.executeTerminate(frame);
        return lr.getReturnValue();
    }

    @Specialization(guards = "!isVirtualized(frame, code)")
    protected Object handle(VirtualFrame frame, LocalReturn lr) {
        ContextObject context = (ContextObject) FrameAccess.getContextOrMarker(frame);
        if (context.isDirty()) {
            ContextObject sender = (ContextObject) context.getSender(); // sender should not be nil
            terminateNode.executeTerminate(frame);
            throw new NonVirtualReturn(lr.getReturnValue(), sender, sender);
        } else {
            terminateNode.executeTerminate(frame);
            return lr.getReturnValue();
        }
    }
}
