package de.hpi.swa.trufflesqueak.nodes.bytecodes.jump;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PopNode;

public class ConditionalJump extends AbstractJump {
    private final int alternativeSuccessorIndex;
    @Child PopNode popNode;
    protected final int offset;
    public final Boolean isIfTrue;

    private ConditionalJump(CompiledCodeObject code, int index, int off, boolean condition) {
        super(code, index);
        alternativeSuccessorIndex = index + off;
        offset = off;
        isIfTrue = condition;
        popNode = new PopNode(code, -1);
    }

    public ConditionalJump(CompiledCodeObject code, int index, int bytecode) {
        this(code, index, shortJumpOffset(bytecode), false);
    }

    public ConditionalJump(CompiledCodeObject code, int index, int bytecode, int parameter, boolean condition) {
        this(code, index + 1, longJumpOffset(bytecode, parameter), condition);
    }

    @Override
    public int executeInt(VirtualFrame frame) {
        if (popNode.executeGeneric(frame) == isIfTrue) {
            return alternativeSuccessorIndex;
        } else {
            return successorIndex;
        }
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        throw new RuntimeException("ConditionalJumps cannot step");
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        throw new RuntimeException("ConditionalJumps cannot be executed");
    }
}
