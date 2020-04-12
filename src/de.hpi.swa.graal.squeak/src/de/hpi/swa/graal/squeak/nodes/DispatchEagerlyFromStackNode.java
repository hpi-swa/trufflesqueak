/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.nodes.context.frame.CreateFrameArgumentsNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackPopNNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.graal.squeak.util.FrameAccess;

@ReportPolymorphism
@ImportStatic({PrimitiveNodeFactory.class, FrameAccess.class})
public abstract class DispatchEagerlyFromStackNode extends AbstractNodeWithCode {
    protected static final int INLINE_CACHE_SIZE = 6;

    protected final int argumentCount;

    protected DispatchEagerlyFromStackNode(final CompiledCodeObject code, final int argumentCount) {
        super(code);
        this.argumentCount = argumentCount;
    }

    public static DispatchEagerlyFromStackNode create(final CompiledCodeObject code, final int argumentCount) {
        return DispatchEagerlyFromStackNodeGen.create(code, argumentCount);
    }

    public abstract Object executeDispatch(VirtualFrame frame, CompiledMethodObject method);

    @Specialization(guards = {"cachedMethod.hasPrimitive()", "method == cachedMethod", "primitiveNode != null"}, //
                    limit = "INLINE_CACHE_SIZE", assumptions = {"cachedMethod.getCallTargetStable()"}, rewriteOn = PrimitiveFailed.class)
    protected final Object doPrimitiveEagerly(final VirtualFrame frame, @SuppressWarnings("unused") final CompiledMethodObject method,
                    @SuppressWarnings("unused") @Cached("method") final CompiledMethodObject cachedMethod,
                    @Cached("forIndex(cachedMethod, true, cachedMethod.primitiveIndex())") final AbstractPrimitiveNode primitiveNode,
                    @Cached("getStackPointer(frame, code)") final int stackPointer,
                    @Cached final PrimitiveFailedCounter failureCounter) {
        /**
         * Pretend that values are popped off the stack. Primitive nodes will read them using
         * ArgumentOnStackNodes.
         */
        FrameAccess.setStackPointer(frame, code, stackPointer - 1 - argumentCount);
        try {
            return primitiveNode.executePrimitive(frame);
        } catch (final PrimitiveFailed pf) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            FrameAccess.setStackPointer(frame, code, stackPointer); // Restore stackPointer.
            if (failureCounter.shouldNoLongerSendEagerly()) {
                throw pf; // Rewrite specialization.
            } else {
                // Slow path send to fallback code.
                final Object[] receiverAndArguments = FrameStackPopNNode.create(code, 1 + argumentCount).execute(frame);
                return IndirectCallNode.getUncached().call(method.getCallTarget(),
                                FrameAccess.newWith(cachedMethod, getContextOrMarker(frame), null, receiverAndArguments));
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
    protected final Object doDirect(final VirtualFrame frame, @SuppressWarnings("unused") final CompiledMethodObject method,
                    @SuppressWarnings("unused") @Cached("method") final CompiledMethodObject cachedMethod,
                    @Cached("create(code, argumentCount)") final CreateFrameArgumentsNode argumentsNode,
                    @Cached("create(cachedMethod.getCallTarget())") final DirectCallNode callNode) {
        return callNode.call(argumentsNode.execute(frame, cachedMethod, getContextOrMarker(frame)));
    }

    @Specialization(guards = {"method == cachedMethod"}, //
                    limit = "INLINE_CACHE_SIZE", assumptions = {"cachedMethod.getCallTargetStable()"}, replaces = {"doPrimitiveEagerly"})
    protected static final Object doDirectWithSender(final VirtualFrame frame, @SuppressWarnings("unused") final CompiledMethodObject method,
                    @SuppressWarnings("unused") @Cached("method") final CompiledMethodObject cachedMethod,
                    @Cached("create(code, true)") final GetOrCreateContextNode getOrCreateContextNode,
                    @Cached("create(code, argumentCount)") final CreateFrameArgumentsNode argumentsNode,
                    @Cached("create(cachedMethod.getCallTarget())") final DirectCallNode callNode) {
        return callNode.call(argumentsNode.execute(frame, cachedMethod, getOrCreateContextNode.executeGet(frame)));
    }

    @Specialization(guards = "method.getDoesNotNeedSenderAssumption().isValid()", replaces = {"doDirect", "doDirectWithSender"})
    protected final Object doIndirect(final VirtualFrame frame, final CompiledMethodObject method,
                    @Cached("create(code, argumentCount)") final CreateFrameArgumentsNode argumentsNode,
                    @Cached final IndirectCallNode callNode) {
        return callNode.call(method.getCallTarget(), argumentsNode.execute(frame, method, getContextOrMarker(frame)));
    }

    @Specialization(guards = "!method.getDoesNotNeedSenderAssumption().isValid()", replaces = {"doDirect", "doDirectWithSender"})
    protected static final Object doIndirectWithSender(final VirtualFrame frame, final CompiledMethodObject method,
                    @Cached("create(code, true)") final GetOrCreateContextNode getOrCreateContextNode,
                    @Cached("create(code, argumentCount)") final CreateFrameArgumentsNode argumentsNode,
                    @Cached final IndirectCallNode callNode) {
        return callNode.call(method.getCallTarget(), argumentsNode.execute(frame, method, getOrCreateContextNode.executeGet(frame)));
    }
}
