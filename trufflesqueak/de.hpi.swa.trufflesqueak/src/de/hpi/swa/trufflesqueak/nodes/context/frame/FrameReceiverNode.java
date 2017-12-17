package de.hpi.swa.trufflesqueak.nodes.context.frame;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

public class FrameReceiverNode extends SqueakNode {
    @Child private FrameSlotReadNode readNode;

    public FrameReceiverNode(CompiledCodeObject code) {
        readNode = FrameSlotReadNode.create(code.stackSlots[0]);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return readNode.executeRead(frame);
    }
}
