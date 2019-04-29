package de.hpi.swa.graal.squeak.nodes.context.frame;

import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;

public abstract class AbstractFrameSlotReadNode extends AbstractFrameSlotNode {
    protected AbstractFrameSlotReadNode(final FrameSlot frameSlot) {
        super(frameSlot);
    }

    public abstract Object executeRead(Frame frame);
}
