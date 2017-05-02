package de.hpi.swa.trufflesqueak.nodes.bytecodes.jump.conditional;

import java.util.Vector;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.SqueakTypesGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.Pop;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.jump.AbstractJump;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.jump.UnconditionalJump;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.send.SendSelector;

public class ConditionalJump extends AbstractJump {
    @Child private SendSelector mustBeBooleanSend;
    @Child private SqueakNode branchCondition;
    @Children private final SqueakBytecodeNode[] body;
    private final ConditionProfile branchProfile;
    private final int offset;

    private ConditionalJump(CompiledMethodObject cm, int idx, int off, SqueakNode condition) {
        super(cm, idx);
        mustBeBooleanSend = new SendSelector(cm, idx, cm.getImage().mustBeBoolean, 0);
        branchProfile = ConditionProfile.createCountingProfile();
        offset = off;
        branchCondition = condition;
        body = new SqueakBytecodeNode[offset - 1];
    }

    public ConditionalJump(CompiledMethodObject cm, int idx, int bytecode) {
        this(cm, idx, shortJumpOffset(bytecode), IfFalseNodeGen.create(new Pop(cm, idx)));
    }

    public ConditionalJump(CompiledMethodObject cm, int idx, int bytecode, int parameter, boolean condition) {
        this(cm, idx + 1, longJumpOffset(bytecode, parameter),
                        condition ? IfTrueNodeGen.create(new Pop(cm, idx)) : IfFalseNodeGen.create(new Pop(cm, idx)));
    }

    @Override
    public int getJump() {
        return offset;
    }

    @Override
    public SqueakBytecodeNode decompileFrom(Vector<SqueakBytecodeNode> sequence) {
        assert offset > 0;
        assert offset == body.length;
        int firstBranchBC = getIndex() + 1;
        for (int i = 0; i < body.length; i++) {
            body[i] = sequence.get(firstBranchBC + i);
            sequence.set(firstBranchBC + i, null);
        }
        SqueakBytecodeNode lastNode = body[body.length - 1];

        if (lastNode instanceof UnconditionalJump) {
            // we're the abort jump out of a loop
            int backJumpPC = lastNode.getIndex() + 1 + lastNode.getJump();
            assert backJumpPC < getIndex();
            int conditionEndIndex = getIndex();
            if (sequence.get(getIndex()) == this) {
                conditionEndIndex += 1;
            }
            SqueakNode[] conditionNodes = sequence.subList(backJumpPC, conditionEndIndex).toArray(new SqueakNode[0]);
            assert conditionNodes[conditionNodes.length - 1] == this;
            conditionNodes[conditionNodes.length - 1] = branchCondition;
            for (int i = backJumpPC; i < conditionEndIndex; i++) {
                sequence.set(i, null);
            }
            LoopNode loopNode = new LoopNode(getMethod(), getIndex(), conditionNodes, body);
            return loopNode;
        } else {
            return this;
        }
    }

    @Override
    @ExplodeLoop
    public Object executeGeneric(VirtualFrame frame) {
        CompilerAsserts.compilationConstant(body.length);
        try {
            if (branchProfile.profile(SqueakTypesGen.expectBoolean(branchCondition.executeGeneric(frame)))) {
                return true;
            } else {
                for (SqueakBytecodeNode node : body) {
                    node.executeGeneric(frame);
                }
                return false;
            }
        } catch (UnexpectedResultException e) {
            return mustBeBooleanSend.executeGeneric(frame);
        }
    }
}
