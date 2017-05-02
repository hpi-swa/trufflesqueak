package de.hpi.swa.trufflesqueak.nodes.bytecodes.jump.conditional;

import java.util.Arrays;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.RepeatingNode;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.SqueakTypesGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.jump.UnconditionalJump;

@NodeInfo(shortName = "loop")
public class LoopNode extends SqueakBytecodeNode {
    @Child private com.oracle.truffle.api.nodes.LoopNode loopNode;

    public static class LoopRepeatingNode extends Node implements RepeatingNode {
        @Children private final SqueakNode[] conditionPrefixNodes;
        @Child private SqueakNode conditionNode;
        @Children private final SqueakNode[] bodyNodes;

        public LoopRepeatingNode(SqueakNode[] cond, SqueakNode[] body) {
            conditionPrefixNodes = Arrays.copyOfRange(cond, 0, cond.length - 1);
            conditionNode = cond[cond.length - 1];
            assert body[body.length - 1] instanceof UnconditionalJump;
            // remove the unconditional backjump at the end
            bodyNodes = Arrays.copyOfRange(body, 0, body.length - 1);
        }

        @ExplodeLoop
        public boolean executeCondition(VirtualFrame frame) {
            for (SqueakNode node : conditionPrefixNodes) {
                node.executeGeneric(frame);
            }
            try {
                return SqueakTypesGen.expectBoolean(conditionNode.executeGeneric(frame));
            } catch (UnexpectedResultException ex) {
                throw new RuntimeException(ex); // TODO: for now, should send boolean
            }
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

    public LoopNode(CompiledMethodObject cm, int idx, SqueakNode[] conditionNodes, SqueakNode[] bodyNodes) {
        super(cm, idx);
        this.loopNode = Truffle.getRuntime().createLoopNode(new LoopRepeatingNode(conditionNodes, bodyNodes));
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        loopNode.executeLoop(frame);
        return null;
    }
}
