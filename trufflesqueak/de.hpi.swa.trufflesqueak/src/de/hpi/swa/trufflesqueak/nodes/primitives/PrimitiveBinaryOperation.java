package de.hpi.swa.trufflesqueak.nodes.primitives;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.context.ContextAccessNode;

@NodeChildren({@NodeChild(value = "receiver", type = ContextAccessNode.class),
                @NodeChild(value = "argument", type = ContextAccessNode.class)})
public class PrimitiveBinaryOperation extends PrimitiveNode {
    public PrimitiveBinaryOperation(CompiledMethodObject cm) {
        super(cm);
    }
}