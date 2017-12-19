package de.hpi.swa.trufflesqueak.nodes.context.frame;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

public class FrameArgumentNode extends SqueakNode {
    private final int argumentIndex;

    public FrameArgumentNode(int argumentIndex) {
        super();
        this.argumentIndex = argumentIndex;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return frame.getArguments()[argumentIndex];
    }
}
