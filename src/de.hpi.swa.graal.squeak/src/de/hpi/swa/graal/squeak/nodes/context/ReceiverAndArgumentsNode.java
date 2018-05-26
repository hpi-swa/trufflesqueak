package de.hpi.swa.graal.squeak.nodes.context;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.FrameMarker;
import de.hpi.swa.graal.squeak.nodes.accessing.CompiledCodeNodes.GetNumAllArgumentsNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotReadNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public abstract class ReceiverAndArgumentsNode extends Node {
    @Child private GetNumAllArgumentsNode numAllArgumentsNode = GetNumAllArgumentsNode.create();
    @Child private FrameSlotReadNode contextOrMarkerReadNode = FrameSlotReadNode.createForContextOrMarker();

    public static ReceiverAndArgumentsNode create() {
        return ReceiverAndArgumentsNodeGen.create();
    }

    public abstract Object executeGet(VirtualFrame frame);

    protected final boolean isVirtualized(final VirtualFrame frame) {
        final Object contextOrMarker = contextOrMarkerReadNode.executeRead(frame);
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

    @Specialization(guards = {"!isVirtualized(frame)"})
    @ExplodeLoop
    protected final Object[] doRcvrAndArgs(final VirtualFrame frame) {
        final ContextObject context = (ContextObject) contextOrMarkerReadNode.executeRead(frame);
        final int numArgs = numAllArgumentsNode.execute(context.getClosureOrMethod());
        return context.getReceiverAndNArguments(numArgs);
    }
}
