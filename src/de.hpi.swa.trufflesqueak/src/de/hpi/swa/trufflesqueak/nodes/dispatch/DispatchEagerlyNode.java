/*
 * Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2023 Oracle and/or its affiliates
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

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetContextOrMarkerNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetOrCreateContextNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory.ArgumentsLocation;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.PrimitiveFailedCounter;

@ImportStatic({PrimitiveNodeFactory.class, ArgumentsLocation.class})
public abstract class DispatchEagerlyNode extends AbstractNode {
    protected static final int INLINE_CACHE_SIZE = 6;

    @Child private GetOrCreateContextNode getOrCreateContextNode;

    public static DispatchEagerlyNode create() {
        return DispatchEagerlyNodeGen.create();
    }

    public abstract Object executeDispatch(VirtualFrame frame, CompiledCodeObject method, Object[] receiverAndArguments);

    @Specialization(guards = {"cachedMethod.hasPrimitive()", "method == cachedMethod", "primitiveNode != null"}, //
                    limit = "INLINE_CACHE_SIZE", assumptions = {"cachedMethod.getCallTargetStable()"}, rewriteOn = PrimitiveFailed.class)
    protected static final Object doPrimitiveEagerly(final VirtualFrame frame, @SuppressWarnings("unused") final CompiledCodeObject method, final Object[] receiverAndArguments,
                    @Cached("method") final CompiledCodeObject cachedMethod,
                    @Cached("getOrCreateIndexedOrNamed(cachedMethod, PROVIDED_ON_EXECUTE)") final AbstractPrimitiveNode primitiveNode,
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

    @Specialization(guards = {"method == cachedMethod"}, //
                    limit = "INLINE_CACHE_SIZE", assumptions = {"cachedMethod.getCallTargetStable()", "cachedMethod.getDoesNotNeedSenderAssumption()"}, replaces = "doPrimitiveEagerly")
    protected static final Object doDirect(final VirtualFrame frame, @SuppressWarnings("unused") final CompiledCodeObject method, final Object[] receiverAndArguments,
                    @Cached("method") final CompiledCodeObject cachedMethod,
                    @Cached final GetContextOrMarkerNode getContextOrMarkerNode,
                    @Cached("create(cachedMethod.getCallTarget())") final DirectCallNode callNode) {
        return callDirect(callNode, cachedMethod, getContextOrMarkerNode.execute(frame), receiverAndArguments);
    }

    @Specialization(guards = {"method == cachedMethod"}, //
                    limit = "INLINE_CACHE_SIZE", assumptions = {"cachedMethod.getCallTargetStable()"}, replaces = {"doPrimitiveEagerly"})
    protected final Object doDirectWithSender(final VirtualFrame frame, @SuppressWarnings("unused") final CompiledCodeObject method, final Object[] receiverAndArguments,
                    @Cached("method") final CompiledCodeObject cachedMethod,
                    @Cached("create(cachedMethod.getCallTarget())") final DirectCallNode callNode) {
        return callDirect(callNode, cachedMethod, getOrCreateContext(frame), receiverAndArguments);
    }

    @ReportPolymorphism.Megamorphic
    @Specialization(guards = "doesNotNeedSender(method, assumptionProfile)", replaces = {"doDirect", "doDirectWithSender"}, limit = "1")
    protected static final Object doIndirect(final VirtualFrame frame, final CompiledCodeObject method, final Object[] receiverAndArguments,
                    @Cached final GetContextOrMarkerNode getContextOrMarkerNode,
                    @SuppressWarnings("unused") @Shared("assumptionProfile") @Cached("createClassProfile()") final ValueProfile assumptionProfile,
                    @Cached final IndirectCallNode callNode) {
        return callIndirect(callNode, method, getContextOrMarkerNode.execute(frame), receiverAndArguments);
    }

    @ReportPolymorphism.Megamorphic
    @Specialization(guards = "!doesNotNeedSender(method, assumptionProfile)", replaces = {"doDirect", "doDirectWithSender"}, limit = "1")
    protected final Object doIndirectWithSender(final VirtualFrame frame, final CompiledCodeObject method, final Object[] receiverAndArguments,
                    @SuppressWarnings("unused") @Shared("assumptionProfile") @Cached("createClassProfile()") final ValueProfile assumptionProfile,
                    @Cached final IndirectCallNode callNode) {
        return callIndirect(callNode, method, getOrCreateContext(frame), receiverAndArguments);
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

    private ContextObject getOrCreateContext(final VirtualFrame frame) {
        if (getOrCreateContextNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getOrCreateContextNode = insert(GetOrCreateContextNode.create());
        }
        return getOrCreateContextNode.executeGet(frame);
    }
}
