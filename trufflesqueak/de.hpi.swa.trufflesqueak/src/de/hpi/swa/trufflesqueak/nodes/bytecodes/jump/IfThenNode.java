package de.hpi.swa.trufflesqueak.nodes.bytecodes.jump;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.SqueakNodeWithCode;
import de.hpi.swa.trufflesqueak.nodes.SqueakTypesGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.send.SendSelector;

public class IfThenNode extends SqueakNodeWithCode {
    @Child private SqueakNode mustBeBooleanSend;
    @Child public SqueakNode conditionNode;
    @Children public final SqueakNode[] thenNodes;
    @Child public SqueakNode thenResult;
    @Children public final SqueakNode[] elseNodes;
    @Child public SqueakNode elseResult;

    IfThenNode(CompiledCodeObject cm,
                    SqueakNode condition,
                    SqueakNode[] thenBranch,
                    SqueakNode thenRes,
                    SqueakNode[] elseBranch,
                    SqueakNode elseRes) {
        super(cm);
        mustBeBooleanSend = new SendSelector(cm, 0, cm.image.mustBeBoolean, 0);
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
            if (SqueakTypesGen.expectBoolean(conditionNode.executeGeneric(frame))) {
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
}
