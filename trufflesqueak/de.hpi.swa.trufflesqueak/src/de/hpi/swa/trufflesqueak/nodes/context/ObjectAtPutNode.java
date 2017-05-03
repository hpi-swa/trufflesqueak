package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.exceptions.UnwrappingError;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakExecutionNode;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

@NodeChildren({@NodeChild(value = "objectNode", type = SqueakNode.class), @NodeChild(value = "valueNode", type = SqueakNode.class)})
public abstract class ObjectAtPutNode extends SqueakExecutionNode {
    private final int index;

    protected ObjectAtPutNode(CompiledMethodObject cm, int variableIndex) {
        super(cm);
        index = variableIndex;
    }

    @Specialization
    protected Object write(NativeObject object, int value) {
        object.atput0(index, value);
        return value;
    }

    @Specialization
    protected Object write(BaseSqueakObject object, int value) {
        try {
            object.atput0(index, object.image.wrapInt(value));
        } catch (UnwrappingError e) {
            throw new RuntimeException(e);
        }
        return value;
    }

    @Specialization
    protected Object write(BaseSqueakObject object, boolean value) {
        try {
            object.atput0(index, value ? method.image.sqTrue : method.image.sqFalse);
        } catch (UnwrappingError e) {
            throw new RuntimeException(e);
        }
        return value;
    }

    @Specialization
    protected Object write(BaseSqueakObject object, BaseSqueakObject value) {
        try {
            object.atput0(index, value);
        } catch (UnwrappingError e) {
            throw new RuntimeException(e);
        }
        return value;
    }
}
