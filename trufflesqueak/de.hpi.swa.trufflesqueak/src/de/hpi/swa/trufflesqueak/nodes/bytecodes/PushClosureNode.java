package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import de.hpi.swa.trufflesqueak.instrumentation.PrettyPrintVisitor;
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
    private final CompiledBlockObject compiledBlock;

    public PushClosureNode(CompiledCodeObject cm, int idx, int i, int j, int k) {
        super(cm, idx);
        numArgs = i & 0xF;
        numCopied = (i >> 4) & 0xF;
        blockSize = (j << 8) | k;
        copiedValueNodes = new SqueakNode[numCopied];
        receiverNode = new ReceiverNode(cm, idx);
        compiledBlock = new CompiledBlockObject(method, numArgs, numCopied);
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
        return new BlockClosure(frameMarker, compiledBlock, receiver, copiedValues);
    }

    @Override
    public int interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> statements, List<SqueakBytecodeNode> sequence) {
        for (int i = numCopied - 1; i >= 0; i--) {
            copiedValueNodes[i] = stack.pop();
        }
        int codeStart = index + 4; // skip param bytes
        int codeEnd = codeStart + blockSize;
        byte[] bytes = Arrays.copyOfRange(method.getBytes(), codeStart, codeEnd);
        compiledBlock.setBytes(bytes);
        stack.push(this);
        return sequence.indexOf(this) + blockSize + 1;
    }

    @Override
    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> sequence) {
        throw new RuntimeException("should not be called");
    }

    @Override
    public void prettyPrintOn(PrettyPrintVisitor b) {
        b.append('[').newline().indent();
        Arrays.stream(compiledBlock.getBytecodeAST()).forEach(n -> b.visitStatement(n));
        b.dedent().append(']');
    }
}
