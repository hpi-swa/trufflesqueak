package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.graal.squeak.nodes.context.frame.CreateArgumentsNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.CreateEagerArgumentsNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveNodeFactory;

@ReportPolymorphism
@ImportStatic(PrimitiveNodeFactory.class)
public abstract class DispatchNode extends Node {
    protected static final int INLINE_CACHE_SIZE = 3;

    @Child private CreateArgumentsNode createArgumentsNode;

    public static DispatchNode create() {
        return DispatchNodeGen.create();
    }

    public abstract Object executeDispatch(VirtualFrame frame, Object method, Object[] receiverAndArguments, Object contextOrMarker);

    protected static final boolean isQuickReturnReceiverVariable(final int primitiveIndex) {
        return 264 <= primitiveIndex && primitiveIndex <= 520;
    }

    @Specialization(guards = {"isQuickReturnReceiverVariable(method.primitiveIndex())"})
    protected static final Object doPrimitiveQuickReturnReceiver(final CompiledMethodObject method, final Object[] receiverAndArguments, @SuppressWarnings("unused") final Object contextOrMarker,
                    @Cached("create()") final SqueakObjectAt0Node at0Node) {
        assert receiverAndArguments[0] instanceof AbstractSqueakObject;
        return at0Node.execute(receiverAndArguments[0], method.primitiveIndex() - 264);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isQuickReturnReceiverVariable(method.primitiveIndex())", "method == cachedMethod", "method.hasPrimitive()", "primitiveNode != null"}, //
                    assumptions = {"callTargetStable"}, rewriteOn = {PrimitiveFailed.class}, limit = "1")
    protected static final Object doPrimitiveEagerly(final VirtualFrame frame, final CompiledMethodObject method, final Object[] receiverAndArguments, final Object contextOrMarker,
                    @Cached("method") final CompiledMethodObject cachedMethod,
                    @Cached("method.getCallTargetStable()") final Assumption callTargetStable,
                    @Cached("forIndex(method, method.primitiveIndex())") final AbstractPrimitiveNode primitiveNode,
                    @Cached("create()") final CreateEagerArgumentsNode createEagerArgumentsNode) {
        return primitiveNode.executeWithArguments(frame, createEagerArgumentsNode.executeCreate(primitiveNode.getNumArguments(), receiverAndArguments));
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isQuickReturnReceiverVariable(method.primitiveIndex())", "method == cachedMethod", "method.hasPrimitive()", "primitiveNode != null"}, //
                    assumptions = {"callTargetStable"}, replaces = "doPrimitiveEagerly", limit = "1")
    protected final Object doPrimitiveEagerlyCatch(final VirtualFrame frame, final CompiledMethodObject method, final Object[] receiverAndArguments, final Object contextOrMarker,
                    @Cached("method") final CompiledMethodObject cachedMethod,
                    @Cached("method.getCallTargetStable()") final Assumption callTargetStable,
                    @Cached("forIndex(method, method.primitiveIndex())") final AbstractPrimitiveNode primitiveNode,
                    @Cached("create()") final CreateEagerArgumentsNode createEagerArgumentsNode,
                    @Cached("method.getCallTarget()") final RootCallTarget cachedTarget,
                    @Cached("create(cachedTarget)") final DirectCallNode callNode) {
        try {
            return primitiveNode.executeWithArguments(frame, createEagerArgumentsNode.executeCreate(primitiveNode.getNumArguments(), receiverAndArguments));
        } catch (PrimitiveFailed e) {
            // TODO (low priority): Skip CallPrimitiveNode somehow, not necessary to fail twice.
            return callNode.call(getCreateArgumentsNode().executeCreate(method, contextOrMarker, receiverAndArguments));
        }
    }

    @Specialization(limit = "INLINE_CACHE_SIZE", //
                    guards = {"code.getCallTarget() == cachedTarget", "!isQuickReturnReceiverVariable(code.primitiveIndex())", "!code.hasPrimitive()"}, //
                    assumptions = "callTargetStable")
    protected final Object doDirectWithoutPrimitive(final CompiledCodeObject code, final Object[] receiverAndArguments, final Object contextOrMarker,
                    @SuppressWarnings("unused") @Cached("code.getCallTargetStable()") final Assumption callTargetStable,
                    @SuppressWarnings("unused") @Cached("code.getCallTarget()") final RootCallTarget cachedTarget,
                    @Cached("create(cachedTarget)") final DirectCallNode callNode) {
        return callNode.call(getCreateArgumentsNode().executeCreate(code, contextOrMarker, receiverAndArguments));
    }

    @Specialization(limit = "INLINE_CACHE_SIZE", //
                    guards = {"code.getCallTarget() == cachedTarget"}, //
                    assumptions = "callTargetStable")
    protected final Object doDirect(final CompiledCodeObject code, final Object[] receiverAndArguments, final Object contextOrMarker,
                    @SuppressWarnings("unused") @Cached("code.getCallTargetStable()") final Assumption callTargetStable,
                    @SuppressWarnings("unused") @Cached("code.getCallTarget()") final RootCallTarget cachedTarget,
                    @Cached("create(cachedTarget)") final DirectCallNode callNode) {
        return callNode.call(getCreateArgumentsNode().executeCreate(code, contextOrMarker, receiverAndArguments));
    }

    @Specialization(replaces = "doDirect")
    protected final Object doIndirect(final CompiledCodeObject code, final Object[] receiverAndArguments, final Object contextOrMarker,
                    @Cached("create()") final IndirectCallNode callNode) {
        return callNode.call(code.getCallTarget(), getCreateArgumentsNode().executeCreate(code, contextOrMarker, receiverAndArguments));
    }

    @SuppressWarnings("unused")
    @Fallback
    protected static final Object doFail(final Object method, final Object[] receiverAndArguments, final Object contextOrMarker) {
        throw new SqueakException("failed to lookup generic selector object on generic class");
    }

    private CreateArgumentsNode getCreateArgumentsNode() {
        if (createArgumentsNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            createArgumentsNode = insert(CreateArgumentsNode.create());
        }
        return createArgumentsNode;
    }

    @Override
    public NodeCost getCost() {
        return createArgumentsNode == null ? NodeCost.UNINITIALIZED : NodeCost.MONOMORPHIC;
    }
}
