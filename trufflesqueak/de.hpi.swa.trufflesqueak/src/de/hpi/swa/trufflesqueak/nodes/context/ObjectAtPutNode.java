package de.hpi.swa.trufflesqueak.nodes.context;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.instrumentation.PrettyPrintVisitor;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.SqueakNodeWithMethod;

@NodeChildren({@NodeChild(value = "objectNode", type = SqueakNode.class), @NodeChild(value = "valueNode", type = SqueakNode.class)})
public abstract class ObjectAtPutNode extends SqueakNodeWithMethod {
    public final int index;

    public ObjectAtPutNode(ObjectAtPutNode original) {
        super(original.method);
        index = original.index;
    }

    protected ObjectAtPutNode(CompiledCodeObject cm, int variableIndex) {
        super(cm);
        index = variableIndex;
    }

    @Specialization
    protected Object write(NativeObject object, int value) {
        object.setNativeAt0(index, value);
        return value;
    }

    @Specialization
    protected Object write(BaseSqueakObject object, int value) {
        object.atput0(index, value);
        return value;
    }

    @Specialization
    protected Object write(BaseSqueakObject object, long value) {
        object.atput0(index, value);
        return value;
    }

    @Specialization
    protected Object write(BaseSqueakObject object, BigInteger value) {
        object.atput0(index, object.image.wrap(value));
        return value;
    }

    @Specialization
    protected Object write(BaseSqueakObject object, Object value) {
        object.atput0(index, value);
        return value;
    }

    @Override
    public void accept(PrettyPrintVisitor b) {
        b.visit(this);
    }
}
