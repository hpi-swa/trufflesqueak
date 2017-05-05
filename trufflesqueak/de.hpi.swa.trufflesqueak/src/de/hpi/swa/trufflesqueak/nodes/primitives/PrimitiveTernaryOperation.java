package de.hpi.swa.trufflesqueak.nodes.primitives;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

@NodeChildren({@NodeChild(value = "receiver", type = SqueakNode.class),
                @NodeChild(value = "argument1", type = SqueakNode.class),
                @NodeChild(value = "argument2", type = SqueakNode.class)})
public abstract class PrimitiveTernaryOperation extends PrimitiveNode {
    public PrimitiveTernaryOperation(CompiledMethodObject cm) {
        super(cm);
    }
}