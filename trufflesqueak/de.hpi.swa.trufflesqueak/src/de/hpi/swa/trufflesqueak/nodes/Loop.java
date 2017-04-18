package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RepeatingNode;

import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public class Loop extends SqueakBytecodeNode {
    @Child private LoopNode loop;

    public Loop(CompiledMethodObject cm, SqueakBytecodeNode[] condition, SqueakBytecodeNode[] body) {
        super(cm);
        RepeatingNode loopNode = new LoopRepeatingNode(new BytecodeSequence(cm, condition), new BytecodeSequence(cm, body));
        loop = Truffle.getRuntime().createLoopNode(loopNode);
    }

    private static class LoopRepeatingNode extends Node implements RepeatingNode {
        @Child private SqueakBytecodeNode condition;
        @Child private SqueakBytecodeNode body;

        public LoopRepeatingNode(SqueakBytecodeNode conditionNode, SqueakBytecodeNode bodyNode) {
            condition = conditionNode;
            body = bodyNode;
        }

        public boolean executeRepeating(VirtualFrame frame) {
            if (condition.executeGeneric(frame) != null) {
                body.executeGeneric(frame);
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public BaseSqueakObject executeGeneric(VirtualFrame frame) throws NonLocalReturn, NonVirtualReturn, ProcessSwitch {
        loop.executeLoop(frame);
        return null;
    }
}
