package de.hpi.swa.graal.squeak.nodes.context.frame;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;

public abstract class FrameSlotWriteNode extends AbstractFrameSlotNode {

    public static FrameSlotWriteNode create(final CompiledCodeObject code, final FrameSlot slot) {
        return FrameSlotWriteNodeGen.create(code, slot);
    }

    protected FrameSlotWriteNode(final CompiledCodeObject code, final FrameSlot slot) {
        super(code, slot);
    }

    public abstract void executeWrite(Frame frame, Object value);

    @Specialization(guards = "isIntSlot(value)")
    protected final void writeInt(final Frame frame, final int value) {
        frame.setInt(slot, value);
    }

    @Specialization(guards = "isLongSlot(value)")
    protected final void writeLong(final Frame frame, final long value) {
        frame.setLong(slot, value);
    }

    @Specialization(guards = "isDoubleSlot(value)")
    protected final void writeDouble(final Frame frame, final double value) {
        frame.setDouble(slot, value);
    }

    @Specialization(guards = "isBooleanSlot(value)")
    protected final void writeBool(final Frame frame, final boolean value) {
        frame.setBoolean(slot, value);
    }

    @Specialization(replaces = {"writeInt", "writeLong", "writeDouble", "writeBool"})
    protected final void writeObject(final Frame frame, final Object value) {
        assert value != null;
        frame.setObject(slot, value);
    }

    // uses `value` to make sure guard is not converted to assertion
    protected final boolean isIntSlot(@SuppressWarnings("unused") final long value) {
        if (frameDescriptor.getFrameSlotKind(slot) == FrameSlotKind.Int) {
            return true;
        }
        if (frameDescriptor.getFrameSlotKind(slot) == FrameSlotKind.Illegal) {
            frameDescriptor.setFrameSlotKind(slot, FrameSlotKind.Int);
            return true;
        }
        return false;
    }

    protected final boolean isLongSlot(@SuppressWarnings("unused") final long value) {
        if (frameDescriptor.getFrameSlotKind(slot) == FrameSlotKind.Long) {
            return true;
        }
        if (frameDescriptor.getFrameSlotKind(slot) == FrameSlotKind.Illegal) {
            frameDescriptor.setFrameSlotKind(slot, FrameSlotKind.Long);
            return true;
        }
        return false;
    }

    protected final boolean isDoubleSlot(@SuppressWarnings("unused") final double value) {
        if (frameDescriptor.getFrameSlotKind(slot) == FrameSlotKind.Double) {
            return true;
        }
        if (frameDescriptor.getFrameSlotKind(slot) == FrameSlotKind.Illegal) {
            frameDescriptor.setFrameSlotKind(slot, FrameSlotKind.Double);
            return true;
        }
        return false;
    }

    protected final boolean isBooleanSlot(@SuppressWarnings("unused") final boolean value) {
        if (frameDescriptor.getFrameSlotKind(slot) == FrameSlotKind.Boolean) {
            return true;
        }
        if (frameDescriptor.getFrameSlotKind(slot) == FrameSlotKind.Illegal) {
            frameDescriptor.setFrameSlotKind(slot, FrameSlotKind.Boolean);
            return true;
        }
        return false;
    }

    protected final boolean isObjectSlot(@SuppressWarnings("unused") final Object value) {
        if (frameDescriptor.getFrameSlotKind(slot) == FrameSlotKind.Object) {
            return true;
        }
        if (frameDescriptor.getFrameSlotKind(slot) == FrameSlotKind.Illegal) {
            frameDescriptor.setFrameSlotKind(slot, FrameSlotKind.Object);
            return true;
        }
        return false;
    }
}
