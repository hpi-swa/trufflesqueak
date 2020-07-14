/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.LookupMethodNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;

public abstract class DispatchSuperSendNode extends AbstractDispatchNode {
    protected final CompiledCodeObject method;

    public DispatchSuperSendNode(final CompiledCodeObject code, final NativeObject selector, final int argumentCount) {
        super(selector, argumentCount);
        method = code.getMethod();
    }

    public static DispatchSuperSendNode create(final CompiledCodeObject code, final NativeObject selector, final int argumentCount) {
        return DispatchSuperSendNodeGen.create(code, selector, argumentCount);
    }

    public abstract Object execute(VirtualFrame frame);

    @Specialization(guards = {"method.getMethodClass(readNode) == cachedMethodClass"}, assumptions = {"cachedMethodClass.getClassHierarchyStable()", "dispatchNode.getCallTargetStable()"})
    protected final Object doCached(final VirtualFrame frame,
                    @SuppressWarnings("unused") @Cached final AbstractPointersObjectReadNode readNode,
                    @SuppressWarnings("unused") @Cached("method.getMethodClassSlow()") final ClassObject cachedMethodClass,
                    @Cached("create(frame, argumentCount, cachedMethodClass, lookupSlow(cachedMethodClass.getSuperclassOrNull()))") final CachedDispatchNode dispatchNode) {
        return dispatchNode.execute(frame, selector);
    }

    protected final Object lookupSlow(final ClassObject receiver) {
        assert receiver != null;
        return LookupMethodNode.getUncached().executeLookup(receiver, selector);
    }
}
