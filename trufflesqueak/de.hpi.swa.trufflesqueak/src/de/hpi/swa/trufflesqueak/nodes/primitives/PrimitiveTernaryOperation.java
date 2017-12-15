package de.hpi.swa.trufflesqueak.nodes.primitives;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.BottomNStackNode;

@NodeChildren({@NodeChild(value = "receiver", type = SqueakNode.class),
                @NodeChild(value = "arg1", type = SqueakNode.class),
                @NodeChild(value = "arg2", type = SqueakNode.class)})
public abstract class PrimitiveTernaryOperation extends PrimitiveNode {
    @Child BottomNStackNode bottomNNode = new BottomNStackNode(3);

    public PrimitiveTernaryOperation(CompiledMethodObject code) {
        super(code);
    }

    @Override
    public final Object executeGeneric(VirtualFrame frame) {
        Object[] args = bottomNNode.execute(frame);
        return executeGeneric(args[0], args[1], args[2]);
    }

    public abstract Object executeGeneric(Object receiver, Object arg1, Object arg2);
}