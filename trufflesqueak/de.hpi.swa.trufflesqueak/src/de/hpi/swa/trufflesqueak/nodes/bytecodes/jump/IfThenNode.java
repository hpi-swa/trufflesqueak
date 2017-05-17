package de.hpi.swa.trufflesqueak.nodes.bytecodes.jump;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import de.hpi.swa.trufflesqueak.instrumentation.PrettyPrintVisitor;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.SqueakNodeWithMethod;
import de.hpi.swa.trufflesqueak.nodes.SqueakTypesGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.send.SendSelector;

public class IfThenNode extends SqueakNodeWithMethod {
    @Child private SqueakNode mustBeBooleanSend;
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

    private static void prettyPrintBranchOn(PrettyPrintVisitor b, String selector, SqueakNode[] branch, SqueakNode result) {
        if (branch == null && result == null)
            return;
        b.append(selector).append(" [").newline().indent();
        if (branch != null)
            Arrays.stream(branch).forEach(n -> b.visitStatement(n));
        if (result != null)
            b.visitStatement(result);
        b.dedent().append(']');
    }

    @Override
    public void prettyPrintOn(PrettyPrintVisitor b) {
        b.visitWithParens(conditionNode);
        b.newline().indent();
        if (conditionNode instanceof IfTrue) {
            prettyPrintBranchOn(b, "ifFalse:", thenNodes, thenResult);
            prettyPrintBranchOn(b, "ifTrue:", elseNodes, elseResult);
        } else {
            prettyPrintBranchOn(b, "ifTrue:", thenNodes, thenResult);
            prettyPrintBranchOn(b, "ifFalse:", elseNodes, elseResult);
        }
        b.dedent();
    }
}
