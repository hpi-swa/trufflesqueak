/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithCode;
import de.hpi.swa.graal.squeak.nodes.SendSelectorNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.JumpBytecodesFactory.ConditionalJumpNodeFactory.HandleConditionResultNodeGen;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackPopNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class JumpBytecodes {

    public static final class ConditionalJumpNode extends AbstractBytecodeNode {
        private final int offset;
        private final boolean isIfTrue;
        private final ConditionProfile conditionProfile = ConditionProfile.createCountingProfile();

        @Child private FrameStackPopNode popNode;
        @Child private HandleConditionResultNode handleConditionResultNode;

        public ConditionalJumpNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int bytecode) {
            super(code, index, numBytecodes);
            offset = (bytecode & 7) + 1;
            isIfTrue = false;
            popNode = FrameStackPopNode.create(code);
            handleConditionResultNode = HandleConditionResultNode.create(code, getSuccessorIndex());
        }

        public ConditionalJumpNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int bytecode, final int parameter, final boolean condition) {
            super(code, index, numBytecodes);
            offset = ((bytecode & 3) << 8) + parameter;
            isIfTrue = condition;
            popNode = FrameStackPopNode.create(code);
            handleConditionResultNode = HandleConditionResultNode.create(code, getSuccessorIndex());
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

        protected abstract static class HandleConditionResultNode extends AbstractNodeWithCode {
            @Child private SendSelectorNode sendMustBeBooleanNode;
            private final int successorIndex;

            protected HandleConditionResultNode(final CompiledCodeObject code, final int successorIndex) {
                super(code);
                this.successorIndex = successorIndex;
            }

            protected static HandleConditionResultNode create(final CompiledCodeObject code, final int successorIndex) {
                return HandleConditionResultNodeGen.create(code, successorIndex);
            }

            protected abstract boolean execute(VirtualFrame frame, boolean expected, Object result);

            @Specialization
            protected static final boolean doBoolean(final boolean expected, final boolean result) {
                return expected == result;
            }

            @Specialization(guards = "resultLib.isBoolean(result)", limit = "2")
            protected static final boolean doBoolean(final boolean expected, final Object result, @CachedLibrary("result") final InteropLibrary resultLib) {
                try {
                    return expected == resultLib.asBoolean(result);
                } catch (final UnsupportedMessageException e) {
                    throw SqueakException.illegalState(e);
                }
            }

            @Fallback
            protected final boolean doMustBeBooleanSend(final VirtualFrame frame, @SuppressWarnings("unused") final boolean expected, final Object result) {
                FrameAccess.setInstructionPointer(frame, code, successorIndex);
                getSendMustBeBooleanNode().executeSend(frame, result);
                throw SqueakException.create("Should not be reached");
            }

            private SendSelectorNode getSendMustBeBooleanNode() {
                if (sendMustBeBooleanNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    sendMustBeBooleanNode = insert(SendSelectorNode.create(code, code.image.mustBeBooleanSelector));
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
