package de.hpi.swa.trufflesqueak.nodes.primitives;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

@NodeChildren({@NodeChild(value = "receiver", type = SqueakNode.class)})
public abstract class PrimitiveUnaryOperation extends PrimitiveNode {
    public PrimitiveUnaryOperation(CompiledMethodObject cm) {
        super(cm);
    }
}