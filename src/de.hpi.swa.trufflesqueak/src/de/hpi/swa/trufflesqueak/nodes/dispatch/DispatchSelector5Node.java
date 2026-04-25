/*
 * Copyright (c) 2025-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2025-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.LookupMethodNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.trufflesqueak.nodes.context.GetOrCreateContextWithoutFrameNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector5NodeFactory.DispatchDirectPrimitiveFallback5NodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNode.DispatchPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive5;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class DispatchSelector5Node extends DispatchSelectorNode {
    public abstract static class DispatchDirect5Node extends AbstractDispatchDirectNode {
        DispatchDirect5Node(final Assumption[] assumptions) {
            super(assumptions);
        }

        public abstract Object execute(VirtualFrame frame, Object receiver, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5);

        @NeverDefault
        public static final DispatchDirect5Node create(final NativeObject selector, final LookupClassGuard guard, final boolean canPrimFail) {
            final ClassObject receiverClass = guard.getSqueakClassInternal(null);
            return create(selector, receiverClass, canPrimFail);
        }

        @NeverDefault
        public static final DispatchDirect5Node create(final NativeObject selector, final ClassObject lookupClass, final boolean canPrimFail) {
            final Object lookupResult = lookupClass.lookupInMethodDictSlow(selector);
            final Assumption[] assumptions = DispatchUtils.createAssumptions(lookupClass, lookupResult);
            if (lookupResult instanceof final CompiledCodeObject lookupMethod) {
                if (lookupMethod.getNumArgs() == 5) {
                    return create(assumptions, lookupMethod);
                } else {
                    // argument count mismatch
                    if (canPrimFail) {
                        return new DispatchDirectPrimitiveBadArguments5Node(assumptions);
                    } else {
                        return create(assumptions, lookupMethod);
                    }
                }
            } else if (lookupResult == null) {
                return createMessageFallbackNode(selector, assumptions, lookupClass);
            } else {
                final ClassObject lookupResultClass = SqueakObjectClassNode.executeUncached(lookupResult);
                final Object runWithInLookupResult = LookupMethodNode.executeUncached(lookupResultClass, SqueakImageContext.getSlow().runWithInSelector);
                if (runWithInLookupResult instanceof final CompiledCodeObject runWithInMethod) {
                    return new DispatchDirectObjectAsMethod5Node(assumptions, selector, runWithInMethod, lookupResult);
                } else {
                    assert runWithInLookupResult == null : "runWithInLookupResult should not be another Object";
                    return createMessageFallbackNode(selector, assumptions, lookupResultClass);
                }
            }
        }

        private static DispatchDirect5Node create(final Assumption[] assumptions, final CompiledCodeObject method) {
            assert checkArgumentCount(method, 5);
            if (method.hasPrimitive()) {
                final AbstractPrimitiveNode primitiveNode = PrimitiveNodeFactory.getOrCreateIndexedOrNamed(method);
                if (primitiveNode instanceof final Primitive5 primitive5) {
                    return new DispatchDirectPrimitive5Node(assumptions, method, primitive5);
                }
                DispatchUtils.logMissingPrimitive(primitiveNode, method);
            }
            return new DispatchDirectMethod5Node(assumptions, method);
        }

        private static DispatchDirect5Node createMessageFallbackNode(final NativeObject selector, final Assumption[] assumptions, final ClassObject receiverClass) {
            final ClassObject.DispatchFailureResult result = receiverClass.resolveDispatchFailure(selector, 5);
            final Assumption[] finalAssumptions = DispatchUtils.getAssumptionsForMessageFallback(assumptions, selector, result.fallbackMethod());

            if (result.convention() == ClassObject.FallbackConvention.CANNOT_INTERPRET) {
                return new DispatchDirectCannotInterpretFallback5Node(finalAssumptions, selector, result.fallbackMethod(), result.fallbackDepth(), result.fallbackSelector());
            } else {
                assert result.convention() == ClassObject.FallbackConvention.STANDARD_DNU : "DNU shortcuts are not supported for arity 4+";
                return new DispatchDirectDNUFallback5Node(finalAssumptions, selector, result.fallbackMethod());
            }
        }
    }

    static final class DispatchDirectPrimitive5Node extends DispatchDirect5Node {
        @Child private Primitive5 primitiveNode;
        @Child private DispatchDirectPrimitiveFallback5Node dispatchFallbackNode;

        DispatchDirectPrimitive5Node(final Assumption[] assumptions, final CompiledCodeObject method, final Primitive5 primitiveNode) {
            super(assumptions);
            this.primitiveNode = primitiveNode;
            dispatchFallbackNode = DispatchDirectPrimitiveFallback5NodeGen.create(method);
        }

        @Override
        public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5) {
            try {
                return primitiveNode.execute(frame, receiver, arg1, arg2, arg3, arg4, arg5);
            } catch (final PrimitiveFailed pf) {
                DispatchUtils.logPrimitiveFailed(primitiveNode);
                return dispatchFallbackNode.execute(frame, receiver, arg1, arg2, arg3, arg4, arg5, pf);
            }
        }
    }

    static final class DispatchDirectPrimitiveBadArguments5Node extends DispatchDirect5Node {
        DispatchDirectPrimitiveBadArguments5Node(final Assumption[] assumptions) {
            super(assumptions);
        }

        @Override
        public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5) {
            CompilerDirectives.transferToInterpreter();
            throw PrimitiveFailed.BAD_NUMBER_OF_ARGUMENTS;
        }
    }

    abstract static class DispatchDirectPrimitiveFallback5Node extends AbstractNode {
        protected final CompiledCodeObject method;

        DispatchDirectPrimitiveFallback5Node(final CompiledCodeObject method) {
            this.method = method;
        }

        protected abstract Object execute(VirtualFrame frame, Object receiver, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, PrimitiveFailed pf);

        @Specialization
        protected static final Object doFallback(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5,
                        final PrimitiveFailed pf,
                        @Bind final Node node,
                        @Cached("create(method)") final HandlePrimitiveFailedNode handlePrimitiveFailedNode,
                        @Cached(inline = true) final GetOrCreateContextWithoutFrameNode senderNode,
                        @Cached("create(method.getCallTarget())") final DirectCallNode callNode) {
            handlePrimitiveFailedNode.execute(pf);
            return callNode.call(FrameAccess.newWith(senderNode.execute(frame, node), null, receiver, arg1, arg2, arg3, arg4, arg5));
        }
    }

    abstract static class DispatchDirectWithSender5Node extends DispatchDirect5Node {
        @Child protected GetOrCreateContextWithoutFrameNode senderNode = GetOrCreateContextWithoutFrameNode.create();

        DispatchDirectWithSender5Node(final Assumption[] assumptions) {
            super(assumptions);
        }
    }

    static final class DispatchDirectMethod5Node extends DispatchDirectWithSender5Node {
        @Child private DirectCallNode callNode;

        DispatchDirectMethod5Node(final Assumption[] assumptions, final CompiledCodeObject method) {
            super(assumptions);
            callNode = DirectCallNode.create(method.getCallTarget());
        }

        @Override
        public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5) {
            return callNode.call(FrameAccess.newWith(senderNode.execute(frame), null, receiver, arg1, arg2, arg3, arg4, arg5));
        }
    }

    abstract static class AbstractDispatchDirectFallback5Node extends DispatchDirectWithSender5Node {
        protected final NativeObject selector;
        @Child protected DirectCallNode callNode;

        AbstractDispatchDirectFallback5Node(final Assumption[] assumptions, final NativeObject selector, final CompiledCodeObject targetMethod) {
            super(assumptions);
            this.selector = selector;
            this.callNode = DirectCallNode.create(targetMethod.getCallTarget());
        }

        @Override
        public abstract Object execute(VirtualFrame frame, Object receiver, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5);
    }

    static final class DispatchDirectDNUFallback5Node extends AbstractDispatchDirectFallback5Node {
        @Child private CreateMessageNode createMessageNode = CreateMessageNodeGen.create();

        DispatchDirectDNUFallback5Node(final Assumption[] assumptions, final NativeObject selector, final CompiledCodeObject dnuMethod) {
            super(assumptions, selector, dnuMethod);
        }

        @Override
        public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5) {
            final PointersObject message = createMessageNode.execute(selector, receiver, new Object[]{arg1, arg2, arg3, arg4, arg5});
            return callNode.call(FrameAccess.newMessageFallbackWith(senderNode.execute(frame), receiver, message));
        }
    }

    static final class DispatchDirectCannotInterpretFallback5Node extends AbstractDispatchDirectFallback5Node {
        private final int fallbackDepth;
        private final NativeObject ciSelector;
        @Child private CreateMessageNode createMessageNode = CreateMessageNodeGen.create();

        DispatchDirectCannotInterpretFallback5Node(final Assumption[] assumptions, final NativeObject selector, final CompiledCodeObject ciMethod, final int fallbackDepth,
                        final NativeObject ciSelector) {
            super(assumptions, selector, ciMethod);
            this.fallbackDepth = fallbackDepth;
            this.ciSelector = ciSelector;
        }

        @Override
        public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5) {
            final PointersObject message = DispatchUtils.buildNestedMessage(
                            createMessageNode,
                            selector,
                            ciSelector,
                            receiver,
                            new Object[]{arg1, arg2, arg3, arg4, arg5},
                            fallbackDepth);
            return callNode.call(FrameAccess.newMessageFallbackWith(senderNode.execute(frame), receiver, message));
        }
    }

    static final class DispatchDirectObjectAsMethod5Node extends DispatchDirectWithSender5Node {
        private final NativeObject selector;
        private final Object targetObject;
        @Child private DirectCallNode callNode;

        DispatchDirectObjectAsMethod5Node(final Assumption[] assumptions, final NativeObject selector, final CompiledCodeObject runWithInMethod, final Object targetObject) {
            super(assumptions);
            this.selector = selector;
            callNode = DirectCallNode.create(runWithInMethod.getCallTarget());
            this.targetObject = targetObject;
        }

        @Override
        public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5) {
            return callNode.call(FrameAccess.newOAMWith(senderNode.execute(frame), targetObject, selector, getContext().asArrayOfObjects(arg1, arg2, arg3, arg4, arg5), receiver));
        }
    }

    @GenerateInline(false)
    public abstract static class DispatchIndirect5Node extends AbstractNode {
        public abstract Object execute(VirtualFrame frame, boolean canPrimFail, NativeObject selector, Object receiver, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5);

        @Specialization
        protected static final Object doIndirect(final VirtualFrame frame, final boolean canPrimFail, final NativeObject selector, final Object receiver, final Object arg1, final Object arg2,
                        final Object arg3, final Object arg4, final Object arg5,
                        @Bind final Node node,
                        @Cached(inline = true) final SqueakObjectClassNode classNode,
                        @Cached final ResolveMethodNode methodNode,
                        @Cached final TryPrimitive5Node tryPrimitiveNode,
                        @Cached(inline = true) final GetOrCreateContextWithoutFrameNode senderNode,
                        @Cached final CreateFrameArgumentsForIndirectCall5Node argumentsNode,
                        @Cached final IndirectCallNode callNode) {
            CompilerAsserts.partialEvaluationConstant(canPrimFail);
            final ClassObject receiverClass = classNode.executeLookup(node, receiver);
            final Object lookupResult = getContext(node).lookup(receiverClass, selector);
            final CompiledCodeObject method = methodNode.execute(node, getContext(node), 5, canPrimFail, selector, receiverClass, lookupResult);

            final Object result;
            if (lookupResult instanceof CompiledCodeObject) {
                result = tryPrimitiveNode.execute(frame, method, receiver, arg1, arg2, arg3, arg4, arg5);
            } else {
                result = null;
            }

            if (result != null) {
                return result;
            } else {
                return callNode.call(method.getCallTarget(),
                                argumentsNode.execute(node, senderNode.execute(frame, node), receiver, arg1, arg2, arg3, arg4, arg5, receiverClass, lookupResult, selector));
            }
        }

        @GenerateInline(false)
        @ImportStatic(PrimitiveNodeFactory.class)
        protected abstract static class TryPrimitive5Node extends AbstractNode {
            abstract Object execute(VirtualFrame frame, CompiledCodeObject method, Object receiver, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5);

            @SuppressWarnings("unused")
            @Specialization(guards = "method.getPrimitiveNodeOrNull() == null")
            protected static final Object doNoPrimitive(final CompiledCodeObject method, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4,
                            final Object arg5) {
                return null;
            }

            @Specialization(guards = {"method == cachedMethod", "primitiveNode != null"}, limit = "INDIRECT_PRIMITIVE_CACHE_LIMIT")
            protected static final Object doCached(final VirtualFrame frame, @SuppressWarnings("unused") final CompiledCodeObject method, final Object receiver, final Object arg1,
                            final Object arg2, final Object arg3, final Object arg4, final Object arg5,
                            @Bind final Node node,
                            @SuppressWarnings("unused") @Cached("method") final CompiledCodeObject cachedMethod,
                            @Cached("getOrCreateIndexedOrNamed(cachedMethod)") final AbstractPrimitiveNode primitiveNode,
                            @Cached final InlinedBranchProfile primitiveFailedProfile) {
                try {
                    return ((Primitive5) primitiveNode).execute(frame, receiver, arg1, arg2, arg3, arg4, arg5);
                } catch (final PrimitiveFailed pf) {
                    primitiveFailedProfile.enter(node);
                    DispatchUtils.handlePrimitiveFailedIndirect(node, method, pf);
                    return null;
                }
            }

            @Specialization(replaces = {"doNoPrimitive", "doCached"})
            protected static final Object doUncached(final VirtualFrame frame, final CompiledCodeObject method, final Object receiver, final Object arg1, final Object arg2, final Object arg3,
                            final Object arg4, final Object arg5,
                            @Bind final Node node,
                            @Cached final InlinedConditionProfile needsFrameProfile) {
                final DispatchPrimitiveNode primitiveNode = method.getPrimitiveNodeOrNull();
                if (primitiveNode != null) {
                    final MaterializedFrame frameOrNull = needsFrameProfile.profile(node, primitiveNode.needsFrame()) ? frame.materialize() : null;
                    return tryPrimitive(primitiveNode, frameOrNull, node, method, receiver, arg1, arg2, arg3, arg4, arg5);
                } else {
                    return null;
                }
            }

            @TruffleBoundary
            private static Object tryPrimitive(final DispatchPrimitiveNode primitiveNode, final MaterializedFrame frame, final Node node, final CompiledCodeObject method, final Object receiver,
                            final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5) {
                try {
                    return ((DispatchPrimitiveNode.DispatchPrimitive5Node) primitiveNode).execute(frame, receiver, arg1, arg2, arg3, arg4, arg5);
                } catch (final PrimitiveFailed pf) {
                    DispatchUtils.handlePrimitiveFailedIndirect(node, method, pf);
                    return null;
                }
            }
        }

        @GenerateInline
        @GenerateCached(false)
        protected abstract static class CreateFrameArgumentsForIndirectCall5Node extends AbstractNode {
            abstract Object[] execute(Node node, AbstractSqueakObject sender, Object receiver, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, ClassObject receiverClass,
                            Object lookupResult, NativeObject selector);

            @Specialization
            @SuppressWarnings("unused")
            protected static final Object[] doMethod(final Node node, final AbstractSqueakObject sender, final Object receiver, final Object arg1, final Object arg2, final Object arg3,
                            final Object arg4, final Object arg5, final ClassObject receiverClass, final CompiledCodeObject lookupResult, final NativeObject selector) {
                return FrameAccess.newWith(sender, null, receiver, arg1, arg2, arg3, arg4, arg5);
            }

            @Specialization(guards = "lookupResult == null")
            protected static final Object[] doMessageFallback(final Node node, final AbstractSqueakObject sender, final Object receiver, final Object arg1, final Object arg2, final Object arg3,
                            final Object arg4, final Object arg5, final ClassObject receiverClass, @SuppressWarnings("unused") final Object lookupResult, final NativeObject selector,
                            @Cached(inline = false) final AbstractPointersObjectWriteNode writeNode,
                            @Cached(inline = false) final CreateMessageNode createMessageNode) {
                final ClassObject.DispatchFailureResult result = getContext(node).findMethodCacheEntry(receiverClass, selector).getOrCreateDispatchFailureResult(5);
                final Object[] arguments = new Object[]{arg1, arg2, arg3, arg4, arg5};

                final PointersObject message;
                if (result.convention() == ClassObject.FallbackConvention.CANNOT_INTERPRET) {
                    message = DispatchUtils.buildNestedMessage(createMessageNode, selector, result.fallbackSelector(), receiver, arguments, result.fallbackDepth());
                } else {
                    message = getContext(node).newMessage(writeNode, selector, receiverClass, arguments);
                }
                return FrameAccess.newMessageFallbackWith(sender, receiver, message);
            }

            @Specialization(guards = {"targetObject != null", "!isCompiledCodeObject(targetObject)"})
            protected static final Object[] doObjectAsMethod(final Node node, final AbstractSqueakObject sender, final Object receiver, final Object arg1, final Object arg2, final Object arg3,
                            final Object arg4, final Object arg5, @SuppressWarnings("unused") final ClassObject receiverClass, final Object targetObject, final NativeObject selector) {
                final Object[] arguments = new Object[]{arg1, arg2, arg3, arg4, arg5};
                return FrameAccess.newOAMWith(sender, targetObject, selector, getContext(node).asArrayOfObjects(arguments), receiver);
            }
        }
    }
}
