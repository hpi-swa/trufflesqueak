package de.hpi.swa.trufflesqueak.nodes.context.stack;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotReadNode;

public abstract class AbstractStackNode extends Node {
    protected final CompiledCodeObject code;
    @Child private FrameSlotReadNode spNode;

    public AbstractStackNode(CompiledCodeObject code) {
        super();
        this.code = code;
        this.spNode = FrameSlotReadNode.create(code.stackPointerSlot);
    }

    protected int stackPointer(VirtualFrame frame) {
        return (int) spNode.executeRead(frame);
    }
}
