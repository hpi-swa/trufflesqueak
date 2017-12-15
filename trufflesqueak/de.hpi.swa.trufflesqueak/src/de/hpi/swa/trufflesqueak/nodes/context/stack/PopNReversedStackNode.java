package de.hpi.swa.trufflesqueak.nodes.context.stack;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class PopNReversedStackNode extends Node {
    @Children private final PopStackNode[] popNodes;

    public PopNReversedStackNode(CompiledCodeObject code, int n) {
        this.popNodes = new PopStackNode[n];
        for (int i = 0; i < n; i++) {
            this.popNodes[i] = new PopStackNode(code);
        }
    }

    @ExplodeLoop
    public Object[] execute(VirtualFrame frame) {
        Object[] result = new Object[popNodes.length];
        for (int i = 0; i < popNodes.length; i++) {
            result[popNodes.length - i - 1] = popNodes[i].execute(frame);
        }
        return result;
    }
}
