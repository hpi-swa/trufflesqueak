package de.hpi.swa.trufflesqueak.nodes.context.frame;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.nodes.WriteNode;

public abstract class FrameSlotWriteNode extends FrameSlotNode implements WriteNode {
    protected FrameSlotWriteNode(FrameSlot slot) {
        super(slot);
    }

    public static FrameSlotWriteNode create(FrameSlot slot) {
        return FrameSlotWriteNodeGen.create(slot);
    }

    protected boolean isNullWrite(VirtualFrame frame, Object value) {
        return isIllegal(frame) && value == null;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "isNullWrite(frame, value)")
    public void skipNullWrite(VirtualFrame frame, Object value) {
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

    @Specialization(replaces = {"skipNullWrite", "writeInt", "writeLong", "writeDouble", "writeBool"})
    public void writeObject(VirtualFrame frame, Object value) {
        slot.setKind(FrameSlotKind.Object);
        frame.setObject(slot, value);
    }
}
