package de.hpi.swa.trufflesqueak.nodes.primitives;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

@NodeChildren({@NodeChild(value = "receiver", type = SqueakNode.class), @NodeChild(value = "arg1", type = SqueakNode.class),
                @NodeChild(value = "arg2", type = SqueakNode.class), @NodeChild(value = "arg3", type = SqueakNode.class),
                @NodeChild(value = "arg4", type = SqueakNode.class)})
public abstract class PrimitiveQuinaryOperation extends PrimitiveNode {
    public PrimitiveQuinaryOperation(CompiledMethodObject cm) {
        super(cm);
    }

    @Override
    public final Object executeGeneric(VirtualFrame frame) {
        Object[] args = popN(frame, 5);
        return executeGeneric(args[4], args[3], args[2], args[1], args[0]);
    }

    public abstract Object executeGeneric(Object receiver, Object arg1, Object arg2, Object arg3, Object arg4);
}
