package de.hpi.swa.graal.squeak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import de.hpi.swa.graal.squeak.exceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithCode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.JumpBytecodesFactory.ConditionalJumpNodeFactory.HandleConditionResultNodeGen;
import de.hpi.swa.graal.squeak.nodes.bytecodes.SendBytecodes.AbstractSendNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.SendBytecodes.SendSelectorNode;
import de.hpi.swa.graal.squeak.nodes.context.stack.StackPopNode;
import de.hpi.swa.graal.squeak.nodes.context.stack.StackPushNode;

public final class JumpBytecodes {

    public static final class ConditionalJumpNode extends UnconditionalJumpNode {
        public static final int FALSE_SUCCESSOR = 0;
        public static final int TRUE_SUCCESSOR = 1;

        private final Boolean isIfTrue;
        @CompilationFinal(dimensions = 1) private final int[] successorExecutionCount = new int[2];

        @Child private StackPopNode popNode;
        @Child private HandleConditionResultNode handleConditionResultNode;

        public ConditionalJumpNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int bytecode) {
            super(code, index, numBytecodes, bytecode);
            isIfTrue = false;
            popNode = StackPopNode.create(code);
            handleConditionResultNode = HandleConditionResultNode.create(code);
        }

        public ConditionalJumpNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int bytecode, final int parameter, final boolean condition) {
            super(code, index, numBytecodes, bytecode, parameter);
            isIfTrue = condition;
            popNode = StackPopNode.create(code);
            handleConditionResultNode = HandleConditionResultNode.create(code);
        }

        public boolean executeCondition(final VirtualFrame frame) {
            final Object result = popNode.executeRead(frame);
            return handleConditionResultNode.execute(frame, isIfTrue, result);
        }

        @Override
        public int executeInt(final VirtualFrame frame) {
            if (executeCondition(frame)) {
                return getJumpSuccessor();
            } else {
                return getSuccessorIndex();
            }
        }

        /*
         * Inspired by Sulong's LLVMBasicBlockNode (https://goo.gl/AVMg4K).
         */
        @ExplodeLoop
        public double getBranchProbability(final long successorIndex) {
            final double successorBranchProbability;
            long succCount = 0;
            long totalExecutionCount = 0;
            for (int i = 0; i < successorExecutionCount.length; i++) {
                final long v = successorExecutionCount[i];
                if (successorIndex == i) {
                    succCount = v;
                }
                totalExecutionCount += v;
            }
            if (succCount == 0) {
                successorBranchProbability = 0;
            } else {
                assert totalExecutionCount > 0;
                successorBranchProbability = (double) succCount / totalExecutionCount;
            }
            assert !Double.isNaN(successorBranchProbability) && successorBranchProbability >= 0 && successorBranchProbability <= 1;
            return successorBranchProbability;
        }

        public void increaseBranchProbability(final int successorIndex) {
            CompilerAsserts.neverPartOfCompilation();
            successorExecutionCount[successorIndex]++;
        }

        @Override
        protected int longJumpOffset(final int bytecode, final int parameter) {
            return ((bytecode & 3) << 8) + parameter;
        }

        @Override
        public String toString() {
            if (isIfTrue) {
                return "jumpTrue: " + offset;
            } else {
                return "jumpFalse: " + offset;
            }
        }

        protected abstract static class HandleConditionResultNode extends AbstractNodeWithCode {
            @Child private StackPushNode pushNode = StackPushNode.create();
            @Child private AbstractSendNode sendMustBeBooleanNode;

            @CompilationFinal private static NativeObject mustBeBooleanSelector;

            protected static HandleConditionResultNode create(final CompiledCodeObject code) {
                return HandleConditionResultNodeGen.create(code);
            }

            protected HandleConditionResultNode(final CompiledCodeObject code) {
                super(code);
                sendMustBeBooleanNode = new SendSelectorNode(code, -1, 1, getMustBeBooleanSelector(), 0);
            }

            protected abstract boolean execute(VirtualFrame frame, boolean expected, Object result);

            @Specialization
            protected static final boolean doBoolean(final boolean expected, final boolean result) {
                return expected == result;
            }

            @Fallback
            protected final boolean doMustBeBooleanSend(final VirtualFrame frame, @SuppressWarnings("unused") final boolean expected, final Object result) {
                pushNode.executeWrite(frame, result);
                sendMustBeBooleanNode.executeSend(frame);
                CompilerDirectives.transferToInterpreter();
                throw new SqueakException("Should not be reached");
            }

            private NativeObject getMustBeBooleanSelector() {
                if (mustBeBooleanSelector == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    mustBeBooleanSelector = (NativeObject) code.image.specialObjectsArray.at0(SPECIAL_OBJECT_INDEX.SelectorMustBeBoolean);
                }
                return mustBeBooleanSelector;
            }
        }
    }

    public static class UnconditionalJumpNode extends AbstractBytecodeNode {
        protected final int offset;

        public UnconditionalJumpNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int bytecode) {
            super(code, index, numBytecodes);
            this.offset = shortJumpOffset(bytecode);
        }

        public UnconditionalJumpNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int bytecode, final int parameter) {
            super(code, index, numBytecodes);
            this.offset = longJumpOffset(bytecode, parameter);
        }

        @Override
        public int executeInt(final VirtualFrame frame) {
            return getJumpSuccessor();
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            throw new SqueakException("Jumps cannot be executed like other bytecode nodes");
        }

        public int getJumpSuccessor() {
            return getSuccessorIndex() + offset;
        }

        protected int longJumpOffset(final int bytecode, final int parameter) {
            return (((bytecode & 7) - 4) << 8) + parameter;
        }

        protected int shortJumpOffset(final int bytecode) {
            return (bytecode & 7) + 1;
        }

        @Override
        public String toString() {
            return "jumpTo: " + offset;
        }
    }
}
