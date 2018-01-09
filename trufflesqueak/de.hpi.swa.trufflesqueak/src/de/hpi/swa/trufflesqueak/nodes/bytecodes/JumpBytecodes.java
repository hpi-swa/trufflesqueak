package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PopStackNode;

public final class JumpBytecodes {

    public static class ConditionalJumpNode extends UnconditionalJumpNode {
        public static final int FALSE_SUCCESSOR = 0;
        public static final int TRUE_SUCCESSOR = 1;
        @CompilationFinal private final Boolean isIfTrue;
        @Child private PopStackNode popNode;
        @CompilationFinal(dimensions = 1) private final long[] successorExecutionCount;

        public ConditionalJumpNode(CompiledCodeObject code, int index, int numBytecodes, int bytecode) {
            super(code, index, numBytecodes, bytecode);
            isIfTrue = false;
            popNode = PopStackNode.create(code);
            successorExecutionCount = new long[2];
        }

        public ConditionalJumpNode(CompiledCodeObject code, int index, int numBytecodes, int bytecode, int parameter, boolean condition) {
            super(code, index, numBytecodes, bytecode, parameter);
            isIfTrue = condition;
            popNode = PopStackNode.create(code);
            successorExecutionCount = new long[2];
        }

        public boolean executeCondition(VirtualFrame frame) {
            return popNode.executeGeneric(frame) == isIfTrue;
        }

        @Override
        public int executeInt(VirtualFrame frame) {
            if (executeCondition(frame)) {
                return getJumpSuccessor();
            } else {
                return getNoJumpSuccessor();
            }
        }

        /*
         * Inspired by Sulong's LLVMBasicBlockNode (https://goo.gl/AVMg4K).
         */
        @ExplodeLoop
        public double getBranchProbability(int successorIndex) {
            double successorBranchProbability;
            long succCount = 0;
            long totalExecutionCount = 0;
            for (int i = 0; i < successorExecutionCount.length; i++) {
                long v = successorExecutionCount[i];
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

        public int getNoJumpSuccessor() {
            return index + numBytecodes;
        }

        public void increaseBranchProbability(int successorIndex) {
            CompilerAsserts.neverPartOfCompilation();
            successorExecutionCount[successorIndex]++;
        }

        @Override
        protected int longJumpOffset(int bytecode, int parameter) {
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
    }

    public static class UnconditionalJumpNode extends AbstractBytecodeNode {
        @CompilationFinal protected final int offset;

        public UnconditionalJumpNode(CompiledCodeObject code, int index, int numBytecodes, int bytecode) {
            super(code, index, numBytecodes);
            this.offset = shortJumpOffset(bytecode);
        }

        public UnconditionalJumpNode(CompiledCodeObject code, int index, int numBytecodes, int bytecode, int parameter) {
            super(code, index, numBytecodes);
            this.offset = longJumpOffset(bytecode, parameter);
        }

        @Override
        public int executeInt(VirtualFrame frame) {
            return getJumpSuccessor();
        }

        @Override
        public void executeVoid(VirtualFrame frame) {
            throw new RuntimeException("Jumps cannot be executed like other bytecode nodes");
        }

        public int getJumpSuccessor() {
            return index + numBytecodes + offset;
        }

        protected int longJumpOffset(int bytecode, int parameter) {
            return (((bytecode & 7) - 4) << 8) + parameter;
        }

        protected int shortJumpOffset(int bytecode) {
            return (bytecode & 7) + 1;
        }

        @Override
        public String toString() {
            return "jumpTo: " + offset;
        }
    }
}
