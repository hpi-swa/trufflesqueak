package de.hpi.swa.graal.squeak.nodes.context.frame;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;

public abstract class FrameSlotWriteNode extends FrameSlotNode {

    public static FrameSlotWriteNode create(final FrameSlot slot) {
        return FrameSlotWriteNodeGen.create(slot);
    }

    protected FrameSlotWriteNode(final FrameSlot slot) {
        super(slot);
    }

    public abstract void executeWrite(VirtualFrame frame, Object value);

    @Specialization(guards = "isIntSlot(value)")
    protected void writeInt(final VirtualFrame frame, final int value) {
        frame.setInt(slot, value);
    }

    @Specialization(guards = "isLongSlot(value)")
    protected void writeLong(final VirtualFrame frame, final long value) {
        frame.setLong(slot, value);
    }

    @Specialization(guards = "isDoubleSlot(value)")
    protected void writeDouble(final VirtualFrame frame, final double value) {
        frame.setDouble(slot, value);
    }

    @Specialization(guards = "isBooleanSlot(value)")
    protected void writeBool(final VirtualFrame frame, final boolean value) {
        frame.setBoolean(slot, value);
    }

    @Specialization(replaces = {"writeInt", "writeLong", "writeDouble", "writeBool"})
    protected void writeObject(final VirtualFrame frame, final Object value) {
        assert value != null;
        frame.setObject(slot, value);
    }

    // uses `value` to make sure guard is not converted to assertion
    protected boolean isIntSlot(@SuppressWarnings("unused") final long value) {
        if (slot.getKind() == FrameSlotKind.Int) {
            return true;
        }
        if (slot.getKind() == FrameSlotKind.Illegal) {
            slot.setKind(FrameSlotKind.Int);
            return true;
        }
        return false;
    }

    protected boolean isLongSlot(@SuppressWarnings("unused") final long value) {
        if (slot.getKind() == FrameSlotKind.Long) {
            return true;
        }
        if (slot.getKind() == FrameSlotKind.Illegal) {
            slot.setKind(FrameSlotKind.Long);
            return true;
        }
        return false;
    }

    protected boolean isDoubleSlot(@SuppressWarnings("unused") final double value) {
        if (slot.getKind() == FrameSlotKind.Double) {
            return true;
        }
        if (slot.getKind() == FrameSlotKind.Illegal) {
            slot.setKind(FrameSlotKind.Double);
            return true;
        }
        return false;
    }

    protected boolean isBooleanSlot(@SuppressWarnings("unused") final boolean value) {
        if (slot.getKind() == FrameSlotKind.Boolean) {
            return true;
        }
        if (slot.getKind() == FrameSlotKind.Illegal) {
            slot.setKind(FrameSlotKind.Boolean);
            return true;
        }
        return false;
    }

    protected boolean isObjectSlot(@SuppressWarnings("unused") final Object value) {
        if (slot.getKind() == FrameSlotKind.Object) {
            return true;
        }
        if (slot.getKind() == FrameSlotKind.Illegal) {
            slot.setKind(FrameSlotKind.Object);
            return true;
        }
        return false;
    }
}
