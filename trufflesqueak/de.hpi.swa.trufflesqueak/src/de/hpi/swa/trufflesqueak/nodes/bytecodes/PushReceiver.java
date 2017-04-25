package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public class PushReceiver extends SqueakBytecodeNode {
    public PushReceiver(CompiledMethodObject cm, int idx) {
        super(cm, idx);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        Object receiver = getReceiver(frame);
        push(frame, receiver);
        return receiver;
    }
}
