package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.List;
import java.util.Stack;
import java.util.Vector;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.SqueakNodeWithMethod;

public abstract class SqueakBytecodeNode extends SqueakNodeWithMethod {
    protected final int index;

    public SqueakBytecodeNode(CompiledMethodObject cm, int idx) {
        super(cm);
        index = idx;
    }

    protected static SqueakNode[] blockFrom(List<SqueakBytecodeNode> nodes, Vector<SqueakBytecodeNode> sequence, Stack<SqueakNode> subStack) {
        if (nodes == null)
            return null;
        Stack<SqueakNode> subStatements = new Stack<>();
        for (int i = 0; i < nodes.size(); i++) {
            SqueakBytecodeNode node = nodes.get(i);
            if (node != null) {
                if (sequence.contains(node)) {
                    int size = subStatements.size();
                    node.interpretOn(subStack, subStatements, sequence);
                    if (!subStack.empty() && subStatements.size() > size) {
                        // some statement was discovered, but something remains on the stack.
                        // this means the statement didn't consume everything from under itself,
                        // and we need to insert the stack item before the last statement.
                        // FIXME: can these asserts be wrong?
                        assert subStatements.size() == size + 1;
                        assert subStack.size() == 1;
                        subStatements.insertElementAt(subStack.pop(), size);
                    }
                    sequence.set(sequence.indexOf(node), null);
                }
            }
        }
        return subStatements.toArray(new SqueakNode[0]);
    }

    abstract public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> sequence);

    @SuppressWarnings("unused")
    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> statements, Vector<SqueakBytecodeNode> sequence) {
        interpretOn(stack, statements);
    }
}
