package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class PushReceiverNode extends SqueakBytecodeNode {

    public PushReceiverNode(CompiledCodeObject code, int index) {
        super(code, index);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return push(frame, receiver(frame));
    }
}
