package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNodeWithCode;

public class ReceiverWriteNode extends SqueakNodeWithCode {
    @Child private FrameSlotWriteNode writeNode;

    public ReceiverWriteNode(CompiledCodeObject code) {
        super(code);
        writeNode = FrameSlotWriteNode.create(code.receiverSlot);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        writeNode.executeWrite(frame, frame.getArguments()[0]);
        return code.image.nil;
    }

}
