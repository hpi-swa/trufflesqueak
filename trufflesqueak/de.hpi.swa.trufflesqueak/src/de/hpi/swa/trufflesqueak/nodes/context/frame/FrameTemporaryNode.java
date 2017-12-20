package de.hpi.swa.trufflesqueak.nodes.context.frame;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

public class FrameTemporaryNode extends SqueakNode {
    @Child private FrameSlotReadNode readNode;

    public FrameTemporaryNode(CompiledCodeObject code, int tempIndex) {
        readNode = FrameSlotReadNode.create(code.getTempSlot(tempIndex));
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return readNode.executeRead(frame);
    }
}
