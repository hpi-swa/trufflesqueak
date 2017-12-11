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
        super(code, index);
        numArgs = i & 0xF;
        numCopied = (i >> 4) & 0xF;
        blockSize = (j << 8) | k;
        compiledBlock = new CompiledBlockObject(code, numArgs, numCopied);
    }

    @Override
    @ExplodeLoop
    public Object executeGeneric(VirtualFrame frame) {
        Object frameMarker = FrameUtil.getObjectSafe(frame, code.markerSlot);
        Object[] copiedValues = popNReversed(frame, numCopied - 1); // -1 -> do not copy receiver
        Object receiver = pop(frame);
        int codeStart = successorIndex;
        int codeEnd = codeStart + blockSize;
        byte[] bytes = Arrays.copyOfRange(code.getBytecodeNode().getBytes(), codeStart, codeEnd);
        compiledBlock.initializeWithBytes(bytes);
        return push(frame, new BlockClosure(frameMarker, compiledBlock, receiver, copiedValues));
    }
}
