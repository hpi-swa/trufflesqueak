package de.hpi.swa.trufflesqueak.nodes.primitives;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.GenerateWrapper;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.ProbeNode;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.FloatObject;
import de.hpi.swa.trufflesqueak.model.LargeIntegerObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNodeWithCode;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

@GenerateWrapper
@NodeChild(value = "arguments", type = SqueakNode[].class)
public abstract class AbstractPrimitiveNode extends AbstractNodeWithCode implements InstrumentableNode {

    public AbstractPrimitiveNode(CompiledMethodObject method) {
        super(method);
    }

    public AbstractPrimitiveNode(AbstractPrimitiveNode original) {
        super(original.code);
    }

    public Object executeWithArguments(VirtualFrame frame, Object... arguments) {
        return executeWithArgumentsSpecialized(frame, arguments);
    }

    public abstract Object executePrimitive(VirtualFrame frame);

    protected abstract Object executeWithArgumentsSpecialized(VirtualFrame frame, Object... arguments);

    protected static final boolean isSmallInteger(long value) {
        return LargeIntegerObject.SMALLINTEGER32_MIN <= value && value <= LargeIntegerObject.SMALLINTEGER32_MAX;
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

    protected final LargeIntegerObject asLargeInteger(final long value) {
        return LargeIntegerObject.valueOf(code.image, value);
    }

    protected final FloatObject asFloatObject(final double value) {
        return FloatObject.valueOf(code.image, value);
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        return tag == StandardTags.StatementTag.class;
    }

    public boolean isInstrumentable() {
        return true;
    }

    public WrapperNode createWrapper(ProbeNode probe) {
        return new AbstractPrimitiveNodeWrapper(this, this, probe);
    }
}
