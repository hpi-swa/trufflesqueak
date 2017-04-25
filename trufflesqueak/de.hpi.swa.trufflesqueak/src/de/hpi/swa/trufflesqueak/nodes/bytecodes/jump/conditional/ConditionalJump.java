package de.hpi.swa.trufflesqueak.nodes.bytecodes.jump.conditional;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.trufflesqueak.exceptions.LocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.SqueakTypesGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.Pop;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.jump.AbstractJump;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.send.SendSelector;

public class ConditionalJump extends AbstractJump {
    @Child private SendSelector mustBeBooleanSend;
    @Child private SqueakNode branchCondition;
    private final ConditionProfile branchProfile;
    private final int offset;

    private ConditionalJump(CompiledMethodObject cm, int idx, int off, SqueakNode condition) {
        super(cm, idx);
        mustBeBooleanSend = new SendSelector(cm, idx, cm.getImage().mustBeBoolean, 0);
        branchProfile = ConditionProfile.createCountingProfile();
        offset = off;
        branchCondition = condition;
    }

    public ConditionalJump(CompiledMethodObject cm, int idx, int bytecode) {
        this(cm, idx, shortJumpOffset(bytecode), IfFalseNodeGen.create(new Pop(cm, idx)));
    }

    public ConditionalJump(CompiledMethodObject cm, int idx, int bytecode, int parameter, boolean condition) {
        this(cm, idx, longJumpOffset(bytecode, parameter),
                        condition ? IfTrueNodeGen.create(new Pop(cm, idx)) : IfFalseNodeGen.create(new Pop(cm, idx)));
    }

    @Override
    public int stepBytecode(VirtualFrame frame) throws NonLocalReturn, NonVirtualReturn, LocalReturn, ProcessSwitch {
        try {
            if (branchProfile.profile(SqueakTypesGen.expectBoolean(branchCondition.executeGeneric(frame)))) {
                return getIndex() + 1 + offset;
            } else {
                return getIndex() + 1;
            }
        } catch (UnexpectedResultException e) {
            return mustBeBooleanSend.stepBytecode(frame);
        }
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        throw new RuntimeException("unexpected executeGeneric on ConditionalJump");
    }
}
