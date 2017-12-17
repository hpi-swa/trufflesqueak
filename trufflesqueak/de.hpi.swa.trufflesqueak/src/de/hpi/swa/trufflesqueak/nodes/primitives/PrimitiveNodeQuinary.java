package de.hpi.swa.trufflesqueak.nodes.primitives;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.BottomNStackNode;

@NodeChildren({@NodeChild(value = "receiver", type = SqueakNode.class), @NodeChild(value = "arg1", type = SqueakNode.class),
                @NodeChild(value = "arg2", type = SqueakNode.class), @NodeChild(value = "arg3", type = SqueakNode.class),
                @NodeChild(value = "arg4", type = SqueakNode.class)})
public class PrimitiveNodeQuinary extends PrimitiveNode {
    @Child BottomNStackNode bottomNNode;

    public PrimitiveNodeQuinary(CompiledMethodObject code) {
        super(code);
        bottomNNode = new BottomNStackNode(code, 5);
    }
}
