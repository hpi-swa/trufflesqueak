package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public abstract class FrameSlotReadNode extends FrameSlotNode {
    protected FrameSlotReadNode(CompiledMethodObject cm, FrameSlot frameSlot) {
        super(cm, frameSlot);
    }

    public static FrameSlotReadNode create(CompiledMethodObject cm, FrameSlot frameSlot) {
        return FrameSlotReadNodeGen.create(cm, frameSlot);
    }

    public static FrameSlotReadNode temp(CompiledMethodObject cm, int index) {
        return create(cm, cm.stackSlots[index]);
    }

    public static FrameSlotReadNode receiver(CompiledMethodObject cm) {
        return create(cm, cm.receiverSlot);
    }

    @Specialization(guards = "isInt()")
    public int readInt(VirtualFrame frame) {
        return FrameUtil.getIntSafe(frame, slot);
    }

    @Specialization(guards = "isLong()")
    public long readLong(VirtualFrame frame) {
        return FrameUtil.getLongSafe(frame, slot);
    }

    @Specialization(guards = "isBoolean()")
    public boolean readBool(VirtualFrame frame) {
        return FrameUtil.getBooleanSafe(frame, slot);
    }

    @Specialization(replaces = {"readInt", "readLong", "readBool"})
    public Object readObject(VirtualFrame frame) {
        return FrameUtil.getObjectSafe(frame, slot);
    }
}
