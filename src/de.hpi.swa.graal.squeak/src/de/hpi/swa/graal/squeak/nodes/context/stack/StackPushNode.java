package de.hpi.swa.graal.squeak.nodes.context.stack;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithCode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackWriteNode;

public abstract class StackPushNode extends AbstractNodeWithCode {

    protected StackPushNode(final CompiledCodeObject code) {
        super(code);
    }

    public static StackPushNode create(final CompiledCodeObject code) {
        return StackPushNodeGen.create(code);
    }

    public abstract void executeWrite(VirtualFrame frame, Object value);

    @Specialization(guards = {"isVirtualized(frame)"})
    protected final void doWriteVirtualized(final VirtualFrame frame, final Object value,
                    @Cached("create(code)") final FrameStackWriteNode writeNode) {
        assert value != null;
        final int currentStackPointer = FrameUtil.getIntSafe(frame, code.stackPointerSlot);
        frame.setInt(code.stackPointerSlot, currentStackPointer + 1);
        writeNode.execute(frame, currentStackPointer, value);
    }

    @Fallback
    protected final void doWrite(final VirtualFrame frame, final Object value) {
        assert value != null;
        getContext(frame).push(value);
    }
}
