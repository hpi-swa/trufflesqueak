package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.nodes.context.frame.CreateEagerArgumentsNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

@NodeInfo(cost = NodeCost.NONE)
public abstract class DispatchEagerlyNode extends AbstractNode {
    protected static final int INLINE_CACHE_SIZE = 6;

    public static DispatchEagerlyNode create() {
        return DispatchEagerlyNodeGen.create();
    }

    public abstract Object executeDispatch(VirtualFrame frame, CompiledMethodObject method, Object[] receiverAndArguments, Object contextOrMarker);

    @Specialization(guards = {"method.hasPrimitive()", "method == cachedMethod", "primitiveNode != null"}, //
                    limit = "INLINE_CACHE_SIZE", assumptions = {"cachedMethod.getCallTargetStable()"}, rewriteOn = PrimitiveFailed.class)
    protected static final Object doPrimitiveEagerly(final VirtualFrame frame, @SuppressWarnings("unused") final CompiledMethodObject method, final Object[] receiverAndArguments,
                    @SuppressWarnings("unused") final Object contextOrMarker,
                    @SuppressWarnings("unused") @Cached("method") final CompiledMethodObject cachedMethod,
                    @Cached("cachedMethod.image.primitiveNodeFactory.forIndex(cachedMethod, cachedMethod.primitiveIndex())") final AbstractPrimitiveNode primitiveNode,
                    @Cached final CreateEagerArgumentsNode createEagerArgumentsNode) {
        return primitiveNode.executeWithArguments(frame, createEagerArgumentsNode.executeCreate(primitiveNode.getNumArguments(), receiverAndArguments));
    }

    @Specialization(guards = {"method == cachedMethod"}, //
                    limit = "INLINE_CACHE_SIZE", assumptions = "cachedMethod.getCallTargetStable()", replaces = "doPrimitiveEagerly")
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
