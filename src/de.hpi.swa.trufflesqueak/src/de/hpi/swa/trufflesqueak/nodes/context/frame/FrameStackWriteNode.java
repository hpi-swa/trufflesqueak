/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.context.frame;

import java.util.Objects;

import org.graalvm.collections.EconomicMap;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DenyReplace;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeUtil;

import de.hpi.swa.trufflesqueak.model.AbstractSqueakObjectWithClassAndHash;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.AbstractBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackWriteNodeFactory.FrameSlotWriteNodeGen;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public abstract class FrameStackWriteNode extends AbstractNode {
    @NeverDefault
    public static FrameStackWriteNode create(final VirtualFrame frame, final Node node) {
        return create(frame, node, 1);
    }

    @NeverDefault
    public static FrameStackWriteNode create(final VirtualFrame frame, final Node node, final int spOffset) {
        final int sp = NodeUtil.findParent(node, AbstractBytecodeNode.class).getSuccessorStackPointer();
        return create(frame, sp - spOffset);
    }

    @NeverDefault
    public static FrameStackWriteNode create(final VirtualFrame frame, final int index) {
        final int numArgs = FrameAccess.getNumArguments(frame);
        if (index < numArgs) {
            return FrameArgumentWriteNode.getOrCreate(index);
        } else {
            final int stackSlotIndex = FrameAccess.toStackSlotIndex(frame, index);
            final int numberOfSlots = frame.getFrameDescriptor().getNumberOfSlots();
            if (stackSlotIndex < numberOfSlots) {
                return FrameSlotWriteNodeGen.create(stackSlotIndex);
            } else {
                return new FrameAuxiliarySlotWriteNode(frame, stackSlotIndex);
            }
        }
    }

    public final void executeWriteAndSetSP(VirtualFrame frame, Object value, final int sp) {
        executeWrite(frame, value);
        FrameAccess.setStackPointer(frame, sp);
    }

    public abstract void executeWrite(VirtualFrame frame, Object value);

    public abstract static class FrameSlotWriteNode extends FrameStackWriteNode {
        private final int slotIndex;

        FrameSlotWriteNode(final int slotIndex) {
            this.slotIndex = slotIndex;
        }

        @Specialization(guards = "isBooleanOrIllegal(frame)")
        protected final void writeBool(final VirtualFrame frame, final boolean value) {
            frame.setBoolean(slotIndex, value);
        }

        @Specialization(guards = "isLongOrIllegal(frame)")
        protected final void writeLong(final VirtualFrame frame, final long value) {
            frame.setLong(slotIndex, value);
        }

        @Specialization(guards = "isDoubleOrIllegal(frame)")
        protected final void writeDouble(final VirtualFrame frame, final double value) {
            frame.setDouble(slotIndex, value);
        }

        @Specialization(replaces = {"writeBool", "writeLong", "writeDouble"})
        protected final void writeAbstractSqueakObjectWithClassAndHash(final VirtualFrame frame, final AbstractSqueakObjectWithClassAndHash value) {
            /* Initialize type on first write. No-op if kind is already Object. */
            frame.getFrameDescriptor().setSlotKind(slotIndex, FrameSlotKind.Object);

            frame.setObject(slotIndex, value.resolveForwardingPointer());
        }

        @Specialization(replaces = {"writeBool", "writeLong", "writeDouble", "writeAbstractSqueakObjectWithClassAndHash"})
        protected final void writeObject(final VirtualFrame frame, final Object value) {
            /* Initialize type on first write. No-op if kind is already Object. */
            frame.getFrameDescriptor().setSlotKind(slotIndex, FrameSlotKind.Object);

            frame.setObject(slotIndex, AbstractSqueakObjectWithClassAndHash.resolveForwardingPointer(value));
        }

        protected final boolean isBooleanOrIllegal(final VirtualFrame frame) {
            return isKindOrIllegal(frame, FrameSlotKind.Boolean);
        }

        protected final boolean isLongOrIllegal(final VirtualFrame frame) {
            return isKindOrIllegal(frame, FrameSlotKind.Long);
        }

        protected final boolean isDoubleOrIllegal(final VirtualFrame frame) {
            return isKindOrIllegal(frame, FrameSlotKind.Double);
        }

        private boolean isKindOrIllegal(final VirtualFrame frame, final FrameSlotKind expectedKind) {
            final FrameDescriptor frameDescriptor = frame.getFrameDescriptor();
            final FrameSlotKind kind = frameDescriptor.getSlotKind(slotIndex);
            if (kind == FrameSlotKind.Illegal) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                frameDescriptor.setSlotKind(slotIndex, expectedKind);
                return true;
            } else {
                return kind == expectedKind;
            }
        }
    }

    @DenyReplace
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
        public void executeWrite(final VirtualFrame frame, final Object value) {
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

    private static class FrameAuxiliarySlotWriteNode extends FrameStackWriteNode {
        private final int auxiliarySlotIndex;

        FrameAuxiliarySlotWriteNode(final VirtualFrame frame, final int slotIndex) {
            auxiliarySlotIndex = frame.getFrameDescriptor().findOrAddAuxiliarySlot(slotIndex);
        }

        @Override
        public void executeWrite(final VirtualFrame frame, final Object value) {
            frame.setAuxiliarySlot(auxiliarySlotIndex, value);
        }
    }
}
