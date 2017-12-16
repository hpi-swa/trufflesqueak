package de.hpi.swa.trufflesqueak.nodes.primitives;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.context.ReceiverNode;

@NodeChildren({@NodeChild(value = "receiver", type = SqueakNode.class)})
public class PrimitiveUnaryOperation extends PrimitiveNode {
    @Child ReceiverNode receiverNode;

    public PrimitiveUnaryOperation(CompiledMethodObject code) {
        super(code);
        receiverNode = new ReceiverNode(code);
    }
}