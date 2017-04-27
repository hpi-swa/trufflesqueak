package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.exceptions.UnwrappingError;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;

@NodeChildren({@NodeChild(value = "objectNode"), @NodeChild(value = "valueNode")})
public abstract class ObjectAtPutNode extends ContextAccessNode {
    private final int index;

    protected ObjectAtPutNode(CompiledMethodObject cm, int idx) {
        super(cm);
        index = idx;
    }

    @Specialization
    protected Object write(NativeObject object, int value) {
        object.atput0(index, value);
        return value;
    }

    @Specialization
    protected Object write(BaseSqueakObject object, int value) {
        try {
            object.atput0(index, object.getImage().wrapInt(value));
        } catch (UnwrappingError e) {
            throw new RuntimeException(e);
        }
        return value;
    }

    @Specialization
    protected Object write(BaseSqueakObject object, boolean value) {
        try {
            object.atput0(index, value ? getImage().sqTrue : getImage().sqFalse);
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
