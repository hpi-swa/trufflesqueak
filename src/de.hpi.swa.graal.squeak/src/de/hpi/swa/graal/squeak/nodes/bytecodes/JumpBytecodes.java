package de.hpi.swa.graal.squeak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import de.hpi.swa.graal.squeak.exceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.graal.squeak.nodes.bytecodes.SendBytecodes.SendSelectorNode;
import de.hpi.swa.graal.squeak.nodes.context.stack.StackPopNode;
import de.hpi.swa.graal.squeak.nodes.context.stack.StackPushNode;

public final class JumpBytecodes {

    public static class ConditionalJumpNode extends UnconditionalJumpNode {
        public static final int FALSE_SUCCESSOR = 0;
        public static final int TRUE_SUCCESSOR = 1;
        @CompilationFinal private static NativeObject mustBeBooleanSelector;
        @CompilationFinal private final Boolean isIfTrue;
        @Child private StackPopNode popNode;
        @Child private StackPushNode pushNode;
        @Child private SendSelectorNode sendMustBeBooleanNode;
        @CompilationFinal(dimensions = 1) private final int[] successorExecutionCount = new int[2];

        public ConditionalJumpNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int bytecode) {
            super(code, index, numBytecodes, bytecode);
            isIfTrue = false;
            initializeChildNodes();
        }

        public ConditionalJumpNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int bytecode, final int parameter, final boolean condition) {
            super(code, index, numBytecodes, bytecode, parameter);
            isIfTrue = condition;
            initializeChildNodes();
        }

        private void initializeChildNodes() {
            popNode = StackPopNode.create(code);
            pushNode = StackPushNode.create(code);
            sendMustBeBooleanNode = new SendSelectorNode(code, -1, 1, getMustBeBooleanSelector(), 0);
        }

        public boolean executeCondition(final VirtualFrame frame) {
            final Object value = popNode.executeRead(frame);
            if (value instanceof Boolean) { // TODO: this could be another node
                return value == isIfTrue;
            } else {
                CompilerDirectives.transferToInterpreter();
                pushNode.executeWrite(frame, value);
                sendMustBeBooleanNode.executeSend(frame);
                throw new SqueakException("Should not be reached");
            }
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

        private NativeObject getMustBeBooleanSelector() {
            if (mustBeBooleanSelector == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                mustBeBooleanSelector = (NativeObject) code.image.specialObjectsArray.at0(SPECIAL_OBJECT_INDEX.SelectorMustBeBoolean);
            }
            return mustBeBooleanSelector;
        }
    }

    public static class UnconditionalJumpNode extends AbstractBytecodeNode {
        @CompilationFinal protected final int offset;

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
