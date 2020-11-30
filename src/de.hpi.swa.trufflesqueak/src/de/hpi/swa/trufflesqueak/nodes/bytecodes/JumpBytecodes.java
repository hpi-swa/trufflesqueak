/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
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
        protected final int offset;
        private final ConditionProfile conditionProfile = ConditionProfile.createCountingProfile();

        @Child private FrameStackPopNode popNode = FrameStackPopNode.create();

        protected ConditionalJumpNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int bytecode) {
            super(code, index, numBytecodes);
            offset = (bytecode & 7) + 1;
        }

        protected ConditionalJumpNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int bytecode, final byte parameter) {
            super(code, index, numBytecodes);
            offset = ((bytecode & 3) << 8) + Byte.toUnsignedInt(parameter);
        }

        @Override
        public final void executeVoid(final VirtualFrame frame) {
            // nothing to do
        }

        public final boolean executeCondition(final VirtualFrame frame) {
            final Object result = popNode.execute(frame);
            if (result instanceof Boolean) {
                return conditionProfile.profile(check((boolean) result));
            } else {
                CompilerDirectives.transferToInterpreter();
                FrameAccess.setInstructionPointer(frame, FrameAccess.getInstructionPointerSlot(frame), getSuccessorIndex());
                lookupContext().mustBeBooleanSelector.executeAsSymbolSlow(frame, result);
                throw SqueakException.create("Should not be reached");
            }
        }

        protected abstract boolean check(boolean value);

        public final int getJumpSuccessorIndex() {
            return getSuccessorIndex() + offset;
        }
    }

    public static final class ConditionalFalseJumpNode extends ConditionalJumpNode {
        public ConditionalFalseJumpNode(final CompiledCodeObject code, final int index, final int bytecode) {
            super(code, index, 1, bytecode);
        }

        public ConditionalFalseJumpNode(final CompiledCodeObject code, final int index, final int bytecode, final byte parameter) {
            super(code, index, 2, bytecode, parameter);
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "jumpTrue: " + offset;
        }

        @Override
        protected boolean check(final boolean value) {
            return !value;
        }
    }

    public static final class ConditionalTrueJumpNode extends ConditionalJumpNode {
        public ConditionalTrueJumpNode(final CompiledCodeObject code, final int index, final int bytecode, final byte parameter) {
            super(code, index, 2, bytecode, parameter);
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "jumpTrue: " + offset;
        }

        @Override
        protected boolean check(final boolean value) {
            return value;
        }
    }

    public static final class UnconditionalJumpNode extends AbstractBytecodeNode {
        private final int offset;

        public UnconditionalJumpNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int bytecode) {
            super(code, index, numBytecodes);
            offset = (bytecode & 7) + 1;
        }

        public UnconditionalJumpNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int bytecode, final byte parameter) {
            super(code, index, numBytecodes);
            offset = ((bytecode & 7) - 4 << 8) + Byte.toUnsignedInt(parameter);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            // nothing to do
        }

        public int getJumpSuccessor() {
            return getSuccessorIndex() + offset;
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "jumpTo: " + offset;
        }
    }
}
