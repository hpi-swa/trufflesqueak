/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.primitives;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;
import de.hpi.swa.graal.squeak.nodes.context.ArgumentNodes.AbstractArgumentNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.AbstractPrimitive;
import de.hpi.swa.graal.squeak.util.FrameAccess;

@NodeChild(value = "arguments", type = AbstractArgumentNode[].class)
public abstract class AbstractPrimitiveNode extends AbstractNode implements AbstractPrimitive {
    protected final CompiledMethodObject method;
    @CompilationFinal private ConditionProfile hasContextProfile;
    @CompilationFinal private ConditionProfile hasMarkerProfile;

    public AbstractPrimitiveNode(final CompiledMethodObject method) {
        this.method = method;
    }

    public AbstractPrimitiveNode(final AbstractPrimitiveNode original) {
        this(original.method);
    }

    public abstract Object executeWithArguments(VirtualFrame frame, Object... arguments);

    public abstract Object executePrimitive(VirtualFrame frame);

    protected final Object getContextOrMarker(final VirtualFrame frame) {
        if (hasContextProfile == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            hasContextProfile = ConditionProfile.createBinaryProfile();
            hasMarkerProfile = ConditionProfile.createBinaryProfile();
        }
        return FrameAccess.getContextOrMarker(frame, method, hasContextProfile, hasMarkerProfile);
    }
}
