package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import de.hpi.swa.trufflesqueak.model.BlockClosure;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

public class PushClosureNode extends SqueakBytecodeNode {
    private final int blockSize;
    private final int numArgs;
    private final int numCopied;
    @Child SqueakNode receiverNode;
    @Children final SqueakNode[] copiedValueNodes;
    @CompilationFinal(dimensions = 1) private SqueakNode[] blockNodes;
    private final FrameDescriptor blockFrameDescriptor;

    public PushClosureNode(CompiledMethodObject cm, int idx, int i, int j, int k) {
        super(cm, idx);
        numArgs = i & 0xF;
        numCopied = (i >> 4) & 0xF;
        blockSize = (j << 8) | k;
        copiedValueNodes = new SqueakNode[numCopied];
        receiverNode = new ReceiverNode(cm, idx);
        blockFrameDescriptor = cm.createFrameDescriptor(numArgs, numCopied, false);
    }

    @Override
    @ExplodeLoop
    public Object executeGeneric(VirtualFrame frame) {
        Object frameMarker = FrameUtil.getObjectSafe(frame, method.markerSlot);
        Object receiver = receiverNode.executeGeneric(frame);
        Object[] copiedValues = new Object[copiedValueNodes.length];
        for (int i = 0; i < copiedValueNodes.length; i++) {
            copiedValues[i] = copiedValueNodes[i].executeGeneric(frame);
        }
        return new BlockClosure(
                        method.image,
                        blockFrameDescriptor,
                        blockNodes,
                        frameMarker,
                        numArgs,
                        receiver,
                        copiedValues);
    }

    private int bytecodeStart() {
        return index + 4;
    }

    @Override
    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> statements, Vector<SqueakBytecodeNode> sequence) {
        for (int i = numCopied - 1; i >= 0; i--) {
            copiedValueNodes[i] = stack.pop();
        }
        int codeStart = bytecodeStart();
        int codeEnd = codeStart + blockSize;
        List<SqueakBytecodeNode> blockCode = sequence.subList(codeStart, codeEnd);
        blockNodes = blockFrom(blockCode, sequence, new Stack<>());
        stack.push(this);
    }

    @Override
    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> sequence) {
        throw new RuntimeException("should not be called");
    }
}
