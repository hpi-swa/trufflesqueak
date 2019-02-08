package de.hpi.swa.graal.squeak.nodes.context.frame;

import com.oracle.truffle.api.frame.FrameSlot;

import de.hpi.swa.graal.squeak.nodes.AbstractNode;

public abstract class AbstractFrameSlotNode extends AbstractNode {
    protected final FrameSlot frameSlot;

    protected AbstractFrameSlotNode(final FrameSlot frameSlot) {
        this.frameSlot = frameSlot;
    }
}
