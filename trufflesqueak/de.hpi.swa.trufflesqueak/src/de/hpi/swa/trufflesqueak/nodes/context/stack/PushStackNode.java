package de.hpi.swa.trufflesqueak.nodes.context.stack;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackWriteNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public abstract class PushStackNode extends AbstractWriteNode {
    @Child private FrameStackWriteNode writeNode = FrameStackWriteNode.create();
    @Child private FrameSlotReadNode spNode;

    public static PushStackNode create(CompiledCodeObject code) {
        return PushStackNodeGen.create(code);
    }

    protected PushStackNode(CompiledCodeObject code) {
        super(code);
        spNode = FrameSlotReadNode.create(code.stackPointerSlot);
    }

    protected int frameStackPointer(VirtualFrame frame) {
        return (int) spNode.executeRead(frame);
    }

    @Specialization(guards = {"isVirtualized(frame)"})
    protected void doWriteVirtualized(VirtualFrame frame, Object value) {
        assert value != null;
        int newSP = frameStackPointer(frame) + 1;
        writeNode.execute(frame, newSP, value);
        frame.setInt(code.stackPointerSlot, newSP);
    }

    @Specialization(guards = {"!isVirtualized(frame)"})
    protected void doWrite(VirtualFrame frame, Object value) {
        assert value != null;
        FrameAccess.getContext(frame).push(value);
    }
}
