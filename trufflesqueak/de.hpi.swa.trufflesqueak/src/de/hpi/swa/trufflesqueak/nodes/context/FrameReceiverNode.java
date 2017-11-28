package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;

public class FrameReceiverNode extends SqueakBytecodeNode {
    public FrameReceiverNode(CompiledCodeObject code) {
        super(code, 0);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return receiver(frame);
    }
}
