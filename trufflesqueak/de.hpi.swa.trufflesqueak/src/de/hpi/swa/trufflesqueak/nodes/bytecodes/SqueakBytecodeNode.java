package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.Stack;
import java.util.Vector;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.SqueakNodeWithMethod;

public abstract class SqueakBytecodeNode extends SqueakNodeWithMethod {
    protected final int index;

    public SqueakBytecodeNode(CompiledCodeObject method, int idx) {
        super(method);
        index = idx;
    }

    abstract public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> sequence);

    @SuppressWarnings("unused")
    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> statements, Vector<SqueakBytecodeNode> sequence) {
        interpretOn(stack, statements);
    }

    public boolean isReturn() {
        return false;
    }
}
