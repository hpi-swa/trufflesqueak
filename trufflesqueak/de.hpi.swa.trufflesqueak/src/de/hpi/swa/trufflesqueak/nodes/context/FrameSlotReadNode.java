package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public abstract class FrameSlotReadNode extends FrameSlotNode {
    protected FrameSlotReadNode(CompiledCodeObject cm, FrameSlot frameSlot) {
        super(cm, frameSlot);
    }

    public static FrameSlotReadNode create(CompiledCodeObject cm, FrameSlot frameSlot) {
        return FrameSlotReadNodeGen.create(cm, frameSlot);
    }

    public static FrameSlotReadNode temp(CompiledCodeObject cm, int index) {
        if (cm.stackSlots.length >= index) {
            return create(cm, cm.stackSlots[index]);
        }
        return null;
    }

    public static FrameSlotReadNode receiver(CompiledCodeObject method) {
        return create(method, method.receiverSlot);
    }

    @Specialization(guards = "isLong(frame)")
    public long readLong(VirtualFrame frame) {
        return FrameUtil.getLongSafe(frame, slot);
    }

    @Specialization(guards = "isBoolean(frame)")
    public boolean readBool(VirtualFrame frame) {
        return FrameUtil.getBooleanSafe(frame, slot);
    }

    @Specialization(replaces = {"readLong", "readBool"})
    public Object readObject(VirtualFrame frame) {
        return FrameUtil.getObjectSafe(frame, slot);
    }

    @Override
    public void prettyPrintOn(StringBuilder b) {
        b.append("temp").append(slot.getIdentifier());
    }
}
