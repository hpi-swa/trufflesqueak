package de.hpi.swa.graal.squeak.nodes.context;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.FrameMarker;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public abstract class ReceiverAndArgumentsNode extends Node {
    public static ReceiverAndArgumentsNode create() {
        return ReceiverAndArgumentsNodeGen.create();
    }

    public abstract Object executeGet(VirtualFrame frame);

    protected static final boolean isVirtualized(final VirtualFrame frame) {
        final Object contextOrMarker = frame.getValue(CompiledCodeObject.thisContextOrMarkerSlot);
        return contextOrMarker instanceof FrameMarker || !((ContextObject) contextOrMarker).isDirty();
    }

    @Specialization(guards = {"isVirtualized(frame)"})
    @ExplodeLoop
    protected static final Object[] doRcvrAndArgsVirtualized(final VirtualFrame frame) {
        final Object[] frameArguments = frame.getArguments();
        final Object[] rcvrAndArgs = new Object[frameArguments.length - FrameAccess.RECEIVER];
        for (int i = 0; i < rcvrAndArgs.length; i++) {
            rcvrAndArgs[i] = frameArguments[FrameAccess.RECEIVER + i];
        }
        return rcvrAndArgs;
    }

    @Fallback
    @ExplodeLoop
    protected static final Object[] doRcvrAndArgs(final VirtualFrame frame) {
        final ContextObject context = (ContextObject) frame.getValue(CompiledCodeObject.thisContextOrMarkerSlot);
        final int numArgsAndCopied = context.getClosureOrMethod().getNumArgsAndCopied();
        return context.getReceiverAndNArguments(numArgsAndCopied);
    }
}
