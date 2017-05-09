package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.Stack;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtNodeGen;

public class PushRemoteTempNode extends RemoteTempBytecodeNode {
    public PushRemoteTempNode(CompiledCodeObject cm, int idx, int indexInArray, int indexOfArray) {
        super(cm, idx);
        execNode = ObjectAtNodeGen.create(indexInArray, getTempArray(cm, indexOfArray));
    }

    @Override
    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> sequence) {
        stack.push(this);
    }
}
