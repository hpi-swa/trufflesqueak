package de.hpi.swa.trufflesqueak.nodes.context.frame;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;

public abstract class FrameSlotWriteNode extends FrameSlotNode {

    public static FrameSlotWriteNode create(FrameSlot slot) {
        return FrameSlotWriteNodeGen.create(slot);
    }

    protected FrameSlotWriteNode(FrameSlot slot) {
        super(slot);
    }

    public abstract void executeWrite(VirtualFrame frame, Object value);

    protected boolean isNullWrite(Object value) {
        return value == null;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "isNullWrite(value)")
    public void failOnNullWrite(VirtualFrame frame, Object value) {
        throw new RuntimeException("Should never write null to a frame slot");
    }

    @Specialization(guards = "isInt(frame) || isIllegal(frame)")
    public void writeInt(VirtualFrame frame, int value) {
        slot.setKind(FrameSlotKind.Int);
        frame.setInt(slot, value);
    }

    @Specialization(guards = "isLong(frame) || isIllegal(frame)")
    public void writeLong(VirtualFrame frame, long value) {
        slot.setKind(FrameSlotKind.Long);
        frame.setLong(slot, value);
    }

    @Specialization(guards = "isDouble(frame) || isIllegal(frame)")
    public void writeDouble(VirtualFrame frame, double value) {
        slot.setKind(FrameSlotKind.Double);
        frame.setDouble(slot, value);
    }

    @Specialization(guards = "isBoolean(frame) || isIllegal(frame)")
    public void writeBool(VirtualFrame frame, boolean value) {
        slot.setKind(FrameSlotKind.Boolean);
        frame.setBoolean(slot, value);
    }

    @Specialization(replaces = {"failOnNullWrite", "writeInt", "writeLong", "writeDouble", "writeBool"})
    public void writeObject(VirtualFrame frame, Object value) {
        slot.setKind(FrameSlotKind.Object);
        frame.setObject(slot, value);
    }
}
