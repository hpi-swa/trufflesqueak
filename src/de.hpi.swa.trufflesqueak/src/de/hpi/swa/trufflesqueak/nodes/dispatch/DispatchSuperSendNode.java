/*
 * Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2023 Oracle and/or its affiliates
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

public abstract class DispatchSuperSendNode extends AbstractDispatchNode {
    protected final ClassObject methodClass;

    public DispatchSuperSendNode(final CompiledCodeObject code, final NativeObject selector, final int argumentCount) {
        super(selector, argumentCount);
        /*
         * Assuming method literals can no longer change the moment the method is executed for the
         * first time, cache the method class. Its hierarchy must still be checked for stability.
         */
        methodClass = code.getMethod().getMethodClassSlow();
    }

    public static DispatchSuperSendNode create(final CompiledCodeObject code, final NativeObject selector, final int argumentCount) {
        return DispatchSuperSendNodeGen.create(code, selector, argumentCount);
    }

    public abstract Object execute(VirtualFrame frame);

    @Specialization(assumptions = {"methodClass.getClassHierarchyStable()", "dispatchNode.getCallTargetStable()"})
    protected static final Object doCached(final VirtualFrame frame,
                    @Cached("create(frame, selector, argumentCount, methodClass, lookupInSuperClassSlow(methodClass))") final CachedDispatchNode dispatchNode) {
        return dispatchNode.execute(frame);
    }
}
