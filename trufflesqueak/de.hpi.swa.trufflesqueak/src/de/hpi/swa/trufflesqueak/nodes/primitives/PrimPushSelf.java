package de.hpi.swa.trufflesqueak.nodes.primitives;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.context.ContextAccessNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotReadNode;

public class PrimPushSelf extends PrimitiveQuickReturnNode {
    @Child ContextAccessNode receiver;

    public PrimPushSelf(CompiledMethodObject cm) {
        super(cm);
        receiver = FrameSlotReadNode.receiver(cm);
    }

    @Override
    Object getConstant(VirtualFrame frame) {
        return receiver.executeGeneric(frame);
    }
}