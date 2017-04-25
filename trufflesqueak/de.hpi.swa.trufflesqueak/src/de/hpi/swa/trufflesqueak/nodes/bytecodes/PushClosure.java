package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

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
    public Object executeGeneric(VirtualFrame frame) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int stepBytecode(VirtualFrame frame) {
        executeGeneric(frame);
        return getIndex() + blockSize + 1;
    }
}
