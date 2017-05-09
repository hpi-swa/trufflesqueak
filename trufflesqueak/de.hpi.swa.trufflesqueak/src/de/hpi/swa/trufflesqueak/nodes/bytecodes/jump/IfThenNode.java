package de.hpi.swa.trufflesqueak.nodes.bytecodes.jump;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.SqueakTypesGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.send.SendSelector;

public class IfThenNode extends SqueakNode {
    @Child private SendSelector mustBeBooleanSend;
    private ConditionProfile branchProfile;
    @Child private SqueakNode conditionNode;
    @Children final private SqueakNode[] thenNodes;
    @Child private SqueakNode thenResult;
    @Children final private SqueakNode[] elseNodes;
    @Child private SqueakNode elseResult;

    IfThenNode(CompiledCodeObject cm,
                    SqueakNode condition,
                    SqueakNode[] thenBranch,
                    SqueakNode thenRes,
                    SqueakNode[] elseBranch,
                    SqueakNode elseRes) {
        mustBeBooleanSend = new SendSelector(cm, 0, cm.image.mustBeBoolean, 0);
        branchProfile = ConditionProfile.createCountingProfile();
        conditionNode = condition;
        thenNodes = thenBranch;
        thenResult = thenRes;
        elseNodes = elseBranch;
        elseResult = elseRes;
    }

    @Override
    @ExplodeLoop
    public Object executeGeneric(VirtualFrame frame) {
        try {
            if (branchProfile.profile(SqueakTypesGen.expectBoolean(conditionNode.executeGeneric(frame)))) {
                if (elseNodes != null) {
                    CompilerAsserts.compilationConstant(elseNodes.length);
                    for (SqueakNode node : elseNodes) {
                        node.executeGeneric(frame);
                    }
                }
                if (elseResult != null) {
                    return elseResult.executeGeneric(frame);
                }
            } else {
                if (thenNodes != null) {
                    CompilerAsserts.compilationConstant(thenNodes.length);
                    for (SqueakNode node : thenNodes) {
                        node.executeGeneric(frame);
                    }
                }
                if (thenResult != null) {
                    return thenResult.executeGeneric(frame);
                }
            }
        } catch (UnexpectedResultException e) {
            return mustBeBooleanSend.executeGeneric(frame);
        }
        return null;
    }

    @Override
    public void prettyPrintOn(StringBuilder b) {
        b.append('(');
        conditionNode.prettyPrintOn(b);
        b.append(") ifTrue: [");
        for (SqueakNode node : thenNodes) {
            node.prettyPrintOn(b);
            b.append('.').append('\n');
        }
        if (thenResult != null) {
            thenResult.prettyPrintOn(b);
        }
        b.append("] ifFalse: [");
        if (elseNodes != null) {
            for (SqueakNode node : elseNodes) {
                node.prettyPrintOn(b);
                b.append('.').append('\n');
            }
        }
        if (elseResult != null) {
            elseResult.prettyPrintOn(b);
        }
        b.append(']');
    }
}
