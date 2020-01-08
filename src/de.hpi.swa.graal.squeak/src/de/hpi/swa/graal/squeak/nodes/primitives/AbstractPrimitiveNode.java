/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.primitives;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;
import de.hpi.swa.graal.squeak.nodes.context.ArgumentNodes.AbstractArgumentNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.AbstractPrimitive;
import de.hpi.swa.graal.squeak.util.FrameAccess;

@NodeChild(value = "arguments", type = AbstractArgumentNode[].class)
public abstract class AbstractPrimitiveNode extends AbstractNode implements AbstractPrimitive {
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
        return FrameAccess.getContextOrMarker(frame, method);
    }

    protected final LargeIntegerObject asLargeInteger(final long value) {
        // TODO: don't allocate for long operations
        return LargeIntegerObject.valueOf(method.image, value);
    }
}
