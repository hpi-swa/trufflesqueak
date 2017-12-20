package de.hpi.swa.trufflesqueak.nodes.bytecodes.push;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameReceiverNode;

public class PushReceiverVariableNode extends AbstractPushNode {
    @Child private ObjectAtNode fetchNode;
    @Child private FrameReceiverNode receiverNode;
    @CompilationFinal private final int variableIndex;

    public PushReceiverVariableNode(CompiledCodeObject code, int index, int numBytecodes, int varIndex) {
        super(code, index, numBytecodes);
        variableIndex = varIndex;
        fetchNode = ObjectAtNode.create(varIndex, new FrameReceiverNode(code));
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        pushNode.executeWrite(frame, fetchNode.executeGeneric(frame));
    }

    @Override
    public String toString() {
        return "pushRcvr: " + variableIndex;
    }
}
