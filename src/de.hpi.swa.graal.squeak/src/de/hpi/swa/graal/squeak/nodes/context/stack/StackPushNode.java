package de.hpi.swa.graal.squeak.nodes.context.stack;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.nodes.AbstractWriteNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotReadNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotWriteNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackWriteNode;

public abstract class StackPushNode extends AbstractWriteNode {
    @Child private FrameStackWriteNode writeNode = FrameStackWriteNode.create();
    @Child private FrameSlotReadNode stackPointerReadNode;
    @Child private FrameSlotWriteNode stackPointerWriteNode;

    public static StackPushNode create(final CompiledCodeObject code) {
        return StackPushNodeGen.create(code);
    }

    protected StackPushNode(final CompiledCodeObject code) {
        super(code);
        stackPointerReadNode = FrameSlotReadNode.create(code.stackPointerSlot);
        stackPointerWriteNode = FrameSlotWriteNode.create(code.stackPointerSlot);
    }

    protected final int getFrameStackPointer(final VirtualFrame frame) {
        return (int) stackPointerReadNode.executeRead(frame);
    }

    protected final void setFrameStackPointer(final VirtualFrame frame, final int value) {
        stackPointerWriteNode.executeWrite(frame, value);
    }

    @Specialization(guards = {"isVirtualized(frame)"})
    protected final void doWriteVirtualized(final VirtualFrame frame, final Object value) {
        assert value != null;
        final int newSP = getFrameStackPointer(frame) + 1;
        writeNode.execute(frame, newSP, value);
        setFrameStackPointer(frame, newSP);
    }

    @Specialization(guards = {"!isVirtualized(frame)"})
    protected final void doWrite(final VirtualFrame frame, final Object value) {
        assert value != null;
        getContext(frame).push(value);
    }
}
