package de.hpi.swa.trufflesqueak.nodes.context.frame;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

public class FrameReceiverAndArgumentsNode extends SqueakNode {

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return frame.getArguments();
    }
}
