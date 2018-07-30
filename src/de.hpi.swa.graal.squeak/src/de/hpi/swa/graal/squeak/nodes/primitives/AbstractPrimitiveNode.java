package de.hpi.swa.graal.squeak.nodes.primitives;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.GenerateWrapper;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.ProbeNode;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;

import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.EmptyObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NotProvided;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithCode;
import de.hpi.swa.graal.squeak.nodes.SqueakNode;

@GenerateWrapper
@NodeChild(value = "arguments", type = SqueakNode[].class)
public abstract class AbstractPrimitiveNode extends AbstractNodeWithCode implements InstrumentableNode {
    public final int numArguments;

    public AbstractPrimitiveNode(final CompiledMethodObject method, final int numArguments) {
        super(method);
        this.numArguments = numArguments;
    }

    public AbstractPrimitiveNode(final AbstractPrimitiveNode original) {
        super(original.code);
        this.numArguments = original.numArguments;
    }

    public Object executeWithArguments(final VirtualFrame frame, final Object... arguments) {
        return executeWithArgumentsSpecialized(frame, arguments);
    }

    public abstract Object executePrimitive(VirtualFrame frame);

    protected abstract Object executeWithArgumentsSpecialized(VirtualFrame frame, Object... arguments);

    protected static final boolean isSmallInteger(final long value) {
        return LargeIntegerObject.SMALLINTEGER32_MIN <= value && value <= LargeIntegerObject.SMALLINTEGER32_MAX;
    }

    protected static final boolean isNotProvided(final Object obj) {
        return NotProvided.isInstance(obj);
    }

    protected static final boolean isNativeObject(final AbstractSqueakObject object) {
        return object instanceof NativeObject;
    }

    protected static final boolean isLargeInteger(final AbstractSqueakObject object) {
        return object instanceof LargeIntegerObject;
    }

    protected static final boolean isFloat(final AbstractSqueakObject object) {
        return object instanceof FloatObject;
    }

    protected static final boolean isEmptyObject(final AbstractSqueakObject object) {
        return object instanceof EmptyObject;
    }

    protected final LargeIntegerObject asLargeInteger(final long value) {
        return LargeIntegerObject.valueOf(code.image, value);
    }

    protected final FloatObject asFloatObject(final double value) {
        return FloatObject.valueOf(code.image, value);
    }

    @Override
    public final boolean hasTag(final Class<? extends Tag> tag) {
        return tag == StandardTags.StatementTag.class;
    }

    public final boolean isInstrumentable() {
        return true;
    }

    public final WrapperNode createWrapper(final ProbeNode probe) {
        return new AbstractPrimitiveNodeWrapper(this, this, probe);
    }
}
