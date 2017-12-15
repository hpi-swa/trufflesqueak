package de.hpi.swa.trufflesqueak.nodes.bytecodes.push;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtNode;

public class PushReceiverVariableNode extends SqueakBytecodeNode {
    @Child ObjectAtNode fetchNode;
    private final int variableIndex;

    public PushReceiverVariableNode(CompiledCodeObject code, int index, int numBytecodes, int varIndex) {
        super(code, index, numBytecodes);
        variableIndex = varIndex;
        fetchNode = ObjectAtNode.create(varIndex);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return push(frame, fetchNode.executeWith(receiver(frame)));
    }

    @Override
    public String toString() {
        return "pushRcvr: " + variableIndex;
    }
}
