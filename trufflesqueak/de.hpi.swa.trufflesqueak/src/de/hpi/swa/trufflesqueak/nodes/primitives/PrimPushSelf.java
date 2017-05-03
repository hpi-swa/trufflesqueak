package de.hpi.swa.trufflesqueak.nodes.primitives;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotReadNode;

public class PrimPushSelf extends PrimitiveQuickReturnNode {
    @Child SqueakNode receiver;

    public PrimPushSelf(CompiledMethodObject cm) {
        super(cm);
        receiver = FrameSlotReadNode.receiver(cm);
    }

    @Override
    Object getConstant(VirtualFrame frame) {
        return receiver.executeGeneric(frame);
    }
}