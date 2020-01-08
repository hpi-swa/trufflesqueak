/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.util.FrameAccess;

/** Uneagerly version of {@link DispatchEagerlyNode} but with uncached version. */
@GenerateUncached
@NodeInfo(cost = NodeCost.NONE)
public abstract class DispatchUneagerlyNode extends AbstractNode {
    protected static final int INLINE_CACHE_SIZE = 3;

    public static DispatchUneagerlyNode getUncached() {
        return DispatchUneagerlyNodeGen.getUncached();
    }

    public abstract Object executeDispatch(CompiledMethodObject method, Object[] receiverAndArguments, Object contextOrMarker);

    @Specialization(guards = {"method == cachedMethod"}, //
                    limit = "INLINE_CACHE_SIZE", assumptions = "cachedMethod.getCallTargetStable()")
    protected static final Object doDirect(@SuppressWarnings("unused") final CompiledMethodObject method, final Object[] receiverAndArguments, final Object contextOrMarker,
                    @SuppressWarnings("unused") @Cached("method") final CompiledMethodObject cachedMethod,
                    @Cached("create(cachedMethod.getCallTarget())") final DirectCallNode callNode) {
        return callNode.call(FrameAccess.newWith(cachedMethod, contextOrMarker, null, receiverAndArguments));
    }

    @Specialization(replaces = "doDirect")
    protected static final Object doIndirect(final CompiledMethodObject method, final Object[] receiverAndArguments, final Object contextOrMarker,
                    @Cached final IndirectCallNode callNode) {
        return callNode.call(method.getCallTarget(), FrameAccess.newWith(method, contextOrMarker, null, receiverAndArguments));
    }
}
