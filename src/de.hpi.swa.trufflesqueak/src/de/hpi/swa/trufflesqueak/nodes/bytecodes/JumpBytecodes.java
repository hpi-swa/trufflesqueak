/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.SendSelectorNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.JumpBytecodesFactory.ConditionalJumpNodeFactory.HandleConditionResultNodeGen;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackPopNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class JumpBytecodes {

    public static final class ConditionalJumpNode extends AbstractBytecodeNode {
        private final int offset;
        private final boolean isIfTrue;
        private final ConditionProfile conditionProfile = ConditionProfile.createCountingProfile();

        @Child private FrameStackPopNode popNode = FrameStackPopNode.create();
        @Child private HandleConditionResultNode handleConditionResultNode;

        public ConditionalJumpNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int bytecode) {
            super(code, index, numBytecodes);
            offset = (bytecode & 7) + 1;
            isIfTrue = false;
            handleConditionResultNode = HandleConditionResultNode.create(getSuccessorIndex());
        }

        public ConditionalJumpNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int bytecode, final int parameter, final boolean condition) {
            super(code, index, numBytecodes);
            offset = ((bytecode & 3) << 8) + parameter;
            isIfTrue = condition;
            handleConditionResultNode = HandleConditionResultNode.create(getSuccessorIndex());
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            // nothing to do
        }

        public boolean executeCondition(final VirtualFrame frame) {
            final Object result = popNode.execute(frame);
            return conditionProfile.profile(handleConditionResultNode.execute(frame, isIfTrue, result));
        }

        public int getJumpSuccessorIndex() {
            return getSuccessorIndex() + offset;
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            if (isIfTrue) {
                return "jumpTrue: " + offset;
            } else {
                return "jumpFalse: " + offset;
            }
        }

        protected abstract static class HandleConditionResultNode extends AbstractNode {
            @Child private SendSelectorNode sendMustBeBooleanNode;
            private final int successorIndex;

            protected HandleConditionResultNode(final int successorIndex) {
                this.successorIndex = successorIndex;
            }

            protected static HandleConditionResultNode create(final int successorIndex) {
                return HandleConditionResultNodeGen.create(successorIndex);
            }

            protected abstract boolean execute(VirtualFrame frame, boolean expected, Object result);

            @Specialization
            protected static final boolean doBoolean(final boolean expected, final boolean result) {
                return expected == result;
            }

            @Specialization(guards = "!isBoolean(result)")
            protected final boolean doMustBeBooleanSend(final VirtualFrame frame, @SuppressWarnings("unused") final boolean expected, final Object result,
                            @Cached("getInstructionPointerSlot(frame)") final FrameSlot instructionPointerSlot) {
                FrameAccess.setInstructionPointer(frame, instructionPointerSlot, successorIndex);
                getSendMustBeBooleanNode().executeSend(frame, result);
                throw SqueakException.create("Should not be reached");
            }

            private SendSelectorNode getSendMustBeBooleanNode() {
                if (sendMustBeBooleanNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    sendMustBeBooleanNode = insert(SendSelectorNode.create(SqueakLanguage.getContext().mustBeBooleanSelector));
                }
                return sendMustBeBooleanNode;
            }
        }
    }

    public static final class UnconditionalJumpNode extends AbstractBytecodeNode {
        private final int offset;

        public UnconditionalJumpNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int bytecode) {
            super(code, index, numBytecodes);
            offset = (bytecode & 7) + 1;
        }

        public UnconditionalJumpNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int bytecode, final int parameter) {
            super(code, index, numBytecodes);
            offset = ((bytecode & 7) - 4 << 8) + parameter;
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
