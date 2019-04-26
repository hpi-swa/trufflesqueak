package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.RootCallTarget;
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

    public abstract Object executeDispatch(CompiledMethodObject method, Object[] receiverAndArguments, Object contextOrMarker);

    @Specialization(guards = {"code.getCallTarget() == cachedTarget"}, //
                    limit = "INLINE_CACHE_SIZE", assumptions = "callTargetStable")
    protected static final Object doDirect(final CompiledMethodObject code, final Object[] receiverAndArguments, final Object contextOrMarker,
                    @SuppressWarnings("unused") @Cached("code.getCallTargetStable()") final Assumption callTargetStable,
                    @SuppressWarnings("unused") @Cached("code.getCallTarget()") final RootCallTarget cachedTarget,
                    @Cached("create(cachedTarget)") final DirectCallNode callNode) {
        return callNode.call(FrameAccess.newWith(code, contextOrMarker, null, receiverAndArguments));
    }

    @Specialization(replaces = "doDirect")
    protected static final Object doIndirect(final CompiledMethodObject method, final Object[] receiverAndArguments, final Object contextOrMarker,
                    @Cached final IndirectCallNode callNode) {
        return callNode.call(method.getCallTarget(), FrameAccess.newWith(method, contextOrMarker, null, receiverAndArguments));
    }
}
