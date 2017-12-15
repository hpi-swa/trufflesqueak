package de.hpi.swa.trufflesqueak.nodes.context.stack;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.nodes.context.FrameStackReadNode;

public class BottomNStackNode extends Node {
    @Children private final FrameStackReadNode[] readNodes;

    public BottomNStackNode(int n) {
        readNodes = new FrameStackReadNode[n];
        for (int i = 0; i < n; i++) {
            readNodes[i] = FrameStackReadNode.create();
        }
    }

    @ExplodeLoop
    public Object[] execute(VirtualFrame frame) {
        Object[] result = new Object[readNodes.length];
        for (int i = 0; i < readNodes.length; i++) {
            result[i] = readNodes[i].execute(frame, i);
        }
        return result;
    }
}
