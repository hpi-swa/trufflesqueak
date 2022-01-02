/*
 * Copyright (c) 2017-2022 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2022 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.context.frame;

import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlotKind;

import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackWriteNodeFactory.FrameSlotWriteNodeGen;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public abstract class FrameStackWriteNode extends AbstractNode {
    public static FrameStackWriteNode create(final Frame frame, final int index) {
        final int numArgs = FrameAccess.getNumArguments(frame);
        if (index < numArgs) {
            return new FrameArgumentWriteNode(index);
        } else {
            return FrameSlotWriteNodeGen.create(FrameAccess.toStackSlotIndex(frame, index));
        }
    }

    public abstract void executeWrite(Frame frame, Object value);

    @NodeField(name = "slotIndex", type = int.class)
    public abstract static class FrameSlotWriteNode extends FrameStackWriteNode {

        protected abstract int getSlotIndex();

        @Specialization(guards = "isBooleanOrIllegal(frame)")
        protected final void writeBool(final Frame frame, final boolean value) {
            /* Initialize type on first write. No-op if kind is already Boolean. */
            frame.getFrameDescriptor().setSlotKind(getSlotIndex(), FrameSlotKind.Boolean);

            frame.setBoolean(getSlotIndex(), value);
        }

        @Specialization(guards = "isLongOrIllegal(frame)")
        protected final void writeLong(final Frame frame, final long value) {
            /* Initialize type on first write. No-op if kind is already Long. */
            frame.getFrameDescriptor().setSlotKind(getSlotIndex(), FrameSlotKind.Long);

            frame.setLong(getSlotIndex(), value);
        }

        @Specialization(guards = "isDoubleOrIllegal(frame)")
        protected final void writeDouble(final Frame frame, final double value) {
            /* Initialize type on first write. No-op if kind is already Double. */
            frame.getFrameDescriptor().setSlotKind(getSlotIndex(), FrameSlotKind.Double);

            frame.setDouble(getSlotIndex(), value);
        }

        @Specialization(replaces = {"writeBool", "writeLong", "writeDouble"})
        protected final void writeObject(final Frame frame, final Object value) {
            /* Initialize type on first write. No-op if kind is already Object. */
            frame.getFrameDescriptor().setSlotKind(getSlotIndex(), FrameSlotKind.Object);

            frame.setObject(getSlotIndex(), value);
        }

        protected final boolean isBooleanOrIllegal(final Frame frame) {
            final FrameSlotKind kind = frame.getFrameDescriptor().getSlotKind(getSlotIndex());
            return kind == FrameSlotKind.Boolean || kind == FrameSlotKind.Illegal;
        }

        protected final boolean isLongOrIllegal(final Frame frame) {
            final FrameSlotKind kind = frame.getFrameDescriptor().getSlotKind(getSlotIndex());
            return kind == FrameSlotKind.Long || kind == FrameSlotKind.Illegal;
        }

        protected final boolean isDoubleOrIllegal(final Frame frame) {
            final FrameSlotKind kind = frame.getFrameDescriptor().getSlotKind(getSlotIndex());
            return kind == FrameSlotKind.Double || kind == FrameSlotKind.Illegal;
        }
    }

    private static final class FrameArgumentWriteNode extends FrameStackWriteNode {
        private final int index;

        private FrameArgumentWriteNode(final int index) {
            this.index = FrameAccess.getArgumentStartIndex() + index;
        }

        @Override
        public void executeWrite(final Frame frame, final Object value) {
            frame.getArguments()[index] = value;
        }
    }
}
