package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNodeWithMethod;

public abstract class FrameSlotNode extends SqueakNodeWithMethod {
    final FrameSlot slot;

    protected FrameSlotNode(CompiledMethodObject cm, FrameSlot frameSlot) {
        super(cm);
        slot = frameSlot;
    }

    protected boolean isInt() {
        return slot.getKind() == FrameSlotKind.Int;
    }

    protected boolean isLong() {
        return slot.getKind() == FrameSlotKind.Long;
    }

    protected boolean isBoolean() {
        return slot.getKind() == FrameSlotKind.Boolean;
    }

    protected boolean isIllegal() {
        return slot.getKind() == FrameSlotKind.Illegal;
    }
}