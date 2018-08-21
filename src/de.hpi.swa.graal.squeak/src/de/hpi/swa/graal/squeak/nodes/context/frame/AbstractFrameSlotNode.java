package de.hpi.swa.graal.squeak.nodes.context.frame;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.nodes.Node;

public abstract class AbstractFrameSlotNode extends Node {
    protected final FrameSlot frameSlot;

    protected AbstractFrameSlotNode(final FrameSlot frameSlot) {
        this.frameSlot = frameSlot;
    }
}
