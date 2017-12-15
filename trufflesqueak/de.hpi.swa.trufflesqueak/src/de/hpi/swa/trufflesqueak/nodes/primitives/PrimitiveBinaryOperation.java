package de.hpi.swa.trufflesqueak.nodes.primitives;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.BottomNStackNode;

@NodeChildren({@NodeChild(value = "receiver", type = SqueakNode.class),
                @NodeChild(value = "argument", type = SqueakNode.class)})
public class PrimitiveBinaryOperation extends PrimitiveNode {
    @Child BottomNStackNode bottomNNode = new BottomNStackNode(2);

    public PrimitiveBinaryOperation(CompiledMethodObject code) {
        super(code);
    }
}