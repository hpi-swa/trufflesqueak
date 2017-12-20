package de.hpi.swa.trufflesqueak.nodes.context.stack;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameSlotReadNode;

public abstract class AbstractStackNode extends SqueakNode {
    protected final CompiledCodeObject code;
    @Child private FrameSlotReadNode spNode;

    public AbstractStackNode(CompiledCodeObject code) {
        super();
        this.code = code;
        this.spNode = FrameSlotReadNode.create(code.stackPointerSlot);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        throw new RuntimeException("Cannot be executed like this");
    }

    protected int stackPointer(VirtualFrame frame) {
        return (int) spNode.executeRead(frame);
    }
}
