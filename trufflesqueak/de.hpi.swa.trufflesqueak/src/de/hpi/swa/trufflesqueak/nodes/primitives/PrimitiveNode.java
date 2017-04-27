package de.hpi.swa.trufflesqueak.nodes.primitives;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.LocalReturn;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakExecutionNode;
import de.hpi.swa.trufflesqueak.nodes.context.ContextAccessNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotReadNode;

public class PrimitiveNode extends SqueakExecutionNode {
    public PrimitiveNode(CompiledMethodObject cm) {
        super(cm);
    }

    public static ContextAccessNode arg(CompiledMethodObject cm, int offset) {
        return FrameSlotReadNode.peek(cm, offset);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) throws LocalReturn {
        return null;
    }

    public PrimitiveNode mostSpecializedVersion() {
        return this;
    }
}
