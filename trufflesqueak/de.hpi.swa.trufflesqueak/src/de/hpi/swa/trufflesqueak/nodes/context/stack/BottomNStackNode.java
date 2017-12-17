package de.hpi.swa.trufflesqueak.nodes.context.stack;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameSlotReadNode;

public class BottomNStackNode extends Node {
    @Children private final FrameSlotReadNode[] readNodes;

    public BottomNStackNode(CompiledCodeObject code, int n) {
        readNodes = new FrameSlotReadNode[n];
        for (int i = 0; i < n; i++) {
            readNodes[i] = FrameSlotReadNode.create(code.stackSlots[i]);
        }
    }

    @ExplodeLoop
    public Object[] execute(VirtualFrame frame) {
        Object[] result = new Object[readNodes.length];
        for (int i = 0; i < readNodes.length; i++) {
            result[i] = readNodes[i].executeRead(frame);
        }
        return result;
    }
}
