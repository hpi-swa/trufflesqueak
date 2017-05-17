package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.Stack;

import com.oracle.truffle.api.instrumentation.StandardTags;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtPutNodeGen;

public class StoreRemoteTempNode extends RemoteTempBytecodeNode {
    private final int indexInArray;
    private final int indexOfArray;

    public StoreRemoteTempNode(CompiledCodeObject cm, int idx, int indexInArray, int indexOfArray) {
        super(cm, idx);
        this.indexInArray = indexInArray;
        this.indexOfArray = indexOfArray;
    }

    @Override
    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> sequence) {
        execNode = ObjectAtPutNodeGen.create(method, indexInArray, getTempArray(method, indexOfArray), stack.pop());
        stack.push(this);
    }

    @Override
    protected boolean isTaggedWith(Class<?> tag) {
        if (tag == StandardTags.StatementTag.class) {
            return getSourceSection().isAvailable();
        }
        return false;
    }
}
