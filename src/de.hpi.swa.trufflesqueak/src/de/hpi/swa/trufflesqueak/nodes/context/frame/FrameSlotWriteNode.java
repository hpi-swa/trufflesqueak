/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.context.frame;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameSlotWriteNodeFactory.FrameSlotWriteImplNodeGen;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public abstract class FrameSlotWriteNode extends AbstractFrameSlotNode {
    public static FrameSlotWriteNode create(final FrameSlot slot) {
        return FrameSlotWriteImplNodeGen.create(slot);
    }

    public static FrameSlotWriteNode create(final CompiledCodeObject code, final int index) {
        final int numArgs = code.getNumArgs();
        if (index < numArgs) {
            return new FrameArgumentWriteNode(index);
        } else {
            return FrameSlotWriteImplNodeGen.create(code.getStackSlot(index));
        }
    }

    public static FrameSlotWriteNode create(final Frame frame, final int index) {
        final int numArgs = FrameAccess.getNumArguments(frame);
        if (index < numArgs) {
            return new FrameArgumentWriteNode(index);
        } else {
            return FrameSlotWriteImplNodeGen.create(FrameAccess.getStackSlotSlow(frame, index));
        }
    }

    public abstract void executeWrite(Frame frame, Object value);

    public abstract static class FrameSlotWriteImplNode extends FrameSlotWriteNode {

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

            assert !isNullOrIllegalPrimitive(value) : "Propagation of illegal value detected: " + value;
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

        private static boolean isNullOrIllegalPrimitive(final Object value) {
            /* `null` and all primitive types not globally used/allowed in TruffleSqueak. */
            return value == null || value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Float;
        }

    }

    private static final class FrameArgumentWriteNode extends FrameSlotWriteNode {
        private final int index;

        public FrameArgumentWriteNode(final int index) {
            this.index = FrameAccess.getArgumentStartIndex() + index;
        }

        @Override
        public void executeWrite(final Frame frame, final Object value) {
            frame.getArguments()[index] = value;
        }

        @Override
        protected FrameSlot getSlot() {
            throw CompilerDirectives.shouldNotReachHere();
        }

    }
}
