package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNodeWithCode;

public class ReceiverReadNode extends SqueakNodeWithCode {
    @Child private FrameSlotReadNode readNode;

    public ReceiverReadNode(CompiledCodeObject code) {
        super(code);
        readNode = FrameSlotReadNode.create(code.receiverSlot);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return readNode.executeRead(frame);
    }

}
