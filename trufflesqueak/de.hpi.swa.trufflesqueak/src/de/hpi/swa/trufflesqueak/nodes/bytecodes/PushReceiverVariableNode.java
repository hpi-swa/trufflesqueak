package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtNode;

public class PushReceiverVariableNode extends SqueakBytecodeNode {
    @Child ObjectAtNode fetchNode;

    public PushReceiverVariableNode(CompiledCodeObject code, int idx, int i) {
        super(code, idx);
        fetchNode = ObjectAtNode.create(i);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return push(frame, fetchNode.executeWith(receiver(frame)));
    }
}
