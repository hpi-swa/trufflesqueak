package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

public class ReceiverNode extends Node {
    @Child private FrameStackReadNode readNode;

    public ReceiverNode() {
        readNode = FrameStackReadNode.create();
    }

    public Object execute(VirtualFrame frame) {
        return readNode.execute(frame, 0);
    }
}
