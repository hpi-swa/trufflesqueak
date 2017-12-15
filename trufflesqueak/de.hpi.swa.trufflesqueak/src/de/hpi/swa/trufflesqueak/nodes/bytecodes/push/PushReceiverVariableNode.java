package de.hpi.swa.trufflesqueak.nodes.bytecodes.push;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtNode;
import de.hpi.swa.trufflesqueak.nodes.context.ReceiverNode;

public class PushReceiverVariableNode extends SqueakBytecodeNode {
    @Child ObjectAtNode fetchNode;
    @Child ReceiverNode receiverNode = new ReceiverNode();
    @CompilationFinal private final int variableIndex;

    public PushReceiverVariableNode(CompiledCodeObject code, int index, int numBytecodes, int varIndex) {
        super(code, index, numBytecodes);
        variableIndex = varIndex;
        fetchNode = ObjectAtNode.create(varIndex);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return push(frame, fetchNode.executeWith(receiverNode.execute(frame)));
    }

    @Override
    public String toString() {
        return "pushRcvr: " + variableIndex;
    }
}
