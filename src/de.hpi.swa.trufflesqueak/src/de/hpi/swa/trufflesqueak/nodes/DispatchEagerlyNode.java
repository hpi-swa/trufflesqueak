/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

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
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.context.frame.CreateEagerArgumentsNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetContextOrMarkerNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetOrCreateContextNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

@ReportPolymorphism
@ImportStatic(PrimitiveNodeFactory.class)
public abstract class DispatchEagerlyNode extends AbstractNode {
    protected static final int INLINE_CACHE_SIZE = 6;

    public static DispatchEagerlyNode create() {
        return DispatchEagerlyNodeGen.create();
    }

    public abstract Object executeDispatch(VirtualFrame frame, CompiledMethodObject method, Object[] receiverAndArguments);

    @Specialization(guards = {"cachedMethod.hasPrimitive()", "method == cachedMethod", "primitiveNode != null"}, //
                    limit = "INLINE_CACHE_SIZE", assumptions = {"cachedMethod.getCallTargetStable()"}, rewriteOn = PrimitiveFailed.class)
    protected static final Object doPrimitiveEagerly(final VirtualFrame frame, @SuppressWarnings("unused") final CompiledMethodObject method, final Object[] receiverAndArguments,
                    @SuppressWarnings("unused") @Cached("method") final CompiledMethodObject cachedMethod,
                    @Cached("forIndex(cachedMethod, false, cachedMethod.primitiveIndex())") final AbstractPrimitiveNode primitiveNode,
                    @Cached final CreateEagerArgumentsNode createEagerArgumentsNode,
                    @Cached final PrimitiveFailedCounter failureCounter) {
        try {
            return primitiveNode.executeWithArguments(frame, createEagerArgumentsNode.executeCreate(primitiveNode.getNumArguments(), receiverAndArguments));
        } catch (final PrimitiveFailed pf) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            if (failureCounter.shouldNoLongerSendEagerly()) {
                throw pf; // Rewrite specialization.
            } else {
                // Slow path send to fallback code.
                return IndirectCallNode.getUncached().call(method.getCallTarget(),
                                FrameAccess.newWith(cachedMethod, FrameAccess.getContextOrMarkerSlow(frame), null, receiverAndArguments));
            }
        }
    }

    /*
     * Counts how often a primitive has failed and indicates whether this node should continue to
     * send the primitive eagerly or not. This is useful to avoid rewriting primitives that set up
     * the image and then are retried in their fallback code (e.g. primitiveCopyBits).
     */
    protected static final class PrimitiveFailedCounter {
        private static final int PRIMITIVE_FAILED_THRESHOLD = 3;
        private int count = 0;

        protected static PrimitiveFailedCounter create() {
            return new PrimitiveFailedCounter();
        }

        protected boolean shouldNoLongerSendEagerly() {
            return ++count > PRIMITIVE_FAILED_THRESHOLD;
        }
    }

    @Specialization(guards = {"method == cachedMethod"}, //
                    limit = "INLINE_CACHE_SIZE", assumptions = {"cachedMethod.getCallTargetStable()", "cachedMethod.getDoesNotNeedSenderAssumption()"}, replaces = "doPrimitiveEagerly")
    protected static final Object doDirect(final VirtualFrame frame, @SuppressWarnings("unused") final CompiledMethodObject method, final Object[] receiverAndArguments,
                    @SuppressWarnings("unused") @Cached("method") final CompiledMethodObject cachedMethod,
                    @Cached final GetContextOrMarkerNode getContextOrMarkerNode,
                    @Cached("create(cachedMethod.getCallTarget())") final DirectCallNode callNode) {
        return callDirect(callNode, cachedMethod, getContextOrMarkerNode.execute(frame), receiverAndArguments);
    }

    @Specialization(guards = {"method == cachedMethod"}, //
                    limit = "INLINE_CACHE_SIZE", assumptions = {"cachedMethod.getCallTargetStable()"}, replaces = {"doPrimitiveEagerly"})
    protected static final Object doDirectWithSender(final VirtualFrame frame, @SuppressWarnings("unused") final CompiledMethodObject method, final Object[] receiverAndArguments,
                    @SuppressWarnings("unused") @Cached("method") final CompiledMethodObject cachedMethod,
                    @Cached("create(true)") final GetOrCreateContextNode getOrCreateContextNode,
                    @Cached("create(cachedMethod.getCallTarget())") final DirectCallNode callNode) {
        return callDirect(callNode, cachedMethod, getOrCreateContextNode.executeGet(frame), receiverAndArguments);
    }

    @Specialization(guards = "doesNotNeedSender(method, assumptionProfile)", replaces = {"doDirect", "doDirectWithSender"}, limit = "1")
    protected static final Object doIndirect(final VirtualFrame frame, final CompiledMethodObject method, final Object[] receiverAndArguments,
                    @Cached final GetContextOrMarkerNode getContextOrMarkerNode,
                    @SuppressWarnings("unused") @Shared("assumptionProfile") @Cached("createClassProfile()") final ValueProfile assumptionProfile,
                    @Cached final IndirectCallNode callNode) {
        return callIndirect(callNode, method, getContextOrMarkerNode.execute(frame), receiverAndArguments);
    }

    @Specialization(guards = "!doesNotNeedSender(method, assumptionProfile)", replaces = {"doDirect", "doDirectWithSender"}, limit = "1")
    protected static final Object doIndirectWithSender(final VirtualFrame frame, final CompiledMethodObject method, final Object[] receiverAndArguments,
                    @Cached("create(true)") final GetOrCreateContextNode getOrCreateContextNode,
                    @SuppressWarnings("unused") @Shared("assumptionProfile") @Cached("createClassProfile()") final ValueProfile assumptionProfile,
                    @Cached final IndirectCallNode callNode) {
        return callIndirect(callNode, method, getOrCreateContextNode.executeGet(frame), receiverAndArguments);
    }

    private static Object callDirect(final DirectCallNode callNode, final CompiledMethodObject cachedMethod, final Object contextOrMarker, final Object[] receiverAndArguments) {
        return callNode.call(FrameAccess.newWith(cachedMethod, contextOrMarker, null, receiverAndArguments));
    }

    private static Object callIndirect(final IndirectCallNode callNode, final CompiledMethodObject method, final Object contextOrMarker, final Object[] receiverAndArguments) {
        return callNode.call(method.getCallTarget(), FrameAccess.newWith(method, contextOrMarker, null, receiverAndArguments));
    }

    protected static final boolean doesNotNeedSender(final CompiledMethodObject method, final ValueProfile assumptionProfile) {
        return assumptionProfile.profile(method.getDoesNotNeedSenderAssumption()).isValid();
    }
}
