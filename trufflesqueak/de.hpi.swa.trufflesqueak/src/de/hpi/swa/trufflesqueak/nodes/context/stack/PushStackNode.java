package de.hpi.swa.trufflesqueak.nodes.context.stack;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.FrameStackWriteNode;

public class PushStackNode extends AbstractStackNode {
    @Child private FrameStackWriteNode writeNode;

    public PushStackNode(CompiledCodeObject code) {
        super(code);
        writeNode = FrameStackWriteNode.create();
    }

    public void executeWrite(VirtualFrame frame, Object value) {
        int newSP = stackPointer(frame) + 1;
        writeNode.execute(frame, newSP, value);
        frame.setInt(code.stackPointerSlot, newSP);
    }
}
