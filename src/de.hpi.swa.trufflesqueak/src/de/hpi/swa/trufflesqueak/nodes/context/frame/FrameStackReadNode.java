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
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DenyReplace;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackReadNodeFactory.FrameSlotReadAndClearNodeGen;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackReadNodeFactory.FrameSlotReadNodeGen;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

@ImportStatic(FrameSlotKind.class)
public abstract class FrameStackReadNode extends AbstractNode {

    @NeverDefault
    public static final FrameStackReadNode create(final VirtualFrame frame, final int index, final boolean clear) {
        final int numArgs = FrameAccess.getNumArguments(frame);
        if (index < numArgs) {
            return FrameArgumentReadNode.getOrCreate(index);
        }
        final int initialSP;
        if (!FrameAccess.hasClosure(frame)) {
            initialSP = FrameAccess.getCodeObject(frame).getNumTemps();
            assert numArgs == FrameAccess.getCodeObject(frame).getNumArgs();
        } else {
            final BlockClosureObject closure = FrameAccess.getClosure(frame);
            initialSP = closure.getNumTemps();
            assert numArgs == closure.getNumArgs() + closure.getNumCopied();
        }
        assert initialSP >= numArgs;
        final int stackSlotIndex = FrameAccess.toStackSlotIndex(frame, index);
        final int numberOfSlots = frame.getFrameDescriptor().getNumberOfSlots();
        // Only clear stack values, not receiver, arguments, or temporary variables.
        final boolean shouldClear = clear && index >= initialSP;
        if (stackSlotIndex < numberOfSlots) {
            return shouldClear ? FrameSlotReadAndClearNodeGen.create(stackSlotIndex) : FrameSlotReadNodeGen.create(stackSlotIndex);
        } else {
            return shouldClear ? new FrameAuxiliarySlotReadAndClearNode(frame, stackSlotIndex) : new FrameAuxiliarySlotReadNode(frame, stackSlotIndex);
        }
    }

    public final Object executeRead(final VirtualFrame frame) {
        final Object value = executeReadUnsafe(frame);
        assert value != null : "Unexpected `null` value";
        return value;
    }

    /* Unsafe as it may return `null` values. */
    public abstract Object executeReadUnsafe(VirtualFrame frame);

    protected abstract static class AbstractFrameSlotReadNode extends FrameStackReadNode {
        protected final int slotIndex;

        AbstractFrameSlotReadNode(final int slotIndex) {
            this.slotIndex = slotIndex;
        }

        @Specialization(guards = "frame.isBoolean(slotIndex)")
        protected final boolean readBoolean(final VirtualFrame frame) {
            return frame.getBoolean(slotIndex);
        }

        @Specialization(guards = "frame.isLong(slotIndex)")
        protected final long readLong(final VirtualFrame frame) {
            return frame.getLong(slotIndex);
        }

        @Specialization(guards = "frame.isDouble(slotIndex)")
        protected final double readDouble(final VirtualFrame frame) {
            return frame.getDouble(slotIndex);
        }
    }

    protected abstract static class FrameSlotReadNode extends AbstractFrameSlotReadNode {
        FrameSlotReadNode(final int slotIndex) {
            super(slotIndex);
        }

        @Specialization(guards = "frame.isObject(slotIndex)")
        protected final Object readObject(final VirtualFrame frame) {
            return frame.getObject(slotIndex);
        }
    }

    protected abstract static class FrameSlotReadAndClearNode extends AbstractFrameSlotReadNode {
        FrameSlotReadAndClearNode(final int slotIndex) {
            super(slotIndex);
        }

        @Specialization(guards = "frame.isObject(slotIndex)")
        protected final Object readObject(final VirtualFrame frame) {
            final Object value = frame.getObject(slotIndex);
            frame.setObject(slotIndex, NilObject.SINGLETON);
            return value;
        }
    }

    @DenyReplace
    private static final class FrameArgumentReadNode extends FrameStackReadNode {
        private static final EconomicMap<Integer, FrameArgumentReadNode> SINGLETONS = EconomicMap.create();

        private final int index;

        private FrameArgumentReadNode(final int index) {
            this.index = FrameAccess.getArgumentStartIndex() + index;
        }

        private static FrameArgumentReadNode getOrCreate(final int index) {
            CompilerAsserts.neverPartOfCompilation();
            FrameArgumentReadNode node = SINGLETONS.get(index);
            if (node == null) {
                node = new FrameArgumentReadNode(index);
                SINGLETONS.put(index, node);
            }
            return node;
        }

        @Override
        public Object executeReadUnsafe(final VirtualFrame frame) {
            return frame.getArguments()[index];
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

    private abstract static class AbstractFrameAuxiliarySlotReadNode extends FrameStackReadNode {
        protected final int auxiliarySlotIndex;

        AbstractFrameAuxiliarySlotReadNode(final VirtualFrame frame, final int slotIndex) {
            auxiliarySlotIndex = frame.getFrameDescriptor().findOrAddAuxiliarySlot(slotIndex);
        }
    }

    private static final class FrameAuxiliarySlotReadNode extends AbstractFrameAuxiliarySlotReadNode {
        FrameAuxiliarySlotReadNode(final VirtualFrame frame, final int slotIndex) {
            super(frame, slotIndex);
        }

        @Override
        public Object executeReadUnsafe(final VirtualFrame frame) {
            return frame.getAuxiliarySlot(auxiliarySlotIndex);
        }
    }

    private static final class FrameAuxiliarySlotReadAndClearNode extends AbstractFrameAuxiliarySlotReadNode {
        FrameAuxiliarySlotReadAndClearNode(final VirtualFrame frame, final int slotIndex) {
            super(frame, slotIndex);
        }

        @Override
        public Object executeReadUnsafe(final VirtualFrame frame) {
            final Object value = frame.getAuxiliarySlot(auxiliarySlotIndex);
            frame.setAuxiliarySlot(auxiliarySlotIndex, NilObject.SINGLETON);
            return value;
        }
    }
}
