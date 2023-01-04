/*
 * Copyright (c) 2017-2022 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2022 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackPopNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class JumpBytecodes {

    public abstract static class ConditionalJumpNode extends AbstractBytecodeNode {
        private final int jumpSuccessorIndex;
        private final ConditionProfile conditionProfile = ConditionProfile.createCountingProfile();

        @Child private FrameStackPopNode popNode = FrameStackPopNode.create();

        protected ConditionalJumpNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int offset) {
            super(code, index, numBytecodes);
            jumpSuccessorIndex = getSuccessorIndex() + offset;
            assert offset > 0 : "Jump offset is expected to be positive for conditional jump bytecodes (Squeak compiler does not produce conditional back jumps)";
        }

        @Override
        public final void executeVoid(final VirtualFrame frame) {
            CompilerDirectives.shouldNotReachHere();
        }

        public final boolean executeCondition(final VirtualFrame frame) {
            final Object result = popNode.execute(frame);
            if (result instanceof Boolean) {
                return conditionProfile.profile(check((boolean) result));
            } else {
                CompilerDirectives.transferToInterpreter();
                FrameAccess.setInstructionPointer(frame, FrameAccess.getCodeObject(frame).getInitialPC() + getSuccessorIndex());
                getContext().mustBeBooleanSelector.executeAsSymbolSlow(frame, result);
                throw SqueakException.create("Should not be reached");
            }
        }

        protected abstract boolean check(boolean value);

        public final int getJumpSuccessorIndex() {
            return jumpSuccessorIndex;
        }
    }

    public static final class ConditionalJumpOnFalseNode extends ConditionalJumpNode {
        private ConditionalJumpOnFalseNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int offset) {
            super(code, index, numBytecodes, offset);
        }

        public static ConditionalJumpOnFalseNode createShort(final CompiledCodeObject code, final int index, final int bytecode) {
            return new ConditionalJumpOnFalseNode(code, index, 1, calculateShortOffset(bytecode));
        }

        public static ConditionalJumpOnFalseNode createLong(final CompiledCodeObject code, final int index, final int bytecode, final byte parameter) {
            return new ConditionalJumpOnFalseNode(code, index, 2, ((bytecode & 3) << 8) + Byte.toUnsignedInt(parameter));
        }

        public static ConditionalJumpOnFalseNode createLongExtended(final CompiledCodeObject code, final int index, final int numBytecodes, final byte bytecode, final int extB) {
            return new ConditionalJumpOnFalseNode(code, index, numBytecodes, Byte.toUnsignedInt(bytecode) + (extB << 8));
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
        private ConditionalJumpOnTrueNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int offset) {
            super(code, index, numBytecodes, offset);
        }

        public static ConditionalJumpOnTrueNode createShort(final CompiledCodeObject code, final int index, final int bytecode) {
            return new ConditionalJumpOnTrueNode(code, index, 1, calculateShortOffset(bytecode));
        }

        public static ConditionalJumpOnTrueNode createLong(final CompiledCodeObject code, final int index, final int bytecode, final byte parameter) {
            return new ConditionalJumpOnTrueNode(code, index, 2, ((bytecode & 3) << 8) + Byte.toUnsignedInt(parameter));
        }

        public static ConditionalJumpOnTrueNode createLongExtended(final CompiledCodeObject code, final int index, final int numBytecodes, final byte bytecode, final int extB) {
            return new ConditionalJumpOnTrueNode(code, index, numBytecodes, Byte.toUnsignedInt(bytecode) + (extB << 8));
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

    public static final class UnconditionalJumpNode extends AbstractBytecodeNode {
        private UnconditionalJumpNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int offset) {
            super(code, index, numBytecodes + offset);
        }

        public static UnconditionalJumpNode createShort(final CompiledCodeObject code, final int index, final int bytecode) {
            return new UnconditionalJumpNode(code, index, 1, calculateShortOffset(bytecode));
        }

        public static UnconditionalJumpNode createLong(final CompiledCodeObject code, final int index, final int bytecode, final byte parameter) {
            return new UnconditionalJumpNode(code, index, 2, ((bytecode & 7) - 4 << 8) + Byte.toUnsignedInt(parameter));
        }

        public static UnconditionalJumpNode createLongExtended(final CompiledCodeObject code, final int index, final int numBytecodes, final byte bytecode, final int extB) {
            return new UnconditionalJumpNode(code, index, numBytecodes, Byte.toUnsignedInt(bytecode) + (extB << 8));
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            CompilerDirectives.shouldNotReachHere();
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "jumpTo: " + getSuccessorIndex();
        }
    }

    private static int calculateShortOffset(final int bytecode) {
        return (bytecode & 7) + 1;
    }
}
