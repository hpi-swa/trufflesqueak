package de.hpi.swa.trufflesqueak.nodes.context;

import java.math.BigInteger;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.WriteNode;

@NodeChildren({@NodeChild(value = "objectNode", type = SqueakNode.class), @NodeChild(value = "valueNode", type = SqueakNode.class)})
public abstract class ObjectAtPutNode extends SqueakNode implements WriteNode {
    @CompilationFinal private final int index;

    protected ObjectAtPutNode(int variableIndex) {
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

    public static WriteNode create(int idx, SqueakNode node) {
        return ObjectAtPutNodeGen.create(idx, node, null);
    }

    public static ObjectAtPutNode create(int idx) {
        return ObjectAtPutNodeGen.create(idx, null, null);
    }

    public final void executeWrite(VirtualFrame frame, Object value) {
        executeWrite(getObjectNode().executeGeneric(frame), value);
    }

    protected abstract SqueakNode getObjectNode();

    public abstract void executeWrite(Object target, Object value);
}
