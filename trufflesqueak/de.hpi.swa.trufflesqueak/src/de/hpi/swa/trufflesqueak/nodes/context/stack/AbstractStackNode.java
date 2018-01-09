package de.hpi.swa.trufflesqueak.nodes.context.stack;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNodeWithCode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameSlotReadNode;

public abstract class AbstractStackNode extends SqueakNodeWithCode {
    @Child private FrameSlotReadNode spNode;

    public AbstractStackNode(CompiledCodeObject code) {
        super(code);
        this.spNode = FrameSlotReadNode.create(code.stackPointerSlot);
    }

    protected int frameStackPointer(VirtualFrame frame) {
        return (int) spNode.executeRead(frame);
    }
}
