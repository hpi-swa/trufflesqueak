package de.hpi.swa.graal.squeak.nodes.context.stack;

import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithCode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackWriteNode;

public final class StackPushNode extends AbstractNodeWithCode {
    @Child private FrameStackWriteNode writeNode;

    private StackPushNode(final CompiledCodeObject code) {
        super(code);
        writeNode = FrameStackWriteNode.create(code);
    }

    public static StackPushNode create(final CompiledCodeObject code) {
        return new StackPushNode(code);
    }

    public void executeWrite(final VirtualFrame frame, final Object value) {
        assert value != null;
        final int currentStackPointer = FrameUtil.getIntSafe(frame, code.getStackPointerSlot());
        frame.setInt(code.getStackPointerSlot(), currentStackPointer + 1);
        writeNode.execute(frame, currentStackPointer, value);
    }
}
