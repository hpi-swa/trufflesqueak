package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.Stack;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

public class StoreAndPopRemoteTempNode extends StoreRemoteTempNode {
    public StoreAndPopRemoteTempNode(CompiledCodeObject compiledMethodObject, int idx, int i, int j) {
        super(compiledMethodObject, idx, i, j);
    }

    @Override
    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> sequence) {
        super.interpretOn(stack, sequence);
        sequence.push(stack.pop());
    }
}
