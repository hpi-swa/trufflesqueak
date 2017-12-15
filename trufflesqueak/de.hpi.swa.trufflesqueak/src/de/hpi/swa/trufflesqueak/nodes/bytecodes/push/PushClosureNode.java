package de.hpi.swa.trufflesqueak.nodes.bytecodes.push;

import java.util.Arrays;

import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.BlockClosure;
import de.hpi.swa.trufflesqueak.model.CompiledBlockObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;

public class PushClosureNode extends SqueakBytecodeNode {
    private final int blockSize;
    private final int numArgs;
    private final int numCopied;
    public final CompiledBlockObject compiledBlock;

    public PushClosureNode(CompiledCodeObject code, int index, int numBytecodes, int i, int j, int k) {
        super(code, index, numBytecodes);
        this.numArgs = i & 0xF;
        this.numCopied = (i >> 4) & 0xF;
        this.blockSize = (j << 8) | k;
        this.compiledBlock = new CompiledBlockObject(code, numArgs, numCopied);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        Object frameMarker = FrameUtil.getObjectSafe(frame, code.markerSlot);
        Object[] copiedValues = popNReversed(frame, numCopied);
        int codeStart = successorIndex;
        int codeEnd = codeStart + blockSize;
        byte[] bytes = Arrays.copyOfRange(code.getBytecodeNode().getBytes(), codeStart, codeEnd);
        compiledBlock.initializeWithBytes(bytes);
        return push(frame, new BlockClosure(frameMarker, compiledBlock, receiver(frame), copiedValues));
    }

    @Override
    public int executeInt(VirtualFrame frame) {
        if (successorIndex < 0) {
            throw new RuntimeException("Inner nodes are not allowed to be executed here");
        }
        executeVoid(frame);
        return successorIndex + blockSize; // jump over block
    }

    @Override
    public String toString() {
        return String.format("closureNumCopied: %d numArgs: %d bytes %d to %d", numCopied, numArgs, successorIndex, successorIndex + blockSize);
    }
}
