package de.hpi.swa.trufflesqueak.nodes.bytecodes.jump;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PopStackNode;

public class ConditionalJumpNode extends AbstractJump {
    public static final int FALSE_SUCCESSOR = 0;
    public static final int TRUE_SUCCESSOR = 1;
    @CompilationFinal private final Boolean isIfTrue;
    @Child private PopStackNode popNode;
    @CompilationFinal(dimensions = 1) private final long[] successorExecutionCount;

    private ConditionalJumpNode(CompiledCodeObject code, int index, int numBytecodes, int offset, boolean condition) {
        super(code, index, numBytecodes, offset);
        isIfTrue = condition;
        popNode = new PopStackNode(code);
        successors[1] = index + numBytecodes + offset;
        successorExecutionCount = new long[2];
    }

    public ConditionalJumpNode(CompiledCodeObject code, int index, int numBytecodes, int bytecode) {
        this(code, index, numBytecodes, shortJumpOffset(bytecode), false);
    }

    public ConditionalJumpNode(CompiledCodeObject code, int index, int numBytecodes, int bytecode, int parameter, boolean condition) {
        this(code, index, numBytecodes, longJumpOffset(bytecode, parameter), condition);
    }

    public boolean executeCondition(VirtualFrame frame) {
        return popNode.execute(frame) == isIfTrue;
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

    public void increaseBranchProbability(int successorIndex) {
        CompilerAsserts.neverPartOfCompilation();
        successorExecutionCount[successorIndex]++;
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
