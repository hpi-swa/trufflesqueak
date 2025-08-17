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
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DenyReplace;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
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
            return FrameSlotReadNodeGen.create(stackSlotIndex, shouldClear);
        } else {
            return new FrameAuxiliarySlotReadNode(frame, stackSlotIndex, shouldClear);
        }
    }

    @NeverDefault
    public static final FrameStackReadNode createTemporaryReadNode(final VirtualFrame frame, final int index) {
        final int numArgs = FrameAccess.getNumArguments(frame);
        if (index < numArgs) {
            return FrameArgumentReadNode.getOrCreate(index);
        } else {
            final int stackSlotIndex = FrameAccess.toStackSlotIndex(frame, index);
            final int numberOfSlots = frame.getFrameDescriptor().getNumberOfSlots();
            if (stackSlotIndex < numberOfSlots) {
                return FrameSlotReadNodeGen.create(stackSlotIndex, false);
            } else {
                return new FrameAuxiliarySlotReadNode(frame, stackSlotIndex, false);
            }
        }
    }

    public final Object executeRead(final VirtualFrame frame) {
        final Object value = executeReadUnsafe(frame);
        assert value != null : "Unexpected `null` value";
        return value;
    }

    /* Unsafe as it may return `null` values. */
    public abstract Object executeReadUnsafe(VirtualFrame frame);

    public abstract long executeReadLong(final VirtualFrame frame) throws UnexpectedResultException;

    protected abstract static class FrameSlotReadNode extends FrameStackReadNode {
        protected final byte slotIndex;
        protected final boolean clearSlotAfterRead;

        FrameSlotReadNode(final int slotIndex, final boolean clearSlotAfterRead) {
            this.slotIndex = (byte) slotIndex;
            assert this.slotIndex == slotIndex;
            this.clearSlotAfterRead = clearSlotAfterRead;
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

        @Specialization(replaces = {"readBoolean", "readDouble"})
        protected final Object readObject(final VirtualFrame frame) {
            final Object value;
            if (!frame.isObject(slotIndex)) {
                /*
                 * The FrameSlotKind has been set to Object, so from now on all writes to the slot
                 * will be Object writes. However, now we are in a frame that still has an old
                 * non-Object value. This is a slow-path operation: we read the non-Object value,
                 * and clear it immediately as an Object value so that we do not hit this path again
                 * multiple times for the same slot of the same frame.
                 */
                CompilerDirectives.transferToInterpreter();
                value = frame.getValue(slotIndex);
                if (!clearSlotAfterRead) {
                    frame.setObject(slotIndex, value);
                }
            } else {
                value = frame.getObject(slotIndex);
            }
            if (clearSlotAfterRead) {
                frame.setObject(slotIndex, null);
            }
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
        public long executeReadLong(final VirtualFrame frame) throws UnexpectedResultException {
            final Object value = frame.getArguments()[index];
            if (value instanceof final Long l) {
                return l;
            } else {
                throw new UnexpectedResultException(value);
            }
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

    private static class FrameAuxiliarySlotReadNode extends FrameStackReadNode {
        private final short auxiliarySlotIndex;
        private final boolean clearSlotAfterRead;

        FrameAuxiliarySlotReadNode(final VirtualFrame frame, final int slotIndex, final boolean clearSlotAfterRead) {
            auxiliarySlotIndex = (short) frame.getFrameDescriptor().findOrAddAuxiliarySlot(slotIndex);
            this.clearSlotAfterRead = clearSlotAfterRead;
        }

        @Override
        public Object executeReadUnsafe(final VirtualFrame frame) {
            final Object value = frame.getAuxiliarySlot(auxiliarySlotIndex);
            if (clearSlotAfterRead) {
                frame.setAuxiliarySlot(auxiliarySlotIndex, null);
            }
            return value;
        }

        @Override
        public long executeReadLong(final VirtualFrame frame) throws UnexpectedResultException {
            final Object value = frame.getAuxiliarySlot(auxiliarySlotIndex);
            if (clearSlotAfterRead) {
                frame.setAuxiliarySlot(auxiliarySlotIndex, null);
            }
            if (value instanceof final Long l) {
                return l;
            } else {
                throw new UnexpectedResultException(value);
            }
        }
    }
}
