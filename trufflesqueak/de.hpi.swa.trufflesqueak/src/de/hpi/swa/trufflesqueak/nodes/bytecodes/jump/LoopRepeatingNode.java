package de.hpi.swa.trufflesqueak.nodes.bytecodes.jump;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.RepeatingNode;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.SqueakTypesGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.send.SendSelector;

@NodeInfo(shortName = "loop")
public class LoopRepeatingNode extends Node implements RepeatingNode {
    @Child private SendSelector mustBeBooleanSend;
    @Child private SqueakNode conditionNode;
    @Children private final SqueakNode[] bodyNodes;

    public LoopRepeatingNode(CompiledMethodObject cm, SqueakNode cond, SqueakNode[] body) {
        mustBeBooleanSend = new SendSelector(cm, 0, cm.image.mustBeBoolean, 0);
        conditionNode = cond;
        bodyNodes = body;
    }

    public boolean executeCondition(VirtualFrame frame) {
        boolean jumpOut = true;
        try {
            jumpOut = SqueakTypesGen.expectBoolean(conditionNode.executeGeneric(frame));
        } catch (UnexpectedResultException e) {
            try {
                jumpOut = SqueakTypesGen.expectBoolean(mustBeBooleanSend.executeGeneric(frame));
            } catch (UnexpectedResultException e1) {
            }
        }
        return jumpOut;
    }

    @ExplodeLoop
    public boolean executeRepeating(VirtualFrame frame) {
        if (!executeCondition(frame)) {
            for (SqueakNode node : bodyNodes) {
                node.executeGeneric(frame);
            }
            return true;
        } else {
            return false;
        }
    }
}
