package de.hpi.swa.trufflesqueak.nodes.primitives;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

@NodeChildren({@NodeChild(value = "receiver", type = SqueakNode.class), @NodeChild(value = "arg1", type = SqueakNode.class),
                @NodeChild(value = "arg2", type = SqueakNode.class), @NodeChild(value = "arg3", type = SqueakNode.class),
                @NodeChild(value = "arg4", type = SqueakNode.class)})
public abstract class PrimitiveQuinaryOperation extends PrimitiveNode {
    public PrimitiveQuinaryOperation(CompiledMethodObject cm) {
        super(cm);
    }
}
