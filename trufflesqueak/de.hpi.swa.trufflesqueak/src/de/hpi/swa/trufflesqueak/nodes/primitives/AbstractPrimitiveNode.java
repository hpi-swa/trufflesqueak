package de.hpi.swa.trufflesqueak.nodes.primitives;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNodeWithCode;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

@NodeChild(value = "arguments", type = SqueakNode[].class)
public abstract class AbstractPrimitiveNode extends AbstractNodeWithCode {

    public AbstractPrimitiveNode(CompiledMethodObject method) {
        super(method);
    }

    protected static boolean isNil(Object obj) {
        return obj instanceof NilObject;
    }

    protected static boolean hasVariableClass(BaseSqueakObject obj) {
        return obj.getSqClass().isVariable();
    }

    public Object executeWithArguments(VirtualFrame frame, Object... arguments) {
        return executeWithArgumentsSpecialized(frame, arguments);
    }

    public abstract Object executePrimitive(VirtualFrame frame);

    protected abstract Object executeWithArgumentsSpecialized(VirtualFrame frame, Object... arguments);

    protected static final boolean isSemaphore(PointersObject receiver) {
        return receiver.isSpecialKindAt(SPECIAL_OBJECT_INDEX.ClassSemaphore);
    }
}
