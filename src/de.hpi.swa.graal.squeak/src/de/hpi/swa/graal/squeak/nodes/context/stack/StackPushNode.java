package de.hpi.swa.graal.squeak.nodes.context.stack;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.nodes.AbstractNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotReadNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotWriteNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackWriteNode;

public abstract class StackPushNode extends AbstractNode {
    @Child private FrameStackWriteNode writeNode = FrameStackWriteNode.create();
    @Child private FrameSlotReadNode stackPointerReadNode = FrameSlotReadNode.createForStackPointer();
    @Child private FrameSlotWriteNode stackPointerWriteNode = FrameSlotWriteNode.createForStackPointer();

    public static StackPushNode create() {
        return StackPushNodeGen.create();
    }

    public abstract void executeWrite(VirtualFrame frame, Object value);

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
