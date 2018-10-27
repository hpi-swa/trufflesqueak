package de.hpi.swa.graal.squeak.nodes.context;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithCode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public abstract class ReceiverAndArgumentsNode extends AbstractNodeWithCode {
    public static ReceiverAndArgumentsNode create(final CompiledCodeObject code) {
        return ReceiverAndArgumentsNodeGen.create(code);
    }

    protected ReceiverAndArgumentsNode(final CompiledCodeObject code) {
        super(code);
    }

    public abstract Object executeGet(VirtualFrame frame);

    @Specialization(guards = {"isVirtualized(frame)"})
    @ExplodeLoop
    protected static final Object[] doRcvrAndArgsVirtualized(final VirtualFrame frame) {
        final Object[] frameArguments = frame.getArguments();
        final int rcvrAndArgsLength = frameArguments.length - FrameAccess.RECEIVER;
        final Object[] rcvrAndArgs = new Object[rcvrAndArgsLength];
        System.arraycopy(frameArguments, FrameAccess.RECEIVER, rcvrAndArgs, 0, rcvrAndArgsLength);
        return rcvrAndArgs;
    }

    @Fallback
    @ExplodeLoop
    protected final Object[] doRcvrAndArgs(final VirtualFrame frame) {
        final ContextObject context = (ContextObject) frame.getValue(code.thisContextOrMarkerSlot);
        final int numArgsAndCopied = context.getClosureOrMethod().getNumArgsAndCopied();
        return context.getReceiverAndNArguments(numArgsAndCopied);
    }
}
