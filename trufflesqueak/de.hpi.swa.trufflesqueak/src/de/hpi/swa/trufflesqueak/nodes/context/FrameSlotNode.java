package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

public abstract class FrameSlotNode extends Node {
    public final FrameSlot slot;

    protected FrameSlotNode(FrameSlot frameSlot) {
        slot = frameSlot;
    }

    protected boolean isInt(@SuppressWarnings("unused") VirtualFrame frame) {
        return slot.getKind() == FrameSlotKind.Int;
    }

    /**
     * @param frame Required so that Truffle doesn't consider the method as pure
     */
    protected boolean isLong(VirtualFrame frame) {
        return slot.getKind() == FrameSlotKind.Long;
    }

    protected boolean isDouble(@SuppressWarnings("unused") VirtualFrame frame) {
        return slot.getKind() == FrameSlotKind.Double;
    }

    protected boolean isBoolean(@SuppressWarnings("unused") VirtualFrame frame) {
        return slot.getKind() == FrameSlotKind.Boolean;
    }

    protected boolean isIllegal(@SuppressWarnings("unused") VirtualFrame frame) {
        return slot.getKind() == FrameSlotKind.Illegal;
    }

    protected boolean isObject(@SuppressWarnings("unused") VirtualFrame frame) {
        return slot.getKind() == FrameSlotKind.Object;
    }
}