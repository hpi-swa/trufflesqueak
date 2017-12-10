package de.hpi.swa.trufflesqueak.nodes.primitives;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

@NodeChildren({@NodeChild(value = "receiver", type = SqueakNode.class),
                @NodeChild(value = "argument", type = SqueakNode.class)})
public abstract class PrimitiveBinaryOperation extends PrimitiveNode {
    public PrimitiveBinaryOperation(CompiledMethodObject code) {
        super(code);
    }

    @Override
    public final Object executeGeneric(VirtualFrame frame) {
        Object[] args = topN(frame, 2);
        return executeGeneric(args[1], args[0]);
    }

    public abstract Object executeGeneric(Object receiver, Object argument);
}