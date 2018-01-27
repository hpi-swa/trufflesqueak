package de.hpi.swa.trufflesqueak.nodes.context.frame;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
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
    protected int readInt(VirtualFrame frame) {
        return FrameUtil.getIntSafe(frame, slot);
    }

    @Specialization(guards = "isLong(frame)", rewriteOn = ArithmeticException.class)
    protected int readLongAsInt(VirtualFrame frame) {
        long longSafe = FrameUtil.getLongSafe(frame, slot);
        return Math.toIntExact(longSafe);
    }

    @Specialization(guards = "isLong(frame)")
    protected long readLong(VirtualFrame frame) {
        return FrameUtil.getLongSafe(frame, slot);
    }

    @Specialization(guards = "isDouble(frame)")
    protected double readDouble(VirtualFrame frame) {
        return FrameUtil.getDoubleSafe(frame, slot);
    }

    @Specialization(guards = "isBoolean(frame)")
    protected boolean readBool(VirtualFrame frame) {
        return FrameUtil.getBooleanSafe(frame, slot);
    }

    @Specialization(guards = "isObject(frame)")
    protected Object readObject(VirtualFrame frame) {
        return FrameUtil.getObjectSafe(frame, slot);
    }

    @Specialization(guards = "isIllegal(frame)")
    protected Object readIllegal(@SuppressWarnings("unused") VirtualFrame frame) {
        throw new RuntimeException("Trying to read from illegal slot");
    }

    protected boolean isInt(VirtualFrame frame) {
        return frame.isInt(slot);
    }

    protected boolean isLong(VirtualFrame frame) {
        return frame.isLong(slot);
    }

    protected boolean isDouble(VirtualFrame frame) {
        return frame.isDouble(slot);
    }

    protected boolean isBoolean(VirtualFrame frame) {
        return frame.isBoolean(slot);
    }

    protected boolean isObject(VirtualFrame frame) {
        return frame.isObject(slot);
    }

    protected boolean isIllegal(@SuppressWarnings("unused") VirtualFrame frame) {
        return slot.getKind() == FrameSlotKind.Illegal;
    }
}
