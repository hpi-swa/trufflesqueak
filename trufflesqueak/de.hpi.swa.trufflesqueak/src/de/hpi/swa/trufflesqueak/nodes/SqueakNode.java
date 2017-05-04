package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.nodes.bytecodes.jump.LoopRepeatingNode;

public abstract class SqueakNode extends Node {
    public abstract Object executeGeneric(VirtualFrame frame);

    public void prettyPrintOn(StringBuilder str) {
        getChildren().forEach(node -> {
            if (node instanceof SqueakNode) {
                ((SqueakNode) node).prettyPrintOn(str);
            } else if (node instanceof LoopNode) {
                ((LoopRepeatingNode) ((LoopNode) node).getRepeatingNode()).prettyPrintOn(str);
            } else {
                str.append(node);
            }
        });
    }

    public String prettyPrint() {
        StringBuilder stringBuilder = new StringBuilder();
        prettyPrintOn(stringBuilder);
        return stringBuilder.toString();
    }
}