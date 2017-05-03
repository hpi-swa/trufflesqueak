package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.Stack;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

/**
 * The Dup node always marks a cascade, so when we get one, we'll leave it on the stack for
 * cascading and remember how many statements we had when the cascade started.
 */
public class DupNode extends UnknownBytecodeNode {
    private int statementsIdx;

    public DupNode(CompiledMethodObject cm, int idx) {
        super(cm, idx, -1);
    }

    @Override
    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> sequence) {
        statementsIdx = sequence.size() - 1;
        stack.push(this);
    }

    public int getStatementsIdx() {
        return statementsIdx;
    }
}
