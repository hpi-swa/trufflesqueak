package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

@NodeChildren({@NodeChild(value = "objectNode", type = SqueakNode.class)})
public abstract class ObjectAtNode extends SqueakNode {
    @CompilationFinal private final int index;

    protected ObjectAtNode(int variableIndex) {
        index = variableIndex;
    }

    @Specialization
    protected Object readObject(BaseSqueakObject object) {
        return object.at0(index);
    }

    public abstract Object executeWith(Object value);

    public static ObjectAtNode create(int i) {
        return create(i, null);
    }

    public static ObjectAtNode create(int i, SqueakNode object) {
        return ObjectAtNodeGen.create(i, object);
    }
}
