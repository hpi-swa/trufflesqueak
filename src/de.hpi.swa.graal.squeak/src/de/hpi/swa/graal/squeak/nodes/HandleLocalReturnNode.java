package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.exceptions.Returns.LocalReturn;
import de.hpi.swa.graal.squeak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.util.FrameAccess;

@ImportStatic(FrameAccess.class)
public abstract class HandleLocalReturnNode extends AbstractNodeWithCode {
    @Child private TerminateContextNode terminateNode;

    public static HandleLocalReturnNode create(final CompiledCodeObject code) {
        return HandleLocalReturnNodeGen.create(code);
    }

    public HandleLocalReturnNode(final CompiledCodeObject code) {
        super(code);
        terminateNode = TerminateContextNode.create(code);
    }

    public abstract Object executeHandle(VirtualFrame frame, LocalReturn lr);

    @Specialization(guards = "isVirtualized(frame)")
    protected Object handleVirtualized(final VirtualFrame frame, final LocalReturn lr) {
        terminateNode.executeTerminate(frame);
        return lr.getReturnValue();
    }

    @Specialization(guards = "!isVirtualized(frame)")
    protected Object handle(final VirtualFrame frame, final LocalReturn lr) {
        final ContextObject context = getContext(frame);
        if (context.hasModifiedSender()) {
            final ContextObject newSender = context.getNotNilSender(); // sender has changed
            terminateNode.executeTerminate(frame);
            throw new NonVirtualReturn(lr.getReturnValue(), newSender, newSender);
        } else {
            terminateNode.executeTerminate(frame);
            return lr.getReturnValue();
        }
    }
}
