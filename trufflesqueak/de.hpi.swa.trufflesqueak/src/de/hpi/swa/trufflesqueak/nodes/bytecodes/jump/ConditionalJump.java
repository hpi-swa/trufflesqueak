package de.hpi.swa.trufflesqueak.nodes.bytecodes.jump;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PopNode;

public class ConditionalJump extends AbstractJump {
    private final int alternativeSuccessorIndex;
    @Child PopNode popNode;
    protected final int offset;
    public final Boolean isIfTrue;

    private ConditionalJump(CompiledCodeObject cm, int idx, int off, boolean condition) {
        super(cm, idx);
        alternativeSuccessorIndex = idx + off;
        offset = off;
        isIfTrue = condition;
        popNode = new PopNode(cm, idx);
    }

    public ConditionalJump(CompiledCodeObject cm, int idx, int bytecode) {
        this(cm, idx, shortJumpOffset(bytecode), false);
    }

    public ConditionalJump(CompiledCodeObject cm, int idx, int bytecode, int parameter, boolean condition) {
        this(cm, idx + 1, longJumpOffset(bytecode, parameter), condition);
    }

    @Override
    public int executeInt(VirtualFrame frame) {
        if (popNode.executeGeneric(frame) == isIfTrue) {
            return alternativeSuccessorIndex;
        } else {
            return successorOffset;
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
