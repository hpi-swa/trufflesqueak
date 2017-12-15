package de.hpi.swa.trufflesqueak.nodes.bytecodes.push;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;

public class PushReceiverNode extends SqueakBytecodeNode {

    public PushReceiverNode(CompiledCodeObject code, int index) {
        super(code, index);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return push(frame, receiver(frame));
    }

    @Override
    public String toString() {
        return "self";
    }
}
