/*
 * Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2023 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import java.util.Objects;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;

public abstract class DispatchSuperSendNode extends AbstractDispatchNode {
    protected final CompiledCodeObject method;

    public DispatchSuperSendNode(final CompiledCodeObject code, final NativeObject selector, final int argumentCount) {
        super(selector, argumentCount);
        method = code.getMethod();
    }

    @NeverDefault
    public static DispatchSuperSendNode create(final CompiledCodeObject code, final NativeObject selector, final int argumentCount) {
        return DispatchSuperSendNodeGen.create(code, selector, argumentCount);
    }

    public abstract Object execute(VirtualFrame frame);

    @Specialization(guards = {"method.getMethodClass(node, readNode) == cachedMethodClass"}, assumptions = {"cachedMethodClass.getClassHierarchyStable()", "dispatchNode.getCallTargetStable()"})
    protected static final Object doCached(final VirtualFrame frame,
                    @SuppressWarnings("unused") @Bind("this") final Node node,
                    @SuppressWarnings("unused") @Cached final AbstractPointersObjectReadNode readNode,
                    @SuppressWarnings("unused") @Cached("getMethodClassSlow(method)") final ClassObject cachedMethodClass,
                    @Cached("create(frame, selector, argumentCount, cachedMethodClass, lookupInSuperClassSlow(cachedMethodClass))") final CachedDispatchNode dispatchNode) {
        return dispatchNode.execute(frame);
    }

    @NeverDefault
    public static final ClassObject getMethodClassSlow(final CompiledCodeObject method) {
        return Objects.requireNonNull(method.getMethodClassSlow());
    }

}
