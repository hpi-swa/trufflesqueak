package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakBytecodeNode;

public class PushReceiverVariable extends SqueakBytecodeNode {
    private int variableIndex;

    public PushReceiverVariable(CompiledMethodObject cm, int idx, int i) {
        super(cm, idx);
        variableIndex = i & 15;
    }

    @Override
    public void executeGeneric(VirtualFrame frame) {
        BaseSqueakObject receiver = getReceiver(frame);
        push(frame, receiver.at0(variableIndex));
    }

}
