package de.hpi.swa.graal.squeak.nodes.primitives;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.GenerateWrapper;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.ProbeNode;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;

import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithCode;
import de.hpi.swa.graal.squeak.nodes.SqueakGuards;
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

    public abstract Object executeWithArguments(VirtualFrame frame, Object... arguments);

    public abstract Object executePrimitive(VirtualFrame frame);

    protected final boolean isSmallInteger(final long value) {
        if (code.image.flags.is64bit()) {
            return SqueakGuards.isSmallInteger64bit(value);
        } else {
            return SqueakGuards.isSmallInteger32bit(value);
        }
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
