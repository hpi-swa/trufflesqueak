/*
 * Copyright (c) 2017-2022 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2022 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.context.frame;

import java.util.Objects;

import org.graalvm.collections.EconomicMap;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackWriteNodeFactory.FrameSlotWriteNodeGen;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public abstract class FrameStackWriteNode extends AbstractNode {
    public static FrameStackWriteNode create(final Frame frame, final int index) {
        final int numArgs = FrameAccess.getNumArguments(frame);
        if (index < numArgs) {
            return FrameArgumentWriteNode.getOrCreate(index);
        } else {
            return FrameSlotWriteNodeGen.create(FrameAccess.toStackSlotIndex(frame, index));
        }
    }

    public abstract void executeWrite(Frame frame, Object value);

    public abstract static class FrameSlotWriteNode extends FrameStackWriteNode {
        private final int slotIndex;

        FrameSlotWriteNode(final int slotIndex) {
            this.slotIndex = slotIndex;
        }

        @Specialization(guards = "isBooleanOrIllegal(frame)")
        protected final void writeBool(final Frame frame, final boolean value) {
            /* Initialize type on first write. No-op if kind is already Boolean. */
            frame.getFrameDescriptor().setSlotKind(slotIndex, FrameSlotKind.Boolean);

            frame.setBoolean(slotIndex, value);
        }

        @Specialization(guards = "isLongOrIllegal(frame)")
        protected final void writeLong(final Frame frame, final long value) {
            /* Initialize type on first write. No-op if kind is already Long. */
            frame.getFrameDescriptor().setSlotKind(slotIndex, FrameSlotKind.Long);

            frame.setLong(slotIndex, value);
        }

        @Specialization(guards = "isDoubleOrIllegal(frame)")
        protected final void writeDouble(final Frame frame, final double value) {
            /* Initialize type on first write. No-op if kind is already Double. */
            frame.getFrameDescriptor().setSlotKind(slotIndex, FrameSlotKind.Double);

            frame.setDouble(slotIndex, value);
        }

        @Specialization(replaces = {"writeBool", "writeLong", "writeDouble"})
        protected final void writeObject(final Frame frame, final Object value) {
            /* Initialize type on first write. No-op if kind is already Object. */
            frame.getFrameDescriptor().setSlotKind(slotIndex, FrameSlotKind.Object);

            frame.setObject(slotIndex, value);
        }

        protected final boolean isBooleanOrIllegal(final Frame frame) {
            final FrameSlotKind kind = frame.getFrameDescriptor().getSlotKind(slotIndex);
            return kind == FrameSlotKind.Boolean || kind == FrameSlotKind.Illegal;
        }

        protected final boolean isLongOrIllegal(final Frame frame) {
            final FrameSlotKind kind = frame.getFrameDescriptor().getSlotKind(slotIndex);
            return kind == FrameSlotKind.Long || kind == FrameSlotKind.Illegal;
        }

        protected final boolean isDoubleOrIllegal(final Frame frame) {
            final FrameSlotKind kind = frame.getFrameDescriptor().getSlotKind(slotIndex);
            return kind == FrameSlotKind.Double || kind == FrameSlotKind.Illegal;
        }
    }

    private static final class FrameArgumentWriteNode extends FrameStackWriteNode {
        private static final EconomicMap<Integer, FrameArgumentWriteNode> SINGLETONS = EconomicMap.create();

        private final int index;

        private FrameArgumentWriteNode(final int index) {
            this.index = FrameAccess.getArgumentStartIndex() + index;
        }

        private static FrameArgumentWriteNode getOrCreate(final int index) {
            CompilerAsserts.neverPartOfCompilation();
            FrameArgumentWriteNode node = SINGLETONS.get(index);
            if (node == null) {
                node = new FrameArgumentWriteNode(index);
                SINGLETONS.put(index, node);
            }
            return node;
        }

        @Override
        public void executeWrite(final Frame frame, final Object value) {
            frame.getArguments()[index] = value;
        }

        @Override
        public boolean isAdoptable() {
            return false;
        }

        @Override
        public Node copy() {
            return Objects.requireNonNull(SINGLETONS.get(index));
        }

        @Override
        public Node deepCopy() {
            return copy();
        }
    }
}
