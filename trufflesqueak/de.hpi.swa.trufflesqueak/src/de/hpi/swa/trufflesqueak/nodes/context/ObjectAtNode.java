package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

@NodeChildren({@NodeChild(value = "objectNode")})
public abstract class ObjectAtNode extends ContextAccessNode {
    private final int index;

    protected ObjectAtNode(CompiledMethodObject cm, int idx) {
        super(cm);
        index = idx;
    }

    @Specialization
    protected Object read(BaseSqueakObject object) {
        return object.at0(index);
    }
}
