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
import com.oracle.truffle.api.HostCompilerDirectives;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
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
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector2NodeFactory.DispatchDirectPrimitiveFallback2NodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNode.DispatchPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive2;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class DispatchSelector2Node extends DispatchSelectorNode {
    public abstract static class Dispatch2Node extends AbstractDispatchNode {
        Dispatch2Node(final NativeObject selector) {
            super(selector);
        }

        public abstract Object execute(VirtualFrame frame, Object receiver, Object arg1, Object arg2);

        @Specialization(guards = "guard.check(receiver)", assumptions = "dispatchDirectNode.getAssumptions()", limit = "INLINE_METHOD_CACHE_LIMIT")
        protected static final Object doDirect(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2,
                        @SuppressWarnings("unused") @Cached("create(receiver)") final LookupClassGuard guard,
                        @Cached("create(selector, guard)") final DispatchDirect2Node dispatchDirectNode) {
            return dispatchDirectNode.execute(frame, receiver, arg1, arg2);
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(replaces = "doDirect")
        @HostCompilerDirectives.InliningCutoff
        @SuppressWarnings("truffle-static-method")
        protected final Object doIndirect(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2,
                        @Cached final DispatchIndirect2Node dispatchNode) {
            return dispatchNode.execute(frame, false, selector, receiver, arg1, arg2);
        }
    }

    public abstract static class DispatchDirect2Node extends AbstractDispatchDirectNode {
        DispatchDirect2Node(final Assumption[] assumptions) {
            super(assumptions);
        }

        public abstract Object execute(VirtualFrame frame, Object receiver, Object arg1, Object arg2);

        @NeverDefault
        protected static final DispatchDirect2Node create(final NativeObject selector, final LookupClassGuard guard) {
            return create(selector, guard, true);
        }

        @NeverDefault
        public static final DispatchDirect2Node create(final NativeObject selector, final LookupClassGuard guard, final boolean canPrimFail) {
            final ClassObject receiverClass = guard.getSqueakClassInternal(null);
            return create(selector, receiverClass, canPrimFail);
        }

        @NeverDefault
        public static final DispatchDirect2Node create(final CompiledCodeObject method, final LookupClassGuard guard) {
            final ClassObject receiverClass = guard.getSqueakClassInternal(null);
            final Assumption[] assumptions = DispatchUtils.createAssumptions(receiverClass, method);
            return create(assumptions, method);
        }

        @NeverDefault
        public static final DispatchDirect2Node create(final NativeObject selector, final ClassObject lookupClass, final boolean canPrimFail) {
            final Object lookupResult = lookupClass.lookupInMethodDictSlow(selector);
            final Assumption[] assumptions = DispatchUtils.createAssumptions(lookupClass, lookupResult);
            if (lookupResult instanceof final CompiledCodeObject lookupMethod) {
                if (lookupMethod.getNumArgs() == 2) {
                    return create(assumptions, lookupMethod);
                } else {
                    // argument count mismatch
                    if (canPrimFail) {
                        return new DispatchDirectPrimitiveBadArguments2Node(assumptions);
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
                    return new DispatchDirectObjectAsMethod2Node(assumptions, selector, runWithInMethod, lookupResult);
                } else {
                    assert runWithInLookupResult == null : "runWithInLookupResult should not be another Object";
                    return createMessageFallbackNode(selector, assumptions, lookupResultClass);
                }
            }
        }

        private static DispatchDirect2Node create(final Assumption[] assumptions, final CompiledCodeObject method) {
            assert checkArgumentCount(method, 2);
            if (method.hasPrimitive()) {
                final AbstractPrimitiveNode primitiveNode = PrimitiveNodeFactory.getOrCreateIndexedOrNamed(method);
                if (primitiveNode instanceof final Primitive2 primitive2) {
                    return new DispatchDirectPrimitive2Node(assumptions, method, primitive2);
                }
                DispatchUtils.logMissingPrimitive(primitiveNode, method);
            }
            return new DispatchDirectMethod2Node(assumptions, method);
        }

        private static DispatchDirect2Node createMessageFallbackNode(final NativeObject selector, final Assumption[] assumptions, final ClassObject receiverClass) {
            final ClassObject.DispatchFailureResult result = receiverClass.resolveDispatchFailure(selector, 2);
            final Assumption[] finalAssumptions = DispatchUtils.getAssumptionsForMessageFallback(assumptions, selector, result.fallbackMethod());

            return switch (result.convention()) {
                case SHORTCUT_DNU -> new DispatchDirectShortcutFallback2Node(finalAssumptions, selector, result.fallbackMethod());
                case STANDARD_DNU -> new DispatchDirectDNUFallback2Node(finalAssumptions, selector, result.fallbackMethod());
                case CANNOT_INTERPRET -> new DispatchDirectCannotInterpretFallback2Node(finalAssumptions, selector, result.fallbackMethod(), result.fallbackDepth(), result.fallbackSelector());
            };
        }
    }

    static final class DispatchDirectPrimitive2Node extends DispatchDirect2Node {
        @Child private Primitive2 primitiveNode;
        @Child private DispatchDirectPrimitiveFallback2Node dispatchFallbackNode;

        DispatchDirectPrimitive2Node(final Assumption[] assumptions, final CompiledCodeObject method, final Primitive2 primitiveNode) {
            super(assumptions);
            this.primitiveNode = primitiveNode;
            dispatchFallbackNode = DispatchDirectPrimitiveFallback2NodeGen.create(method);
        }

        @Override
        public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2) {
            try {
                return primitiveNode.execute(frame, receiver, arg1, arg2);
            } catch (final PrimitiveFailed pf) {
                DispatchUtils.logPrimitiveFailed(primitiveNode);
                return dispatchFallbackNode.execute(frame, receiver, arg1, arg2, pf);
            }
        }
    }

    static final class DispatchDirectPrimitiveBadArguments2Node extends DispatchDirect2Node {
        DispatchDirectPrimitiveBadArguments2Node(final Assumption[] assumptions) {
            super(assumptions);
        }

        @Override
        public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2) {
            CompilerDirectives.transferToInterpreter();
            throw PrimitiveFailed.BAD_NUMBER_OF_ARGUMENTS;
        }
    }

    abstract static class DispatchDirectPrimitiveFallback2Node extends AbstractNode {
        protected final CompiledCodeObject method;

        DispatchDirectPrimitiveFallback2Node(final CompiledCodeObject method) {
            this.method = method;
        }

        protected abstract Object execute(VirtualFrame frame, Object receiver, Object arg1, Object arg2, PrimitiveFailed pf);

        @Specialization
        protected static final Object doFallback(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final PrimitiveFailed pf,
                        @Bind final Node node,
                        @Cached("create(method)") final HandlePrimitiveFailedNode handlePrimitiveFailedNode,
                        @Cached(inline = true) final GetOrCreateContextWithoutFrameNode senderNode,
                        @Cached("create(method.getCallTarget())") final DirectCallNode callNode) {
            handlePrimitiveFailedNode.execute(pf);
            return callNode.call(FrameAccess.newWith(senderNode.execute(frame, node), null, receiver, arg1, arg2));
        }
    }

    abstract static class DispatchDirectWithSender2Node extends DispatchDirect2Node {
        @Child protected GetOrCreateContextWithoutFrameNode senderNode = GetOrCreateContextWithoutFrameNode.create();

        DispatchDirectWithSender2Node(final Assumption[] assumptions) {
            super(assumptions);
        }
    }

    static final class DispatchDirectMethod2Node extends DispatchDirectWithSender2Node {
        @Child private DirectCallNode callNode;

        DispatchDirectMethod2Node(final Assumption[] assumptions, final CompiledCodeObject method) {
            super(assumptions);
            callNode = DirectCallNode.create(method.getCallTarget());
        }

        @Override
        public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2) {
            return callNode.call(FrameAccess.newWith(senderNode.execute(frame), null, receiver, arg1, arg2));
        }
    }

    abstract static class AbstractDispatchDirectFallback2Node extends DispatchDirectWithSender2Node {
        protected final NativeObject selector;
        @Child protected DirectCallNode callNode;

        AbstractDispatchDirectFallback2Node(final Assumption[] assumptions, final NativeObject selector, final CompiledCodeObject targetMethod) {
            super(assumptions);
            this.selector = selector;
            this.callNode = DirectCallNode.create(targetMethod.getCallTarget());
        }

        @Override
        public abstract Object execute(VirtualFrame frame, Object receiver, Object arg1, Object arg2);
    }

    static final class DispatchDirectDNUFallback2Node extends AbstractDispatchDirectFallback2Node {
        @Child private CreateMessageNode createMessageNode = CreateMessageNodeGen.create();

        DispatchDirectDNUFallback2Node(final Assumption[] assumptions, final NativeObject selector, final CompiledCodeObject dnuMethod) {
            super(assumptions, selector, dnuMethod);
        }

        @Override
        public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2) {
            final PointersObject message = createMessageNode.execute(selector, receiver, new Object[]{arg1, arg2});
            return callNode.call(FrameAccess.newMessageFallbackWith(senderNode.execute(frame), receiver, message));
        }
    }

    static final class DispatchDirectCannotInterpretFallback2Node extends AbstractDispatchDirectFallback2Node {
        private final int fallbackDepth;
        private final NativeObject ciSelector;
        @Child private CreateMessageNode createMessageNode = CreateMessageNodeGen.create();

        DispatchDirectCannotInterpretFallback2Node(final Assumption[] assumptions, final NativeObject selector, final CompiledCodeObject ciMethod, final int fallbackDepth,
                        final NativeObject ciSelector) {
            super(assumptions, selector, ciMethod);
            this.fallbackDepth = fallbackDepth;
            this.ciSelector = ciSelector;
        }

        @Override
        public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2) {
            final PointersObject message = DispatchUtils.buildNestedMessage(
                            createMessageNode,
                            selector,
                            ciSelector,
                            receiver,
                            new Object[]{arg1, arg2},
                            fallbackDepth);
            return callNode.call(FrameAccess.newMessageFallbackWith(senderNode.execute(frame), receiver, message));
        }
    }

    static final class DispatchDirectShortcutFallback2Node extends AbstractDispatchDirectFallback2Node {

        DispatchDirectShortcutFallback2Node(final Assumption[] assumptions, final NativeObject selector, final CompiledCodeObject shortcutMethod) {
            super(assumptions, selector, shortcutMethod);
        }

        @Override
        public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2) {
            return callNode.call(FrameAccess.newWith(senderNode.execute(frame), null, receiver, arg1, arg2, selector));
        }
    }

    static final class DispatchDirectObjectAsMethod2Node extends DispatchDirectWithSender2Node {
        private final NativeObject selector;
        private final Object targetObject;
        @Child private DirectCallNode callNode;

        DispatchDirectObjectAsMethod2Node(final Assumption[] assumptions, final NativeObject selector, final CompiledCodeObject runWithInMethod, final Object targetObject) {
            super(assumptions);
            this.selector = selector;
            callNode = DirectCallNode.create(runWithInMethod.getCallTarget());
            this.targetObject = targetObject;
        }

        @Override
        public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2) {
            return callNode.call(FrameAccess.newOAMWith(senderNode.execute(frame), targetObject, selector, getContext().asArrayOfObjects(arg1, arg2), receiver));
        }
    }

    @GenerateInline(false)
    public abstract static class DispatchIndirect2Node extends AbstractNode {
        public abstract Object execute(VirtualFrame frame, boolean canPrimFail, NativeObject selector, Object receiver, Object arg1, Object arg2);

        @Specialization
        protected static final Object doIndirect(final VirtualFrame frame, final boolean canPrimFail, final NativeObject selector, final Object receiver, final Object arg1, final Object arg2,
                        @Bind final Node node,
                        @Cached(inline = true) final SqueakObjectClassNode classNode,
                        @Cached final ResolveMethodNode methodNode,
                        @Cached final TryPrimitive2Node tryPrimitiveNode,
                        @Cached(inline = true) final GetOrCreateContextWithoutFrameNode senderNode,
                        @Cached final CreateFrameArgumentsForIndirectCall2Node argumentsNode,
                        @Cached final IndirectCallNode callNode) {
            CompilerAsserts.partialEvaluationConstant(canPrimFail);
            final ClassObject receiverClass = classNode.executeLookup(node, receiver);
            final Object lookupResult = getContext(node).lookup(receiverClass, selector);
            final CompiledCodeObject method = methodNode.execute(node, getContext(node), 2, canPrimFail, selector, receiverClass, lookupResult);

            final Object result;
            if (lookupResult instanceof CompiledCodeObject) {
                result = tryPrimitiveNode.execute(frame, method, receiver, arg1, arg2);
            } else {
                result = null;
            }

            if (result != null) {
                return result;
            } else {
                return callNode.call(method.getCallTarget(), argumentsNode.execute(node, senderNode.execute(frame, node), receiver, arg1, arg2, receiverClass, lookupResult, selector));
            }
        }

        @GenerateInline(false)
        @ImportStatic(PrimitiveNodeFactory.class)
        protected abstract static class TryPrimitive2Node extends AbstractNode {
            abstract Object execute(VirtualFrame frame, CompiledCodeObject method, Object receiver, Object arg1, Object arg2);

            @SuppressWarnings("unused")
            @Specialization(guards = "method.getPrimitiveNodeOrNull() == null")
            protected static final Object doNoPrimitive(final CompiledCodeObject method, final Object receiver, final Object arg1, final Object arg2) {
                return null;
            }

            @Specialization(guards = {"method == cachedMethod", "primitiveNode != null"}, limit = "INDIRECT_PRIMITIVE_CACHE_LIMIT")
            protected static final Object doCached(final VirtualFrame frame, @SuppressWarnings("unused") final CompiledCodeObject method, final Object receiver, final Object arg1, final Object arg2,
                            @Bind final Node node,
                            @SuppressWarnings("unused") @Cached("method") final CompiledCodeObject cachedMethod,
                            @Cached("getOrCreateIndexedOrNamed(cachedMethod)") final AbstractPrimitiveNode primitiveNode,
                            @Cached final InlinedBranchProfile primitiveFailedProfile) {
                try {
                    return ((Primitive2) primitiveNode).execute(frame, receiver, arg1, arg2);
                } catch (final PrimitiveFailed pf) {
                    primitiveFailedProfile.enter(node);
                    DispatchUtils.handlePrimitiveFailedIndirect(node, method, pf);
                    return null;
                }
            }

            @Specialization(replaces = {"doNoPrimitive", "doCached"})
            protected static final Object doUncached(final VirtualFrame frame, final CompiledCodeObject method, final Object receiver, final Object arg1, final Object arg2,
                            @Bind final Node node,
                            @Cached final InlinedConditionProfile needsFrameProfile) {
                final DispatchPrimitiveNode primitiveNode = method.getPrimitiveNodeOrNull();
                if (primitiveNode != null) {
                    final MaterializedFrame frameOrNull = needsFrameProfile.profile(node, primitiveNode.needsFrame()) ? frame.materialize() : null;
                    return tryPrimitive(primitiveNode, frameOrNull, node, method, receiver, arg1, arg2);
                } else {
                    return null;
                }
            }

            @TruffleBoundary
            private static Object tryPrimitive(final DispatchPrimitiveNode primitiveNode, final MaterializedFrame frame, final Node node, final CompiledCodeObject method, final Object receiver,
                            final Object arg1, final Object arg2) {
                try {
                    return ((DispatchPrimitiveNode.DispatchPrimitive2Node) primitiveNode).execute(frame, receiver, arg1, arg2);
                } catch (final PrimitiveFailed pf) {
                    DispatchUtils.handlePrimitiveFailedIndirect(node, method, pf);
                    return null;
                }
            }
        }

        @GenerateInline
        @GenerateCached(false)
        protected abstract static class CreateFrameArgumentsForIndirectCall2Node extends AbstractNode {
            abstract Object[] execute(Node node, AbstractSqueakObject sender, Object receiver, Object arg1, Object arg2, ClassObject receiverClass, Object lookupResult, NativeObject selector);

            @Specialization
            @SuppressWarnings("unused")
            protected static final Object[] doMethod(final Node node, final AbstractSqueakObject sender, final Object receiver, final Object arg1, final Object arg2, final ClassObject receiverClass,
                            final CompiledCodeObject lookupResult, final NativeObject selector) {
                return FrameAccess.newWith(sender, null, receiver, arg1, arg2);
            }

            @Specialization(guards = "lookupResult == null")
            protected static final Object[] doMessageFallback(final Node node, final AbstractSqueakObject sender, final Object receiver, final Object arg1, final Object arg2,
                            final ClassObject receiverClass,
                            @SuppressWarnings("unused") final Object lookupResult, final NativeObject selector,
                            @Cached(inline = false) final AbstractPointersObjectWriteNode writeNode,
                            @Cached(inline = false) final CreateMessageNode createMessageNode) {
                final ClassObject.DispatchFailureResult result = getContext(node).findMethodCacheEntry(receiverClass, selector).getOrCreateDispatchFailureResult(2);
                if (result.convention() == ClassObject.FallbackConvention.SHORTCUT_DNU) {
                    return FrameAccess.newWith(sender, null, receiver, arg1, arg2, selector);
                }

                final Object[] arguments = new Object[]{arg1, arg2};
                final PointersObject message;
                if (result.convention() == ClassObject.FallbackConvention.CANNOT_INTERPRET) {
                    message = DispatchUtils.buildNestedMessage(createMessageNode, selector, result.fallbackSelector(), receiver, arguments, result.fallbackDepth());
                } else {
                    message = getContext(node).newMessage(writeNode, selector, receiverClass, arguments);
                }
                return FrameAccess.newMessageFallbackWith(sender, receiver, message);
            }

            @Specialization(guards = {"targetObject != null", "!isCompiledCodeObject(targetObject)"})
            protected static final Object[] doObjectAsMethod(final Node node, final AbstractSqueakObject sender, final Object receiver, final Object arg1, final Object arg2,
                            @SuppressWarnings("unused") final ClassObject receiverClass, final Object targetObject, final NativeObject selector) {
                final Object[] arguments = new Object[]{arg1, arg2};
                return FrameAccess.newOAMWith(sender, targetObject, selector, getContext(node).asArrayOfObjects(arguments), receiver);
            }
        }
    }
}
