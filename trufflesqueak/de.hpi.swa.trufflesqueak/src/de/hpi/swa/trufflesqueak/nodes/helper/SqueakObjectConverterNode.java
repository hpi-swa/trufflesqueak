package de.hpi.swa.trufflesqueak.nodes.helper;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.PrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.SqueakExecutionNode;

@NodeChildren({@NodeChild(value = "value", type = PrimitiveNode.class)})
public abstract class SqueakObjectConverterNode extends SqueakExecutionNode {
    public SqueakObjectConverterNode(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization
    public Object convert(BaseSqueakObject value) {
        return value;
    }
}
