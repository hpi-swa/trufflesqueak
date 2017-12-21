package de.hpi.swa.trufflesqueak.nodes.context.frame;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

public class FrameTemporaryReadNode extends SqueakNode {
    @Child private FrameSlotReadNode readNode;

    public static SqueakNode create(CompiledCodeObject code, int tempIndex) {
        int stackIndex = code.convertTempIndexToStackIndex(tempIndex);
        if (stackIndex >= 0) {
            return new FrameTemporaryReadNode(code, stackIndex);
        } else {
            return new FrameArgumentNode(1 + tempIndex);
        }
    }

    private FrameTemporaryReadNode(CompiledCodeObject code, int stackIndex) {
        readNode = FrameSlotReadNode.create(code.getStackSlot(stackIndex));
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return readNode.executeRead(frame);
    }
}
