/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.context.frame;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;

public abstract class FrameSlotWriteNode extends AbstractFrameSlotNode {
    public static FrameSlotWriteNode create(final FrameSlot slot) {
        return FrameSlotWriteNodeGen.create(slot);
    }

    public abstract void executeWrite(Frame frame, Object value);

    @Specialization(guards = "isBooleanOrIllegal(frame)")
    protected final void writeBool(final Frame frame, final boolean value) {
        /* Initialize type on first write. No-op if kind is already Boolean. */
        frame.getFrameDescriptor().setFrameSlotKind(getSlot(), FrameSlotKind.Boolean);

        frame.setBoolean(getSlot(), value);
    }

    @Specialization(guards = "isLongOrIllegal(frame)")
    protected final void writeLong(final Frame frame, final long value) {
        /* Initialize type on first write. No-op if kind is already Long. */
        frame.getFrameDescriptor().setFrameSlotKind(getSlot(), FrameSlotKind.Long);

        frame.setLong(getSlot(), value);
    }

    @Specialization(guards = "isDoubleOrIllegal(frame)")
    protected final void writeDouble(final Frame frame, final double value) {
        /* Initialize type on first write. No-op if kind is already Double. */
        frame.getFrameDescriptor().setFrameSlotKind(getSlot(), FrameSlotKind.Double);

        frame.setDouble(getSlot(), value);
    }

    @Specialization(replaces = {"writeBool", "writeLong", "writeDouble"})
    protected final void writeObject(final Frame frame, final Object value) {
        /* Initialize type on first write. No-op if kind is already Object. */
        frame.getFrameDescriptor().setFrameSlotKind(getSlot(), FrameSlotKind.Object);

        assert verifyWrite(value) : "Illegal write operation: " + value;
        frame.setObject(getSlot(), value);
    }

    protected final boolean isBooleanOrIllegal(final Frame frame) {
        final FrameSlotKind kind = frame.getFrameDescriptor().getFrameSlotKind(getSlot());
        return kind == FrameSlotKind.Boolean || kind == FrameSlotKind.Illegal;
    }

    protected final boolean isLongOrIllegal(final Frame frame) {
        final FrameSlotKind kind = frame.getFrameDescriptor().getFrameSlotKind(getSlot());
        return kind == FrameSlotKind.Long || kind == FrameSlotKind.Illegal;
    }

    protected final boolean isDoubleOrIllegal(final Frame frame) {
        final FrameSlotKind kind = frame.getFrameDescriptor().getFrameSlotKind(getSlot());
        return kind == FrameSlotKind.Double || kind == FrameSlotKind.Illegal;
    }

    private static boolean verifyWrite(final Object value) {
        return value != null && !(value instanceof Byte || value instanceof Integer || value instanceof Float);
    }
}
