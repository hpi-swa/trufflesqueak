package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public abstract class FrameSlotReadNode extends FrameSlotNode {
    protected FrameSlotReadNode(CompiledMethodObject cm, SlotGetter slotGetter) {
        super(cm, slotGetter);
    }

    public static FrameSlotReadNode create(CompiledMethodObject cm, FrameSlot frameSlot) {
        return FrameSlotReadNodeGen.create(cm, new SlotGetter(frameSlot));
    }

    public static FrameSlotReadNode peek(CompiledMethodObject cm, int offset) {
        return FrameSlotReadNodeGen.create(cm, new SlotGetter(offset));
    }

    public static FrameSlotReadNode top(CompiledMethodObject cm) {
        return peek(cm, 0);
    }

    public static FrameSlotReadNode temp(CompiledMethodObject cm, int index) {
        return create(cm, cm.stackSlots[index]);
    }

    public static FrameSlotReadNode receiver(CompiledMethodObject cm) {
        return create(cm, cm.receiverSlot);
    }

    @Specialization(guards = "isInt(frame)")
    public int readInt(VirtualFrame frame) {
        return FrameUtil.getIntSafe(frame, getSlot(frame));
    }

    @Specialization(guards = "isLong(frame)")
    public long readLong(VirtualFrame frame) {
        return FrameUtil.getLongSafe(frame, getSlot(frame));
    }

    @Specialization(guards = "isBoolean(frame)")
    public boolean readBool(VirtualFrame frame) {
        return FrameUtil.getBooleanSafe(frame, getSlot(frame));
    }

    @Specialization(replaces = {"readInt", "readLong", "readBool"})
    public Object readObject(VirtualFrame frame) {
        return FrameUtil.getObjectSafe(frame, getSlot(frame));
    }
}
