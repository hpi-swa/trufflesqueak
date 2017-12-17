package de.hpi.swa.trufflesqueak.nodes.context.frame;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;

public abstract class FrameSlotReadNode extends FrameSlotNode {
    protected FrameSlotReadNode(FrameSlot frameSlot) {
        super(frameSlot);
    }

    public static FrameSlotReadNode create(FrameSlot frameSlot) {
        return FrameSlotReadNodeGen.create(frameSlot);
    }

    public abstract Object executeRead(VirtualFrame frame);

    @Specialization(guards = "isInt(frame)")
    public int readInt(VirtualFrame frame) {
        return FrameUtil.getIntSafe(frame, slot);
    }

    @Specialization(guards = "isLong(frame)", rewriteOn = ArithmeticException.class)
    public int readLongAsInt(VirtualFrame frame) {
        long longSafe = FrameUtil.getLongSafe(frame, slot);
        return Math.toIntExact(longSafe);
    }

    @Specialization(guards = "isLong(frame)")
    public long readLong(VirtualFrame frame) {
        return FrameUtil.getLongSafe(frame, slot);
    }

    @Specialization(guards = "isDouble(frame)")
    public double readDouble(VirtualFrame frame) {
        return FrameUtil.getDoubleSafe(frame, slot);
    }

    @Specialization(guards = "isBoolean(frame)")
    public boolean readBool(VirtualFrame frame) {
        return FrameUtil.getBooleanSafe(frame, slot);
    }

    @Specialization(guards = "isObject(frame)")
    public Object readObject(VirtualFrame frame) {
        return FrameUtil.getObjectSafe(frame, slot);
    }

    @Specialization(guards = "isIllegal(frame)")
    public Object readIllegal(@SuppressWarnings("unused") VirtualFrame frame) {
        return null;
    }
}
