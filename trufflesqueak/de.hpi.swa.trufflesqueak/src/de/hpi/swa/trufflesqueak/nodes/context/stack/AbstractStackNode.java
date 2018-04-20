package de.hpi.swa.trufflesqueak.nodes.context.stack;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNodeWithCode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameSlotWriteNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackReadNode;

public abstract class AbstractStackNode extends SqueakNodeWithCode {
    @Child private FrameSlotReadNode stackPointerReadNode;
    @Child private FrameSlotWriteNode stackPointerWriteNode;
    @Child protected FrameStackReadNode readNode = FrameStackReadNode.create();

    public AbstractStackNode(final CompiledCodeObject code) {
        super(code);
        stackPointerReadNode = FrameSlotReadNode.create(code.stackPointerSlot);
        stackPointerWriteNode = FrameSlotWriteNode.create(code.stackPointerSlot);
    }

    protected long frameStackPointer(final VirtualFrame frame) {
        return (long) stackPointerReadNode.executeRead(frame);
    }

    protected void setFrameStackPointer(final VirtualFrame frame, final long value) {
        stackPointerWriteNode.executeWrite(frame, value);
    }
}
