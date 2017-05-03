package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.Stack;
import java.util.Vector;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNodeWithMethod;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

public abstract class SqueakBytecodeNode extends SqueakNodeWithMethod {
    protected final int index;

    public SqueakBytecodeNode(CompiledMethodObject cm, int idx) {
        super(cm);
        index = idx;
    }

    abstract public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> sequence);

    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> statements, Vector<SqueakBytecodeNode> sequence) {
        interpretOn(stack, statements);
    }
}
