package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public class PushClosure extends SqueakBytecodeNode {
    private final int blockSize;
    private final int numArgs;
    private final int numCopied;

    public PushClosure(CompiledMethodObject compiledMethodObject, int idx, int i, int j, int k) {
        super(compiledMethodObject, idx);
        numArgs = (i >> 4) & 0xF;
        numCopied = i & 0xF;
        blockSize = (j << 8) | k;
    }

    @Override
    @ExplodeLoop
    public Object executeGeneric(VirtualFrame frame) {
        CompiledMethodObject outerMethod = getMethod();
        int pcWithBytecodeOffset = outerMethod.getBytecodeOffset() + getIndex() + 1;
        Object[] copiedValues = new Object[numCopied];
        // for (int i = 0; i < numCopied; i++) {
        // copiedValues[i] = pop(frame);
        // }
        return copiedValues[0];
        // TODO: implement
    }

    @Override
    public int stepBytecode(VirtualFrame frame) {
        executeGeneric(frame);
        return getIndex() + blockSize + 1;
    }
}
