package de.hpi.swa.trufflesqueak.nodes.bytecodes.jump;

import java.util.Arrays;
import java.util.Stack;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.RepeatingNode;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import de.hpi.swa.trufflesqueak.instrumentation.PrettyPrintVisitor;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.SqueakNodeWithMethod;
import de.hpi.swa.trufflesqueak.nodes.SqueakTypesGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.send.SendSelector;

@NodeInfo(shortName = "loop")
public class LoopRepeatingNode extends SqueakNodeWithMethod implements RepeatingNode {
    @Child private SqueakNode mustBeBooleanSend;
    @Children private final SqueakNode[] conditionBodyNodes;
    @Child private SqueakNode conditionNode;
    @Children private final SqueakNode[] bodyNodes;

    public LoopRepeatingNode(CompiledCodeObject cm, SqueakNode[] conditionBody, SqueakNode cond, SqueakNode[] body) {
        super(cm);
        mustBeBooleanSend = new SendSelector(cm, 0, cm.image.mustBeBoolean, 0);
        conditionBodyNodes = conditionBody;
        conditionNode = cond;
        bodyNodes = body;
    }

    @ExplodeLoop
    public boolean executeCondition(VirtualFrame frame) {
        for (SqueakNode node : conditionBodyNodes) {
            node.executeGeneric(frame);
        }
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

    @Override
    public void prettyPrintOn(PrettyPrintVisitor str) {
        str.append('[').newline().indent();
        Arrays.stream(conditionBodyNodes).forEach(n -> str.visitStatement(n));
        str.visitStatement(conditionNode);
        str.dedent();
        if (conditionNode instanceof IfTrue) {
            str.append("] whileFalse: [");
        } else {
            str.append("] whileTrue: [");
        }
        str.newline().indent();
        Arrays.stream(bodyNodes).forEach(n -> str.visitStatement(n));
        str.dedent().append("]");
    }

    public static class WhileNode extends SqueakBytecodeNode {
        @Child LoopNode loop;

        public WhileNode(CompiledCodeObject method, int idx, LoopNode node) {
            super(method, idx);
            loop = node;
        }

        @Override
        public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> sequence) {
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            loop.executeLoop(frame);
            return null;
        }
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        throw new RuntimeException("should not run");
    }
}
