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
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackReadNodeFactory.FrameSlotReadClearNodeGen;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackReadNodeFactory.FrameSlotReadNoClearNodeGen;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

@ImportStatic(FrameSlotKind.class)
public abstract class FrameStackReadNode extends AbstractNode {

    public static final FrameStackReadNode create(final Frame frame, final int index, final boolean clear) {
        final int numArgs = FrameAccess.getNumArguments(frame);
        if (index < numArgs) {
            return FrameArgumentReadNode.getOrCreate(index);
        }
        // Only clear stack values, not receiver, arguments, or temporary variables.
        final int initialSP;
        final BlockClosureObject closure = FrameAccess.getClosure(frame);
        if (closure == null) {
            initialSP = FrameAccess.getCodeObject(frame).getNumTemps();
            assert numArgs == FrameAccess.getCodeObject(frame).getNumArgs();
        } else {
            initialSP = closure.getNumTemps();
            assert numArgs == closure.getNumArgs() + closure.getNumCopied();
        }
        assert initialSP >= numArgs;
        if (clear && index >= initialSP) { // only ever clear non-temp slots
            return FrameSlotReadClearNodeGen.create(FrameAccess.toStackSlotIndex(frame, index));
        } else {
            return FrameSlotReadNoClearNodeGen.create(FrameAccess.toStackSlotIndex(frame, index));
        }
    }

    public static final FrameStackReadNode createTemporaryReadNode(final Frame frame, final int index) {
        final int numArgs = FrameAccess.getNumArguments(frame);
        if (index < numArgs) {
            return FrameArgumentReadNode.getOrCreate(index);
        } else {
            return FrameSlotReadNoClearNodeGen.create(FrameAccess.toStackSlotIndex(frame, index));
        }
    }

    public final Object executeRead(final Frame frame) {
        final Object value = executeReadUnsafe(frame);
        assert value != null : "Unexpected `null` value";
        return value;
    }

    /* Unsafe as it may return `null` values. */
    public abstract Object executeReadUnsafe(Frame frame);

    protected abstract static class AbstractFrameSlotReadNode extends FrameStackReadNode {
        protected final int slotIndex;

        AbstractFrameSlotReadNode(final int slotIndex) {
            this.slotIndex = slotIndex;
        }

        @Specialization(guards = "frame.isBoolean(slotIndex)")
        protected final boolean readBoolean(final Frame frame) {
            return frame.getBoolean(slotIndex);
        }

        @Specialization(guards = "frame.isLong(slotIndex)")
        protected final long readLong(final Frame frame) {
            return frame.getLong(slotIndex);
        }

        @Specialization(guards = "frame.isDouble(slotIndex)")
        protected final double readDouble(final Frame frame) {
            return frame.getDouble(slotIndex);
        }
    }

    protected abstract static class FrameSlotReadNoClearNode extends AbstractFrameSlotReadNode {

        FrameSlotReadNoClearNode(final int slotIndex) {
            super(slotIndex);
        }

        @Specialization(replaces = {"readBoolean", "readLong", "readDouble"})
        protected final Object readObject(final Frame frame) {
            if (!frame.isObject(slotIndex)) {
                /*
                 * The FrameSlotKind has been set to Object, so from now on all writes to the slot
                 * will be Object writes. However, now we are in a frame that still has an old
                 * non-Object value. This is a slow-path operation: we read the non-Object value,
                 * and write it immediately as an Object value so that we do not hit this path again
                 * multiple times for the same slot of the same frame.
                 */
                CompilerDirectives.transferToInterpreter();
                final Object value = frame.getValue(slotIndex);
                assert value != null : "Unexpected `null` value";
                frame.setObject(slotIndex, value);
                return value;
            } else {
                return frame.getObject(slotIndex);
            }
        }
    }

    protected abstract static class FrameSlotReadClearNode extends AbstractFrameSlotReadNode {

        FrameSlotReadClearNode(final int slotIndex) {
            super(slotIndex);
        }

        @Specialization(replaces = {"readBoolean", "readLong", "readDouble"})
        protected final Object readAndClearObject(final Frame frame) {
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
            } else {
                value = frame.getObject(slotIndex);
            }
            frame.setObject(slotIndex, null);
            return value;
        }
    }

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
        public Object executeReadUnsafe(final Frame frame) {
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
}
