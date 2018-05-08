package de.hpi.swa.graal.squeak.nodes.context.stack;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.nodes.SqueakNodeWithCode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotReadNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotWriteNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackReadNode;

public abstract class AbstractStackNode extends SqueakNodeWithCode {
    @Child private FrameSlotReadNode stackPointerReadNode;
    @Child private FrameSlotWriteNode stackPointerWriteNode;
    @Child protected FrameStackReadNode readNode = FrameStackReadNode.create();

    public AbstractStackNode(final CompiledCodeObject code) {
        super(code);
        stackPointerReadNode = FrameSlotReadNode.create(code.stackPointerSlot);
        stackPointerWriteNode = FrameSlotWriteNode.create(code.stackPointerSlot);
    }

    protected int frameStackPointer(final VirtualFrame frame) {
        return (int) stackPointerReadNode.executeRead(frame);
    }

    protected void setFrameStackPointer(final VirtualFrame frame, final int value) {
        stackPointerWriteNode.executeWrite(frame, value);
    }
}
