package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class ReceiverNode extends Node {
    @Child private FrameSlotReadNode readNode;

    public ReceiverNode(CompiledCodeObject code) {
        readNode = FrameSlotReadNode.create(code.stackSlots[0]);
    }

    public Object execute(VirtualFrame frame) {
        return readNode.executeRead(frame);
    }
}
