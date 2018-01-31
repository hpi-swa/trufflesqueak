package de.hpi.swa.trufflesqueak.nodes.context.stack;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameSlotWriteNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackWriteNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public abstract class PushStackNode extends AbstractWriteNode {
    @Child private FrameStackWriteNode writeNode = FrameStackWriteNode.create();
    @Child private FrameSlotReadNode stackPointerReadNode;
    @Child private FrameSlotWriteNode stackPointerWriteNode;

    public static PushStackNode create(CompiledCodeObject code) {
        return PushStackNodeGen.create(code);
    }

    protected PushStackNode(CompiledCodeObject code) {
        super(code);
        stackPointerReadNode = FrameSlotReadNode.create(code.stackPointerSlot);
        stackPointerWriteNode = FrameSlotWriteNode.create(code.stackPointerSlot);
    }

    protected int getFrameStackPointer(VirtualFrame frame) {
        return (int) stackPointerReadNode.executeRead(frame);
    }

    protected void setFrameStackPointer(VirtualFrame frame, int value) {
        stackPointerWriteNode.executeWrite(frame, value);
    }

    @Specialization(guards = {"isVirtualized(frame)"})
    protected void doWriteVirtualized(VirtualFrame frame, Object value) {
        assert value != null;
        int newSP = getFrameStackPointer(frame) + 1;
        writeNode.execute(frame, newSP, value);
        setFrameStackPointer(frame, newSP);
    }

    @Specialization(guards = {"!isVirtualized(frame)"})
    protected void doWrite(VirtualFrame frame, Object value) {
        assert value != null;
        FrameAccess.getContext(frame).push(value);
    }
}
