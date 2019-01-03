package de.hpi.swa.graal.squeak.nodes.context.frame;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;

public abstract class FrameSlotWriteNode extends AbstractFrameSlotNode {

    public static FrameSlotWriteNode create(final FrameSlot slot) {
        return FrameSlotWriteNodeGen.create(slot);
    }

    protected FrameSlotWriteNode(final FrameSlot slot) {
        super(slot);
    }

    public abstract void executeWrite(Frame frame, Object value);

    @Specialization(guards = "isBooleanOrIllegal(frame)")
    protected final void writeBool(final Frame frame, final boolean value) {
        /* Initialize type on first write. No-op if kind is already Boolean. */
        frame.getFrameDescriptor().setFrameSlotKind(frameSlot, FrameSlotKind.Boolean);

        frame.setBoolean(frameSlot, value);
    }

    @Specialization(guards = "isLongOrIllegal(frame)")
    protected final void writeLong(final Frame frame, final long value) {
        /* Initialize type on first write. No-op if kind is already Long. */
        frame.getFrameDescriptor().setFrameSlotKind(frameSlot, FrameSlotKind.Long);

        frame.setLong(frameSlot, value);
    }

    @Specialization(guards = "isDoubleOrIllegal(frame)")
    protected final void writeDouble(final Frame frame, final double value) {
        /* Initialize type on first write. No-op if kind is already Double. */
        frame.getFrameDescriptor().setFrameSlotKind(frameSlot, FrameSlotKind.Double);

        frame.setDouble(frameSlot, value);
    }

    @Specialization(replaces = {"writeBool", "writeLong", "writeDouble"})
    protected final void writeObject(final Frame frame, final Object value) {
        assert !(value instanceof Byte || value instanceof Integer || value instanceof Float) : "Illegal write operation";

        /* Initialize type on first write. No-op if kind is already Object. */
        frame.getFrameDescriptor().setFrameSlotKind(frameSlot, FrameSlotKind.Object);

        assert value != null;
        frame.setObject(frameSlot, value);
    }

    protected final boolean isBooleanOrIllegal(final Frame frame) {
        final FrameSlotKind kind = frame.getFrameDescriptor().getFrameSlotKind(frameSlot);
        return kind == FrameSlotKind.Boolean || kind == FrameSlotKind.Illegal;
    }

    protected final boolean isLongOrIllegal(final Frame frame) {
        final FrameSlotKind kind = frame.getFrameDescriptor().getFrameSlotKind(frameSlot);
        return kind == FrameSlotKind.Long || kind == FrameSlotKind.Illegal;
    }

    protected final boolean isDoubleOrIllegal(final Frame frame) {
        final FrameSlotKind kind = frame.getFrameDescriptor().getFrameSlotKind(frameSlot);
        return kind == FrameSlotKind.Double || kind == FrameSlotKind.Illegal;
    }
}
