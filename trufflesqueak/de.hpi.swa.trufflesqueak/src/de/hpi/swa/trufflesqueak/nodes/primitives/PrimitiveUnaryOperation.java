package de.hpi.swa.trufflesqueak.nodes.primitives;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.context.ReceiverNode;

@NodeChildren({@NodeChild(value = "receiver", type = SqueakNode.class)})
public abstract class PrimitiveUnaryOperation extends PrimitiveNode {
    @Child ReceiverNode receiverNode = new ReceiverNode();

    public PrimitiveUnaryOperation(CompiledMethodObject code) {
        super(code);
    }

    @Override
    public final Object executeGeneric(VirtualFrame frame) {
        return executeGeneric(receiverNode.execute(frame));
    }

    public abstract Object executeGeneric(Object receiver);
}