package de.hpi.swa.graal.squeak.nodes.context.frame;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;

public abstract class FrameSlotWriteNode extends AbstractFrameSlotNode {

    public static FrameSlotWriteNode create(final FrameSlot slot) {
        return FrameSlotWriteNodeGen.create(slot);
    }

    public static FrameSlotWriteNode createForContextOrMarker() {
        return FrameSlotWriteNodeGen.create(CompiledCodeObject.thisContextOrMarkerSlot);
    }

    public static FrameSlotWriteNode createForInstructionPointer() {
        return FrameSlotWriteNodeGen.create(CompiledCodeObject.instructionPointerSlot);
    }

    public static FrameSlotWriteNode createForStackPointer() {
        return FrameSlotWriteNodeGen.create(CompiledCodeObject.stackPointerSlot);
    }

    protected FrameSlotWriteNode(final FrameSlot slot) {
        super(slot);
    }

    public abstract void executeWrite(VirtualFrame frame, Object value);

    @Specialization(guards = "isIntSlot(value)")
    protected final void writeInt(final VirtualFrame frame, final int value) {
        frame.setInt(slot, value);
    }

    @Specialization(guards = "isLongSlot(value)")
    protected final void writeLong(final VirtualFrame frame, final long value) {
        frame.setLong(slot, value);
    }

    @Specialization(guards = "isDoubleSlot(value)")
    protected final void writeDouble(final VirtualFrame frame, final double value) {
        frame.setDouble(slot, value);
    }

    @Specialization(guards = "isBooleanSlot(value)")
    protected final void writeBool(final VirtualFrame frame, final boolean value) {
        frame.setBoolean(slot, value);
    }

    @Specialization(replaces = {"writeInt", "writeLong", "writeDouble", "writeBool"})
    protected final void writeObject(final VirtualFrame frame, final Object value) {
        assert value != null;
        frame.setObject(slot, value);
    }

    // uses `value` to make sure guard is not converted to assertion
    protected final boolean isIntSlot(@SuppressWarnings("unused") final long value) {
        if (slot.getKind() == FrameSlotKind.Int) {
            return true;
        }
        if (slot.getKind() == FrameSlotKind.Illegal) {
            slot.setKind(FrameSlotKind.Int);
            return true;
        }
        return false;
    }

    protected final boolean isLongSlot(@SuppressWarnings("unused") final long value) {
        if (slot.getKind() == FrameSlotKind.Long) {
            return true;
        }
        if (slot.getKind() == FrameSlotKind.Illegal) {
            slot.setKind(FrameSlotKind.Long);
            return true;
        }
        return false;
    }

    protected final boolean isDoubleSlot(@SuppressWarnings("unused") final double value) {
        if (slot.getKind() == FrameSlotKind.Double) {
            return true;
        }
        if (slot.getKind() == FrameSlotKind.Illegal) {
            slot.setKind(FrameSlotKind.Double);
            return true;
        }
        return false;
    }

    protected final boolean isBooleanSlot(@SuppressWarnings("unused") final boolean value) {
        if (slot.getKind() == FrameSlotKind.Boolean) {
            return true;
        }
        if (slot.getKind() == FrameSlotKind.Illegal) {
            slot.setKind(FrameSlotKind.Boolean);
            return true;
        }
        return false;
    }

    protected final boolean isObjectSlot(@SuppressWarnings("unused") final Object value) {
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
