package de.hpi.swa.trufflesqueak.nodes.bytecodes.jump;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.SqueakTypesGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.send.SendSelector;

public class IfThenNode extends Node {
    @Child private SendSelector mustBeBooleanSend;
    private ConditionProfile branchProfile;
    @Child private SqueakNode conditionNode;
    @Children final private SqueakNode[] thenNodes;
    @Children final private SqueakNode[] elseNodes;

    IfThenNode(CompiledMethodObject cm, SqueakNode condition, SqueakNode[] thenBranch, SqueakNode[] elseBranch) {
        mustBeBooleanSend = new SendSelector(cm, 0, cm.image.mustBeBoolean, 0);
        branchProfile = ConditionProfile.createCountingProfile();
        conditionNode = condition;
        thenNodes = thenBranch;
        elseNodes = elseBranch;
    }

    @ExplodeLoop
    public Object executeGeneric(VirtualFrame frame) {
        CompilerAsserts.compilationConstant(thenNodes.length);
        CompilerAsserts.compilationConstant(elseNodes.length);
        try {
            Object last = null;
            if (branchProfile.profile(SqueakTypesGen.expectBoolean(conditionNode.executeGeneric(frame)))) {
                if (elseNodes == null) {
                    return null;
                }
                for (SqueakNode node : elseNodes) {
                    last = node.executeGeneric(frame);
                }
            } else {
                for (SqueakNode node : thenNodes) {
                    last = node.executeGeneric(frame);
                }
            }
            return last;
        } catch (UnexpectedResultException e) {
            return mustBeBooleanSend.executeGeneric(frame);
        }
    }
}
