/*
 * Copyright (c) 2017-2024 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2024 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

/** Uneagerly version of {@link DispatchEagerlyNode} but with uncached version. */
@GenerateInline
@GenerateUncached
@GenerateCached(false)
public abstract class DispatchUneagerlyNode extends AbstractNode {
    protected static final int INLINE_CACHE_SIZE = 3;

    public static final Object executeUncached(final CompiledCodeObject method, final Object[] receiverAndArguments, final Object contextOrMarker) {
        return DispatchUneagerlyNodeGen.getUncached().executeDispatch(null, method, receiverAndArguments, contextOrMarker);
    }

    public abstract Object executeDispatch(Node node, CompiledCodeObject method, Object[] receiverAndArguments, Object contextOrMarker);

    @Specialization(guards = {"method == cachedMethod"}, //
                    limit = "INLINE_CACHE_SIZE", assumptions = "cachedMethod.getCallTargetStable()")
    protected static final Object doDirect(@SuppressWarnings("unused") final CompiledCodeObject method, final Object[] receiverAndArguments, final Object contextOrMarker,
                    @SuppressWarnings("unused") @Cached("method") final CompiledCodeObject cachedMethod,
                    @Cached("create(cachedMethod.getCallTarget())") final DirectCallNode callNode) {
        return callNode.call(FrameAccess.newWith(contextOrMarker, null, receiverAndArguments));
    }

    @ReportPolymorphism.Megamorphic
    @Specialization(replaces = "doDirect")
    protected static final Object doIndirect(final CompiledCodeObject method, final Object[] receiverAndArguments, final Object contextOrMarker,
                    @Cached(inline = false) final IndirectCallNode callNode) {
        return callNode.call(method.getCallTarget(), FrameAccess.newWith(contextOrMarker, null, receiverAndArguments));
    }
}
