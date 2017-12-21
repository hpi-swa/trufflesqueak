package de.hpi.swa.trufflesqueak.nodes.context.stack;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackReadNode;

public class PopStackNode extends AbstractStackNode {
    @Child private FrameStackReadNode readNode;

    public PopStackNode(CompiledCodeObject code) {
        super(code);
        readNode = FrameStackReadNode.create();
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        int sp = stackPointer(frame);
        frame.setInt(code.stackPointerSlot, sp - 1);
        if (sp < 0) {
            return frame.getArguments()[frame.getArguments().length + sp];
        }
        return readNode.execute(frame, sp);
    }
}
