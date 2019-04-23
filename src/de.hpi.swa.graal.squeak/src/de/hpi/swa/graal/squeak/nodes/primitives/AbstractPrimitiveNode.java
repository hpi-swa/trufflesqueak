package de.hpi.swa.graal.squeak.nodes.primitives;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.GenerateWrapper;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.ProbeNode;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;

import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;
import de.hpi.swa.graal.squeak.nodes.SqueakNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.AbstractPrimitive;
import de.hpi.swa.graal.squeak.util.FrameAccess;

@GenerateWrapper
@NodeChild(value = "arguments", type = SqueakNode[].class)
public abstract class AbstractPrimitiveNode extends AbstractNode implements AbstractPrimitive, InstrumentableNode {
    protected final CompiledMethodObject method;

    public AbstractPrimitiveNode(final CompiledMethodObject method) {
        this.method = method;
    }

    public AbstractPrimitiveNode(final AbstractPrimitiveNode original) {
        this(original.method);
    }

    public abstract Object executeWithArguments(VirtualFrame frame, Object... arguments);

    public abstract Object executePrimitive(VirtualFrame frame);

    protected final Object getContextOrMarker(final VirtualFrame frame) {
        final ContextObject context = FrameAccess.getContext(frame, method);
        return context != null ? context : FrameAccess.getMarker(frame, method);
    }

    protected final LargeIntegerObject asLargeInteger(final long value) {
        // TODO: don't allocate for long operations
        return LargeIntegerObject.valueOf(method.image, value);
    }

    protected final FloatObject asFloatObject(final double value) {
        return FloatObject.valueOf(method.image, value);
    }

    @Override
    public final boolean hasTag(final Class<? extends Tag> tag) {
        return tag == StandardTags.StatementTag.class;
    }

    @Override
    public final boolean isInstrumentable() {
        return true;
    }

    @Override
    public final WrapperNode createWrapper(final ProbeNode probe) {
        return new AbstractPrimitiveNodeWrapper(this, this, probe);
    }
}
