package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

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
    @Specialization(guards = {"!isQuickReturnReceiverVariable(method.primitiveIndex())", "method == cachedMethod", "method.hasPrimitive()", "primitiveNode != null"}, assumptions = {
                    "callTargetStable"}, rewriteOn = {PrimitiveFailed.class, UnsupportedSpecializationException.class})
    protected static final Object doPrimitiveEagerly(final VirtualFrame frame, final CompiledMethodObject method, final Object[] receiverAndArguments, final Object contextOrMarker,
                    @Cached("method") final CompiledMethodObject cachedMethod,
                    @Cached("method.getCallTargetStable()") final Assumption callTargetStable,
                    @Cached("forIndex(method, method.primitiveIndex())") final AbstractPrimitiveNode primitiveNode,
                    @Cached("create()") final CreateEagerArgumentsNode createEagerArgumentsNode) {
        return primitiveNode.executeWithArguments(frame, createEagerArgumentsNode.executeCreate(primitiveNode.numArguments, receiverAndArguments));
    }

    @Specialization(guards = {"method == cachedMethod"}, assumptions = {"callTargetStable"}, replaces = "doPrimitiveEagerly")
    protected final Object doDirect(final CompiledCodeObject method, final Object[] receiverAndArguments, final Object contextOrMarker,
                    @Cached("method") final CompiledCodeObject cachedMethod,
                    @Cached("create()") final InvokeNode invokeNode,
                    @SuppressWarnings("unused") @Cached("method.getCallTargetStable()") final Assumption callTargetStable) {
        return invokeNode.executeInvoke(cachedMethod, getCreateArgumentsNode().executeCreate(method, contextOrMarker, receiverAndArguments));
    }

    @Specialization(replaces = "doDirect")
    protected final Object doIndirect(final CompiledCodeObject method, final Object[] receiverAndArguments, final Object contextOrMarker,
                    @Cached("create()") final InvokeNode invokeNode) {
        return invokeNode.executeInvoke(method, getCreateArgumentsNode().executeCreate(method, contextOrMarker, receiverAndArguments));
    }

    @SuppressWarnings("unused")
    @Fallback
    protected static final Object doFail(final Object method, final Object[] receiverAndArguments, final Object contextOrMarker) {
        throw new SqueakException("failed to lookup generic selector object on generic class");
    }

    protected final CreateArgumentsNode getCreateArgumentsNode() {
        if (createArgumentsNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            createArgumentsNode = insert(CreateArgumentsNode.create());
        }
        return createArgumentsNode;
    }
}
