package de.hpi.swa.trufflesqueak.nodes.primitives;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.LargeIntegerObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNodeWithCode;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

@NodeChild(value = "arguments", type = SqueakNode[].class)
public abstract class AbstractPrimitiveNode extends AbstractNodeWithCode {

    public AbstractPrimitiveNode(CompiledMethodObject method) {
        super(method);
    }

    public Object executeWithArguments(VirtualFrame frame, Object... arguments) {
        return executeWithArgumentsSpecialized(frame, arguments);
    }

    public abstract Object executePrimitive(VirtualFrame frame);

    protected abstract Object executeWithArgumentsSpecialized(VirtualFrame frame, Object... arguments);

    protected static final boolean isSmallInteger(long value) {
        return LargeIntegerObject.SMALL_INTEGER_MIN <= value && value <= LargeIntegerObject.SMALL_INTEGER_MAX;
    }

    protected static final boolean hasVariableClass(BaseSqueakObject obj) {
        return obj.getSqClass().isVariable();
    }

    protected static final boolean isNil(Object obj) {
        return obj instanceof NilObject;
    }

    protected static final boolean isSemaphore(PointersObject receiver) {
        return receiver.isSpecialKindAt(SPECIAL_OBJECT_INDEX.ClassSemaphore);
    }
}
