package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.exceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

@ImportStatic(PrimitiveNodeFactory.class)
public abstract class DispatchNode extends Node {

    public static DispatchNode create() {
        return DispatchNodeGen.create();
    }

    public abstract Object executeDispatch(VirtualFrame frame, Object method, Object[] receiverAndArguments, Object contextOrMarker);

    @Specialization(guards = {"264 <= method.primitiveIndex()", "method.primitiveIndex() <= 520"})
    protected Object doPrimitiveQuickReturnReceiver(CompiledMethodObject method, Object[] receiverAndArguments, @SuppressWarnings("unused") Object contextOrMarker) {
        assert receiverAndArguments[0] instanceof BaseSqueakObject;
        return ((BaseSqueakObject) receiverAndArguments[0]).at0(method.primitiveIndex() - 264);
    }

    // FIXME: primitive 84 cannot be the only one, figure out what's going on
    @SuppressWarnings("unused")
    @Specialization(guards = {"method == cachedMethod", "method.hasPrimitive()", "method.primitiveIndex() != 84", "primitiveNode != null"}, assumptions = {"callTargetStable"}, rewriteOn = {
                    PrimitiveFailed.class,
                    UnsupportedSpecializationException.class})
    protected Object doPrimitiveEagerly(VirtualFrame frame, CompiledMethodObject method, Object[] receiverAndArguments, Object contextOrMarker,
                    @Cached("method") CompiledMethodObject cachedMethod,
                    @Cached("method.getCallTargetStable()") Assumption callTargetStable,
                    @Cached("forIndex(method, method.primitiveIndex())") AbstractPrimitiveNode primitiveNode) {
        return primitiveNode.executeWithArguments(frame, receiverAndArguments);
    }

    @Specialization(guards = {"method == cachedMethod"}, assumptions = {"callTargetStable"}, replaces = "doPrimitiveEagerly")
    protected Object doDirect(CompiledCodeObject method, Object[] receiverAndArguments, Object contextOrMarker,
                    @Cached("method") CompiledCodeObject cachedMethod,
                    @Cached("create()") InvokeNode invokeNode,
                    @SuppressWarnings("unused") @Cached("method.getCallTargetStable()") Assumption callTargetStable) {
        return invokeNode.executeInvoke(cachedMethod, FrameAccess.newWith(method, contextOrMarker, null, receiverAndArguments));
    }

    @Specialization(replaces = "doDirect")
    protected Object doIndirect(CompiledCodeObject method, Object[] receiverAndArguments, Object contextOrMarker,
                    @Cached("create()") InvokeNode invokeNode) {
        return invokeNode.executeInvoke(method, FrameAccess.newWith(method, contextOrMarker, null, receiverAndArguments));
    }

    @SuppressWarnings("unused")
    @Fallback
    protected Object fail(Object method, Object[] receiverAndArguments, Object contextOrMarker) {
        throw new SqueakException("failed to lookup generic selector object on generic class");
    }
}
