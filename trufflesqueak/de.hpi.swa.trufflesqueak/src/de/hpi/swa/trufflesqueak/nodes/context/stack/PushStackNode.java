package de.hpi.swa.trufflesqueak.nodes.context.stack;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackWriteNode;

public abstract class PushStackNode extends AbstractWriteNode {
    @Child private FrameStackWriteNode writeNode;
    @Child private FrameSlotReadNode spNode;

    public static PushStackNode create(CompiledCodeObject code) {
        return PushStackNodeGen.create(code);
    }

    protected PushStackNode(CompiledCodeObject code) {
        super(code);
        this.spNode = FrameSlotReadNode.create(code.stackPointerSlot);
        writeNode = FrameStackWriteNode.create();
    }

    protected int frameStackPointer(VirtualFrame frame) {
        return (int) spNode.executeRead(frame);
    }

    @Specialization(guards = {"isVirtualized(frame)"})
    protected void doWriteVirtualized(VirtualFrame frame, Object value) {
        assert value != null;
        int sp = frameStackPointer(frame);
        writeNode.execute(frame, sp, value);
        frame.setInt(code.stackPointerSlot, sp + 1);
    }

    @Specialization(guards = {"!isVirtualized(frame)"})
    protected void doWrite(VirtualFrame frame, Object value) {
        getContext(frame).push(value);
    }
}
