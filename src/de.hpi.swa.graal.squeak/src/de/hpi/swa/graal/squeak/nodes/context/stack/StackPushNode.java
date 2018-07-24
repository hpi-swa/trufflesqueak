package de.hpi.swa.graal.squeak.nodes.context.stack;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackWriteNode;

public abstract class StackPushNode extends AbstractNode {
    public static StackPushNode create() {
        return StackPushNodeGen.create();
    }

    public abstract void executeWrite(VirtualFrame frame, Object value);

    @Specialization(guards = {"isVirtualized(frame)"})
    protected static final void doWriteVirtualized(final VirtualFrame frame, final Object value,
                    @Cached("create()") final FrameStackWriteNode writeNode) {
        assert value != null;
        final int newSP = FrameUtil.getIntSafe(frame, CompiledCodeObject.stackPointerSlot) + 1;
        writeNode.execute(frame, newSP, value);
        frame.setInt(CompiledCodeObject.stackPointerSlot, newSP);
    }

    @Fallback
    protected static final void doWrite(final VirtualFrame frame, final Object value) {
        assert value != null;
        getContext(frame).push(value);
    }
}
