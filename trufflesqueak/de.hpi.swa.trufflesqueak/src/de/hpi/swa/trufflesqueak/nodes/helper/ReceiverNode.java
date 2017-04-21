package de.hpi.swa.trufflesqueak.nodes.helper;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakExecutionNode;

public class ReceiverNode extends SqueakExecutionNode {
    public ReceiverNode(CompiledMethodObject cm) {
        super(cm);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return getReceiver(frame);
    }
}
