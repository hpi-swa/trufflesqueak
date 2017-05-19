package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNodeWithMethod;

public abstract class FrameSlotNode extends SqueakNodeWithMethod {
    final FrameSlot slot;

    protected FrameSlotNode(CompiledCodeObject cm, FrameSlot frameSlot) {
        super(cm);
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

    protected boolean isBoolean(@SuppressWarnings("unused") VirtualFrame frame) {
        return slot.getKind() == FrameSlotKind.Boolean;
    }

    protected boolean isIllegal(@SuppressWarnings("unused") VirtualFrame frame) {
        return slot.getKind() == FrameSlotKind.Illegal;
    }
}