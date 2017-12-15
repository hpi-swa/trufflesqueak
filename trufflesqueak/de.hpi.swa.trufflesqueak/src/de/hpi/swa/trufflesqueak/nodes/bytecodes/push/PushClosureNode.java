package de.hpi.swa.trufflesqueak.nodes.bytecodes.push;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.BlockClosure;
import de.hpi.swa.trufflesqueak.model.CompiledBlockObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.context.ReceiverNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PopNReversedStackNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PushStackNode;

public class PushClosureNode extends SqueakBytecodeNode {
    @CompilationFinal private final int blockSize;
    @CompilationFinal private final int numArgs;
    @CompilationFinal private final int numCopied;
    @CompilationFinal private final CompiledBlockObject compiledBlock;
    @Child private PopNReversedStackNode popNReversedNode;
    @Child private PushStackNode pushNode;
    @Child ReceiverNode receiverNode = new ReceiverNode();

    public PushClosureNode(CompiledCodeObject code, int index, int numBytecodes, int i, int j, int k) {
        super(code, index, numBytecodes);
        this.numArgs = i & 0xF;
        this.numCopied = (i >> 4) & 0xF;
        this.blockSize = (j << 8) | k;
        this.compiledBlock = new CompiledBlockObject(code, numArgs, numCopied);
        pushNode = new PushStackNode(code);
        popNReversedNode = new PopNReversedStackNode(code, numCopied);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        Object frameMarker = FrameUtil.getObjectSafe(frame, code.markerSlot);
        Object[] copiedValues = popNReversedNode.execute(frame);
        int codeStart = successorIndex;
        int codeEnd = codeStart + blockSize;
        byte[] bytes = Arrays.copyOfRange(code.getBytecodeNode().getBytes(), codeStart, codeEnd);
        compiledBlock.initializeWithBytes(bytes);
        return pushNode.executeWrite(frame, new BlockClosure(frameMarker, compiledBlock, receiverNode.execute(frame), copiedValues));
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
