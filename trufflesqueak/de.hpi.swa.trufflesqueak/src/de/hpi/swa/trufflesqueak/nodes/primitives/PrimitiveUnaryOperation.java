package de.hpi.swa.trufflesqueak.nodes.primitives;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameReceiverNode;

@NodeChildren({@NodeChild(value = "receiver", type = SqueakNode.class)})
public class PrimitiveUnaryOperation extends PrimitiveNode {
    @Child FrameReceiverNode receiverNode;

    public PrimitiveUnaryOperation(CompiledMethodObject code) {
        super(code);
        receiverNode = new FrameReceiverNode(code);
    }
}