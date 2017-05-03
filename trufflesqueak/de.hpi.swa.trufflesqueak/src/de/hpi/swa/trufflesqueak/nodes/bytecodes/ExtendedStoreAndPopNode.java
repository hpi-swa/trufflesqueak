package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.Stack;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

public class ExtendedStoreAndPopNode extends ExtendedStoreNode {
    public ExtendedStoreAndPopNode(CompiledMethodObject compiledMethodObject, int idx, int i) {
        super(compiledMethodObject, idx, i);
    }

    @Override
    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> statements) {
        super.interpretOn(stack, statements);
        statements.add(stack.pop());
    }
}
