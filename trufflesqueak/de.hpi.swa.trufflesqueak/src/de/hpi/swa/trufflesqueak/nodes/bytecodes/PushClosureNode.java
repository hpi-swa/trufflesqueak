package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.Stack;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

public class PushClosureNode extends SqueakBytecodeNode {
    private final int blockSize;
    private final int numArgs;
    private final int numCopied;
    @Children private final SqueakNode[] closureArguments;

    public PushClosureNode(CompiledMethodObject compiledMethodObject, int idx, int i, int j, int k) {
        super(compiledMethodObject, idx);
        numArgs = (i >> 4) & 0xF;
        numCopied = i & 0xF;
        blockSize = (j << 8) | k;
        closureArguments = new SqueakNode[numCopied];
    }

    @Override
    @ExplodeLoop
    public Object executeGeneric(VirtualFrame frame) {
        // TODO: implement
        return null;
    }

    @Override
    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> sequence) {
        for (int i = numCopied - 1; i >= 0; i++) {
            closureArguments[i] = stack.pop();
        }
        stack.push(this);
    }
}
