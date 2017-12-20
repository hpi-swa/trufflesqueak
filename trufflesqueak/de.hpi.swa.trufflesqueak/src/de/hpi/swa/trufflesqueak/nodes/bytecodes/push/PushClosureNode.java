package de.hpi.swa.trufflesqueak.nodes.bytecodes.push;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.BlockClosure;
import de.hpi.swa.trufflesqueak.model.CompiledBlockObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameReceiverNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PopNReversedStackNode;

public class PushClosureNode extends AbstractPushNode {
    @CompilationFinal private final int blockSize;
    @CompilationFinal private final int numArgs;
    @CompilationFinal private final int numCopied;
    @CompilationFinal private final CompiledBlockObject compiledBlock;
    @Child private PopNReversedStackNode popNReversedNode;
    @Child private FrameReceiverNode receiverNode = new FrameReceiverNode();

    public PushClosureNode(CompiledCodeObject code, int index, int numBytecodes, int i, int j, int k) {
        super(code, index, numBytecodes);
        this.numArgs = i & 0xF;
        this.numCopied = (i >> 4) & 0xF;
        this.blockSize = (j << 8) | k;
        this.compiledBlock = new CompiledBlockObject(code, numArgs, numCopied);
        popNReversedNode = new PopNReversedStackNode(code, numCopied);
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        Object frameMarker = FrameUtil.getObjectSafe(frame, code.markerSlot);
        Object[] copiedValues = popNReversedNode.execute(frame);
        int codeStart = index + numBytecodes;
        int codeEnd = codeStart + blockSize;
        byte[] bytes = Arrays.copyOfRange(code.getBytes(), codeStart, codeEnd);
        compiledBlock.setBytes(bytes);
        pushNode.executeWrite(frame, new BlockClosure(frameMarker, compiledBlock, receiverNode.executeGeneric(frame), copiedValues));
    }

    @Override
    public int executeInt(VirtualFrame frame) {
        executeVoid(frame);
        return index + numBytecodes + blockSize;
    }

    @Override
    public String toString() {
        return String.format("closureNumCopied: %d numArgs: %d bytes %d to %d", numCopied, numArgs, index + numBytecodes, index + numBytecodes + blockSize);
    }
}
