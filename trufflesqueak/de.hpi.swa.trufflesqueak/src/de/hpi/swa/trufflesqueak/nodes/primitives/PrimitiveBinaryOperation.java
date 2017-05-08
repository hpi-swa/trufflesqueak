package de.hpi.swa.trufflesqueak.nodes.primitives;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.TypeSystemReference;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.SqueakTypes;

@NodeChildren({@NodeChild(value = "receiver", type = SqueakNode.class),
                @NodeChild(value = "argument", type = SqueakNode.class)})
public class PrimitiveBinaryOperation extends PrimitiveNode {
    public PrimitiveBinaryOperation(CompiledMethodObject cm) {
        super(cm);
    }
}