package de.hpi.swa.trufflesqueak.nodes.context.stack;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNodeWithCode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameSlotWriteNode;

public abstract class AbstractStackNode extends SqueakNodeWithCode {
    @Child private FrameSlotReadNode stackPointerReadNode;
    @Child private FrameSlotWriteNode stackPointerWriteNode;

    public AbstractStackNode(CompiledCodeObject code) {
        super(code);
        stackPointerReadNode = FrameSlotReadNode.create(code.stackPointerSlot);
        stackPointerWriteNode = FrameSlotWriteNode.create(code.stackPointerSlot);
    }

    protected int frameStackPointer(VirtualFrame frame) {
        return (int) stackPointerReadNode.executeRead(frame);
    }

    protected void setFrameStackPointer(VirtualFrame frame, int value) {
        stackPointerWriteNode.executeWrite(frame, value);
    }
}
