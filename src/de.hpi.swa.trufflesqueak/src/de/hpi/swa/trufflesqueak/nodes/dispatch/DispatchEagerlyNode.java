/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.profiles.ValueProfile;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetContextOrMarkerNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetOrCreateContextNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.PrimitiveFailedCounter;

@ReportPolymorphism
@ImportStatic(PrimitiveNodeFactory.class)
public abstract class DispatchEagerlyNode extends AbstractNode {
    protected static final int INLINE_CACHE_SIZE = 6;

    public static DispatchEagerlyNode create() {
        return DispatchEagerlyNodeGen.create();
    }

    public abstract Object executeDispatch(VirtualFrame frame, CompiledCodeObject method, Object[] receiverAndArguments);

    @Specialization(guards = {"cachedMethod.hasPrimitive()", "method == cachedMethod", "primitiveNode != null"}, //
                    limit = "INLINE_CACHE_SIZE", assumptions = {"cachedMethod.getCallTargetStable()"}, rewriteOn = PrimitiveFailed.class)
    protected static final Object doPrimitiveEagerly(final VirtualFrame frame, @SuppressWarnings("unused") final CompiledCodeObject method, final Object[] receiverAndArguments,
                    @SuppressWarnings("unused") @Cached("method") final CompiledCodeObject cachedMethod,
                    @Cached("forIndex(cachedMethod, false, cachedMethod.primitiveIndex(), true)") final AbstractPrimitiveNode primitiveNode,
                    @Cached final PrimitiveFailedCounter failureCounter) {
        try {
            return primitiveNode.executeWithArguments(frame, receiverAndArguments);
        } catch (final PrimitiveFailed pf) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            if (failureCounter.shouldNoLongerSendEagerly()) {
                throw pf; // Rewrite specialization.
            } else {
                // Slow path send to fallback code.
                return IndirectCallNode.getUncached().call(cachedMethod.getCallTarget(),
                                FrameAccess.newWith(cachedMethod, FrameAccess.getContextOrMarkerSlow(frame), null, receiverAndArguments));
            }
        }
    }

    @Specialization(guards = {"method == cachedMethod", "cachedMethod.hasPrimitive()"}, //
                    limit = "INLINE_CACHE_SIZE", assumptions = {"cachedMethod.getCallTargetStable()", "cachedMethod.getDoesNotNeedSenderAssumption()"}, replaces = "doPrimitiveEagerly")
    protected static final Object doDirectWithPrimitive(final VirtualFrame frame, @SuppressWarnings("unused") final CompiledCodeObject method, final Object[] receiverAndArguments,
                    @SuppressWarnings("unused") @Cached("method") final CompiledCodeObject cachedMethod,
                    @Cached("forIndex(cachedMethod, false, cachedMethod.primitiveIndex())") final AbstractPrimitiveNode primitiveNode,
                    @Cached final GetContextOrMarkerNode getContextOrMarkerNode,
                    @Cached("create(cachedMethod.getCallTarget())") final DirectCallNode callNode) {
        try {
            return primitiveNode.executeWithArguments(frame, receiverAndArguments);
        } catch (final PrimitiveFailed pf) {
            // FIXME: do something with error code
            return callDirect(callNode, cachedMethod, getContextOrMarkerNode.execute(frame), receiverAndArguments);
        }
    }

    @Specialization(guards = {"method == cachedMethod"}, //
                    limit = "INLINE_CACHE_SIZE", assumptions = {"cachedMethod.getCallTargetStable()", "cachedMethod.getDoesNotNeedSenderAssumption()"})
    protected static final Object doDirect(final VirtualFrame frame, @SuppressWarnings("unused") final CompiledCodeObject method, final Object[] receiverAndArguments,
                    @SuppressWarnings("unused") @Cached("method") final CompiledCodeObject cachedMethod,
                    @Cached final GetContextOrMarkerNode getContextOrMarkerNode,
                    @Cached("create(cachedMethod.getCallTarget())") final DirectCallNode callNode) {
        return callDirect(callNode, cachedMethod, getContextOrMarkerNode.execute(frame), receiverAndArguments);
    }

    @Specialization(guards = {"method == cachedMethod"}, //
                    limit = "INLINE_CACHE_SIZE", assumptions = {"cachedMethod.getCallTargetStable()"}, replaces = {"doPrimitiveEagerly"})
    protected static final Object doDirectWithSender(final VirtualFrame frame, @SuppressWarnings("unused") final CompiledCodeObject method, final Object[] receiverAndArguments,
                    @SuppressWarnings("unused") @Cached("method") final CompiledCodeObject cachedMethod,
                    @Cached("create(true)") final GetOrCreateContextNode getOrCreateContextNode,
                    @Cached("create(cachedMethod.getCallTarget())") final DirectCallNode callNode) {
        return callDirect(callNode, cachedMethod, getOrCreateContextNode.executeGet(frame), receiverAndArguments);
    }

    @Specialization(guards = "doesNotNeedSender(method, assumptionProfile)", replaces = {"doDirect", "doDirectWithSender"}, limit = "1")
    protected static final Object doIndirect(final VirtualFrame frame, final CompiledCodeObject method, final Object[] receiverAndArguments,
                    @Cached final GetContextOrMarkerNode getContextOrMarkerNode,
                    @SuppressWarnings("unused") @Shared("assumptionProfile") @Cached("createClassProfile()") final ValueProfile assumptionProfile,
                    @Cached final IndirectCallNode callNode) {
        assert !method.hasPrimitive();
        return callIndirect(callNode, method, getContextOrMarkerNode.execute(frame), receiverAndArguments);
    }

    @Specialization(guards = "!doesNotNeedSender(method, assumptionProfile)", replaces = {"doDirect", "doDirectWithSender"}, limit = "1")
    protected static final Object doIndirectWithSender(final VirtualFrame frame, final CompiledCodeObject method, final Object[] receiverAndArguments,
                    @Cached("create(true)") final GetOrCreateContextNode getOrCreateContextNode,
                    @SuppressWarnings("unused") @Shared("assumptionProfile") @Cached("createClassProfile()") final ValueProfile assumptionProfile,
                    @Cached final IndirectCallNode callNode) {
        return callIndirect(callNode, method, getOrCreateContextNode.executeGet(frame), receiverAndArguments);
    }

    private static Object callDirect(final DirectCallNode callNode, final CompiledCodeObject cachedMethod, final Object contextOrMarker, final Object[] receiverAndArguments) {
        return callNode.call(FrameAccess.newWith(cachedMethod, contextOrMarker, null, receiverAndArguments));
    }

    private static Object callIndirect(final IndirectCallNode callNode, final CompiledCodeObject method, final Object contextOrMarker, final Object[] receiverAndArguments) {
        return callNode.call(method.getCallTarget(), FrameAccess.newWith(method, contextOrMarker, null, receiverAndArguments));
    }

    protected static final boolean doesNotNeedSender(final CompiledCodeObject method, final ValueProfile assumptionProfile) {
        return assumptionProfile.profile(method.getDoesNotNeedSenderAssumption()).isValid();
    }
}
