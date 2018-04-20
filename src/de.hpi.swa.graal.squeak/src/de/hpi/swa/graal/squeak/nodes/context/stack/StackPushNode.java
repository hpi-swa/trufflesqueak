package de.hpi.swa.graal.squeak.nodes.context.stack;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotReadNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotWriteNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackWriteNode;
import de.hpi.swa.graal.squeak.nodes.helpers.AbstractWriteNode;

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

    protected long getFrameStackPointer(final VirtualFrame frame) {
        return (long) stackPointerReadNode.executeRead(frame);
    }

    protected void setFrameStackPointer(final VirtualFrame frame, final long value) {
        stackPointerWriteNode.executeWrite(frame, value);
    }

    @Specialization(guards = {"isVirtualized(frame)"})
    protected void doWriteVirtualized(final VirtualFrame frame, final Object value) {
        CompilerDirectives.ensureVirtualizedHere(frame);
        assert value != null;
        final long newSP = getFrameStackPointer(frame) + 1;
        writeNode.execute(frame, (int) newSP, value);
        setFrameStackPointer(frame, newSP);
    }

    @Specialization(guards = {"!isVirtualized(frame)"})
    protected void doWrite(final VirtualFrame frame, final Object value) {
        assert value != null;
        getContext(frame).push(value);
    }
}
