package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import de.hpi.swa.trufflesqueak.model.BlockClosure;
import de.hpi.swa.trufflesqueak.model.CompiledBlockObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

public class PushClosureNode extends SqueakBytecodeNode {
    private final int blockSize;
    private final int numArgs;
    private final int numCopied;
    @Child SqueakNode receiverNode;
    @Children final SqueakNode[] copiedValueNodes;
    public final CompiledBlockObject compiledBlock;

    public PushClosureNode(CompiledCodeObject code, int index, int i, int j, int k) {
        super(code, index);
        numArgs = i & 0xF;
        numCopied = (i >> 4) & 0xF;
        blockSize = (j << 8) | k;
        copiedValueNodes = new SqueakNode[numCopied];
        receiverNode = new PushReceiverNode(code, -1);
        compiledBlock = new CompiledBlockObject(code, numArgs, numCopied);
    }

    @Override
    @ExplodeLoop
    public Object executeGeneric(VirtualFrame frame) {
        Object frameMarker = FrameUtil.getObjectSafe(frame, code.markerSlot);
        Object receiver = receiverNode.executeGeneric(frame);
        Object[] copiedValues = new Object[copiedValueNodes.length];
        for (int i = 0; i < copiedValueNodes.length; i++) {
            copiedValues[i] = copiedValueNodes[i].executeGeneric(frame);
        }
        return new BlockClosure(frameMarker, compiledBlock, receiver, copiedValues);
    }
}
