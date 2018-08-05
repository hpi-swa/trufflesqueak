package de.hpi.swa.graal.squeak.nodes.context.frame;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;

public abstract class AbstractFrameSlotNode extends Node {
    protected final FrameDescriptor frameDescriptor;
    protected final FrameSlot slot;

    protected AbstractFrameSlotNode(final CompiledCodeObject codeObject, final FrameSlot frameSlot) {
        frameDescriptor = codeObject.getFrameDescriptor();
        slot = frameSlot;
    }
}
