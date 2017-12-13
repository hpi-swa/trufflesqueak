package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.Arrays;

import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import de.hpi.swa.trufflesqueak.model.BlockClosure;
import de.hpi.swa.trufflesqueak.model.CompiledBlockObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class PushClosureNode extends SqueakBytecodeNode {
    private final int blockSize;
    private final int numArgs;
    private final int numCopied;
    public final CompiledBlockObject compiledBlock;

    public PushClosureNode(CompiledCodeObject code, int index, int i, int j, int k) {
        this(code, index, i, (j << 8) | k);
    }

    public PushClosureNode(CompiledCodeObject code, int index, int i, int blockSize) {
        super(code, index + blockSize); // jump over block
        this.numArgs = i & 0xF;
        this.numCopied = (i >> 4) & 0xF;
        this.blockSize = blockSize;
        this.compiledBlock = new CompiledBlockObject(code, numArgs, numCopied);
    }

    @Override
    @ExplodeLoop
    public Object executeGeneric(VirtualFrame frame) {
        Object frameMarker = FrameUtil.getObjectSafe(frame, code.markerSlot);
        Object[] copiedValues = popNReversed(frame, numCopied);
        int codeEnd = successorIndex;
        int codeStart = codeEnd - blockSize;
        byte[] bytes = Arrays.copyOfRange(code.getBytecodeNode().getBytes(), codeStart, codeEnd);
        compiledBlock.initializeWithBytes(bytes);
        return push(frame, new BlockClosure(frameMarker, compiledBlock, receiver(frame), copiedValues));
    }
}
