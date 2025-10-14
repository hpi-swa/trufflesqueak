/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.profiles.CountingConditionProfile;

import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodes.AbstractSendNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackPopNode;
import de.hpi.swa.trufflesqueak.nodes.interrupts.CheckForInterruptsQuickNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.BlockClosurePrimitives.AbstractClosurePrimitiveNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class JumpBytecodes {

    public abstract static class ConditionalJumpNode extends AbstractBytecodeNode {
        private final int jumpSuccessorIndex;
        private final CountingConditionProfile conditionProfile = CountingConditionProfile.create();

        @Child private FrameStackPopNode popNode = FrameStackPopNode.create();

        @SuppressWarnings("this-escape")
        protected ConditionalJumpNode(final int successorIndex, final int offset) {
            super(successorIndex);
            jumpSuccessorIndex = getSuccessorIndex() + offset;
            assert offset > 0 : "Jump offset is expected to be positive for conditional jump bytecodes (Squeak compiler does not produce conditional back jumps)";
        }

        @Override
        public final void executeVoid(final VirtualFrame frame) {
            throw CompilerDirectives.shouldNotReachHere();
        }

        public final boolean executeCondition(final VirtualFrame frame) {
            final Object result = popNode.execute(frame);
            if (result instanceof Boolean r) {
                return conditionProfile.profile(check(r));
            } else {
                CompilerDirectives.transferToInterpreter();
                FrameAccess.setInstructionPointer(frame, getSuccessorIndex());
                final SqueakImageContext image = getContext();
                image.mustBeBooleanSelector.executeAsSymbolSlow(image, frame, result);
                throw SqueakException.create("Should not be reached");
            }
        }

        protected abstract boolean check(boolean value);

        public final int getJumpSuccessorIndex() {
            return jumpSuccessorIndex;
        }
    }

    public static final class ConditionalJumpOnFalseNode extends ConditionalJumpNode {
        private ConditionalJumpOnFalseNode(final int successorIndex, final int offset) {
            super(successorIndex, offset);
        }

        public static ConditionalJumpOnFalseNode createShort(final int successorIndex, final int bytecode) {
            return new ConditionalJumpOnFalseNode(successorIndex, calculateShortOffset(bytecode));
        }

        public static ConditionalJumpOnFalseNode createLong(final int successorIndex, final int bytecode, final byte parameter) {
            return new ConditionalJumpOnFalseNode(successorIndex, calculateLongOffset(bytecode, parameter));
        }

        public static ConditionalJumpOnFalseNode createLongExtended(final int successorIndex, final byte bytecode, final int extB) {
            return new ConditionalJumpOnFalseNode(successorIndex, calculateLongExtendedOffset(bytecode, extB));
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "jumpFalse: " + getJumpSuccessorIndex();
        }

        @Override
        protected boolean check(final boolean value) {
            return !value;
        }
    }

    public static final class ConditionalJumpOnTrueNode extends ConditionalJumpNode {
        private ConditionalJumpOnTrueNode(final int successorIndex, final int offset) {
            super(successorIndex, offset);
        }

        public static ConditionalJumpOnTrueNode createShort(final int successorIndex, final int bytecode) {
            return new ConditionalJumpOnTrueNode(successorIndex, calculateShortOffset(bytecode));
        }

        public static ConditionalJumpOnTrueNode createLong(final int successorIndex, final int bytecode, final byte parameter) {
            return new ConditionalJumpOnTrueNode(successorIndex, calculateLongOffset(bytecode, parameter));
        }

        public static ConditionalJumpOnTrueNode createLongExtended(final int successorIndex, final byte bytecode, final int extB) {
            return new ConditionalJumpOnTrueNode(successorIndex, calculateLongExtendedOffset(bytecode, extB));
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "jumpTrue: " + getJumpSuccessorIndex();
        }

        @Override
        protected boolean check(final boolean value) {
            return value;
        }
    }

    public abstract static class AbstractUnconditionalJumpNode extends AbstractBytecodeNode {
        private AbstractUnconditionalJumpNode(final int successorIndex, final int offset) {
            super(successorIndex + offset);
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "jumpTo: " + getSuccessorIndex();
        }
    }

    public static final class UnconditionalForwardJumpWithoutCheckNode extends AbstractUnconditionalJumpNode {
        private UnconditionalForwardJumpWithoutCheckNode(final int successorIndex, final int offset) {
            super(successorIndex, offset);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            // nothing to do
        }
    }

    public abstract static class AbstractUnconditionalBackJumpNode extends AbstractUnconditionalJumpNode {
        protected AbstractUnconditionalBackJumpNode(final int successorIndex, final int offset) {
            super(successorIndex, offset);
        }

        public abstract void executeCheck(VirtualFrame frame);

        @Override
        public void executeVoid(final VirtualFrame frame) {
            throw CompilerDirectives.shouldNotReachHere();
        }
    }

    public static final class UnconditionalBackJumpWithoutCheckNode extends AbstractUnconditionalBackJumpNode {
        private UnconditionalBackJumpWithoutCheckNode(final int successorIndex, final int offset) {
            super(successorIndex, offset);
        }

        @Override
        public void executeCheck(final VirtualFrame frame) {
            // nothing to do
        }
    }

    public static final class UnconditionalBackJumpWithCheckNode extends AbstractUnconditionalBackJumpNode {
        @Child private CheckForInterruptsQuickNode interruptHandlerNode = CheckForInterruptsQuickNode.createForLoop();

        private UnconditionalBackJumpWithCheckNode(final int successorIndex, final int offset) {
            super(successorIndex, offset);
            assert offset < 0;
        }

        @Override
        public void executeCheck(final VirtualFrame frame) {
            try {
                interruptHandlerNode.execute(frame);
            } catch (ProcessSwitch ps) {
                // persist PC
                FrameAccess.setInstructionPointer(frame, getSuccessorIndex());
                throw ps;
            }
        }
    }

    public static AbstractUnconditionalJumpNode createUnconditionalShortJump(final AbstractBytecodeNode[] bytecodeNodes, final int index, final int bytecode) {
        return create(bytecodeNodes, index, 1, calculateShortOffset(bytecode));
    }

    public static AbstractUnconditionalJumpNode createUnconditionalLongJump(final AbstractBytecodeNode[] bytecodeNodes, final int index, final int bytecode, final byte parameter) {
        final int offset = ((bytecode & 7) - 4 << 8) + Byte.toUnsignedInt(parameter);
        return create(bytecodeNodes, index, 2, offset);
    }

    public static AbstractUnconditionalJumpNode createUnconditionalLongExtendedJump(final AbstractBytecodeNode[] bytecodeNodes, final int index, final int numBytecodes, final byte bytecode,
                    final int extB) {
        return create(bytecodeNodes, index, numBytecodes, calculateLongExtendedOffset(bytecode, extB));
    }

    private static AbstractUnconditionalJumpNode create(final AbstractBytecodeNode[] bytecodeNodes, final int index, final int numBytecodes, final int offset) {
        final int successorIndex = index + numBytecodes;
        if (offset < 0) {
            if (needsCheck(bytecodeNodes, index, numBytecodes, offset)) {
                return new UnconditionalBackJumpWithCheckNode(successorIndex, offset);
            } else {
                return new UnconditionalBackJumpWithoutCheckNode(successorIndex, offset);
            }
        } else {
            return new UnconditionalForwardJumpWithoutCheckNode(successorIndex, offset);
        }
    }

    private static boolean needsCheck(final AbstractBytecodeNode[] bytecodeNodes, final int index, final int numBytecodes, final int offset) {
        CompilerAsserts.neverPartOfCompilation();
        if (SqueakImageContext.getSlow().interruptHandlerDisabled()) {
            return false;
        }
        assert offset < 0 : "back jumps only";
        final int backJumpIndex = index + numBytecodes + offset;
        for (int i = backJumpIndex; i < index; i++) {
            if (bytecodeNodes[i] instanceof final AbstractSendNode abs) {
                // NodeUtil.printTree(System.out, abs);
                /*
                 * Search for call nodes but reject the ones from closure primitives as they do not
                 * check for interrupts.
                 */
                if ((NodeUtil.findFirstNodeInstance(abs, DirectCallNode.class) != null || NodeUtil.findFirstNodeInstance(abs, IndirectCallNode.class) != null) &&
                                NodeUtil.findFirstNodeInstance(abs, AbstractClosurePrimitiveNode.class) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    public static int calculateShortOffset(final int bytecode) {
        return (bytecode & 7) + 1;
    }

    private static int calculateLongOffset(final int bytecode, final byte parameter) {
        return ((bytecode & 3) << 8) + Byte.toUnsignedInt(parameter);
    }

    public static int calculateLongExtendedOffset(final byte bytecode, final int extB) {
        return Byte.toUnsignedInt(bytecode) + (extB << 8);
    }
}
