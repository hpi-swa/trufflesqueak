package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public abstract class FrameSlotNode extends ContextAccessNode {
    protected final SlotGetter getter;

    protected FrameSlotNode(CompiledMethodObject cm, SlotGetter slotGetter) {
        super(cm);
        getter = slotGetter;
    }

    protected FrameSlot getSlot(VirtualFrame frame) {
        return getter.getSlot(FrameUtil.getIntSafe(frame, getMethod().stackPointerSlot), getMethod());
    }

    protected boolean isInt(VirtualFrame frame) {
        return getSlot(frame).getKind() == FrameSlotKind.Int;
    }

    protected boolean isLong(VirtualFrame frame) {
        return getSlot(frame).getKind() == FrameSlotKind.Long;
    }

    protected boolean isBoolean(VirtualFrame frame) {
        return getSlot(frame).getKind() == FrameSlotKind.Boolean;
    }

    protected boolean isIllegal(VirtualFrame frame) {
        return getSlot(frame).getKind() == FrameSlotKind.Illegal;
    }
}