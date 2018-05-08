package de.hpi.swa.graal.squeak.nodes.context.frame;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.exceptions.SqueakException;

public abstract class FrameSlotReadNode extends FrameSlotNode {

    public static FrameSlotReadNode create(final FrameSlot frameSlot) {
        return FrameSlotReadNodeGen.create(frameSlot);
    }

    protected FrameSlotReadNode(final FrameSlot frameSlot) {
        super(frameSlot);
    }

    public abstract Object executeRead(Frame frame);

    @Specialization(guards = "isInt(frame)")
    protected int readInt(final VirtualFrame frame) {
        return FrameUtil.getIntSafe(frame, slot);
    }

    @Specialization(guards = "isLong(frame)")
    protected long readLong(final VirtualFrame frame) {
        return FrameUtil.getLongSafe(frame, slot);
    }

    @Specialization(guards = "isDouble(frame)")
    protected double readDouble(final VirtualFrame frame) {
        return FrameUtil.getDoubleSafe(frame, slot);
    }

    @Specialization(guards = "isBoolean(frame)")
    protected boolean readBool(final VirtualFrame frame) {
        return FrameUtil.getBooleanSafe(frame, slot);
    }

    @Specialization(guards = "isObject(frame)")
    protected Object readObject(final VirtualFrame frame) {
        return FrameUtil.getObjectSafe(frame, slot);
    }

    @Specialization(guards = "isIllegal(frame)")
    protected Object readIllegal(@SuppressWarnings("unused") final VirtualFrame frame) {
        throw new SqueakException("Trying to read from illegal slot");
    }

    protected boolean isInt(final VirtualFrame frame) {
        return frame.isInt(slot);
    }

    protected boolean isLong(final VirtualFrame frame) {
        return frame.isLong(slot);
    }

    protected boolean isDouble(final VirtualFrame frame) {
        return frame.isDouble(slot);
    }

    protected boolean isBoolean(final VirtualFrame frame) {
        return frame.isBoolean(slot);
    }

    protected boolean isObject(final VirtualFrame frame) {
        return frame.isObject(slot);
    }

    protected boolean isIllegal(@SuppressWarnings("unused") final VirtualFrame frame) {
        return slot.getKind() == FrameSlotKind.Illegal;
    }
}
