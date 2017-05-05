package de.hpi.swa.trufflesqueak.nodes.primitives;

import java.lang.reflect.InvocationTargetException;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

@NodeChildren({@NodeChild(value = "receiver", type = SqueakNode.class)})
public abstract class PrimitiveUnaryOperation extends PrimitiveNode {
    public PrimitiveUnaryOperation(CompiledMethodObject cm) {
        super(cm);
    }

    public static PrimitiveNode createInstance(Class<? extends PrimitiveNode> cls, CompiledMethodObject cm)
                    throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        return (PrimitiveNode) cls.getMethod("create", CompiledMethodObject.class, SqueakNode.class).invoke(cm, arg(0));
    }
}