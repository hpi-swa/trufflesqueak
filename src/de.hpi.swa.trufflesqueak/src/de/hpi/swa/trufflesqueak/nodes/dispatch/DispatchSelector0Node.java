/*
 * Copyright (c) 2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import static de.hpi.swa.trufflesqueak.util.UnsafeUtils.uncheckedCast;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
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
import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.LookupMethodNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetOrCreateContextWithoutFrameNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector0NodeFactory.Dispatch0NodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector0NodeFactory.DispatchDirectPrimitiveFallback0NodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector0NodeFactory.DispatchDirectedSuper0NodeFactory.DirectedSuperDispatch0NodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector0NodeFactory.DispatchSuper0NodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNode.DispatchPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class DispatchSelector0Node extends DispatchSelectorNode {
    @Child private FrameStackReadNode receiverNode;
    @Child private AbstractDispatch0Node dispatchNode;

    DispatchSelector0Node(final VirtualFrame frame, final AbstractDispatch0Node dispatchNode) {
        final int sp = FrameAccess.getStackPointer(frame);
        receiverNode = FrameStackReadNode.create(frame, sp - 1, false); // replaced by result
        this.dispatchNode = dispatchNode;
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        return dispatchNode.execute(frame, receiverNode.executeRead(frame));
    }

    @Override
    public NativeObject getSelector() {
        return dispatchNode.selector;
    }

    static DispatchSelector0Node create(final VirtualFrame frame, final NativeObject selector) {
        return new DispatchSelector0Node(frame, Dispatch0NodeGen.create(selector));
    }

    static DispatchSelector0Node createSuper(final VirtualFrame frame, final ClassObject methodClass, final NativeObject selector) {
        return new DispatchSelector0Node(frame, DispatchSuper0NodeGen.create(methodClass, selector));
    }

    static DispatchSelector0Node createDirectedSuper(final VirtualFrame frame, final NativeObject selector) {
        final int stackPointer = FrameAccess.getStackPointer(frame);
        // Trick: decrement stack pointer so that node uses the right receiver and args
        FrameAccess.setStackPointer(frame, stackPointer - 1);
        final DispatchSelector0Node result = new DispatchSelector0Node(frame, new DispatchDirectedSuper0Node(frame, selector, stackPointer));
        // Restore stack pointer
        FrameAccess.setStackPointer(frame, stackPointer);
        return result;
    }

    protected abstract static class AbstractDispatch0Node extends AbstractDispatchNode {
        AbstractDispatch0Node(final NativeObject selector) {
            super(selector);
        }

        public abstract Object execute(VirtualFrame frame, Object receiver);
    }

    public abstract static class Dispatch0Node extends AbstractDispatch0Node {
        Dispatch0Node(final NativeObject selector) {
            super(selector);
        }

        @Specialization(guards = "guard.check(receiver)", assumptions = "dispatchDirectNode.getAssumptions()", limit = "INLINE_METHOD_CACHE_LIMIT")
        protected static final Object doDirect(final VirtualFrame frame, final Object receiver,
                        @SuppressWarnings("unused") @Cached("create(receiver)") final LookupClassGuard guard,
                        @Cached("create(selector, guard)") final DispatchDirect0Node dispatchDirectNode) {
            return dispatchDirectNode.execute(frame, receiver);
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(replaces = "doDirect")
        @SuppressWarnings("truffle-static-method")
        protected final Object doIndirect(final VirtualFrame frame, final Object receiver,
                        @Cached final DispatchIndirect0Node dispatchNode) {
            return dispatchNode.execute(frame, false, selector, receiver);
        }
    }

    public abstract static class DispatchSuper0Node extends AbstractDispatch0Node {
        protected final ClassObject methodClass;

        DispatchSuper0Node(final ClassObject methodClass, final NativeObject selector) {
            super(selector);
            this.methodClass = methodClass;
        }

        @Specialization(assumptions = {"methodClass.getClassHierarchyAndMethodDictStable()", "dispatchDirectNode.getAssumptions()"})
        protected static final Object doCached(final VirtualFrame frame, final Object receiver,
                        @Cached("create(selector, methodClass.getResolvedSuperclass())") final DispatchDirect0Node dispatchDirectNode) {
            return dispatchDirectNode.execute(frame, receiver);
        }
    }

    public static final class DispatchDirectedSuper0Node extends AbstractDispatch0Node {
        @Child private FrameStackReadNode directedClassNode;
        @Child private DispatchDirectedSuper0Node.DirectedSuperDispatch0Node dispatchNode;

        DispatchDirectedSuper0Node(final VirtualFrame frame, final NativeObject selector, final int stackPointer) {
            super(selector);
            directedClassNode = FrameStackReadNode.create(frame, stackPointer - 1, true);
            dispatchNode = DirectedSuperDispatch0NodeGen.create(selector);
        }

        @Override
        public Object execute(final VirtualFrame frame, final Object receiver) {
            final ClassObject lookupClass = CompilerDirectives.castExact(directedClassNode.executeRead(frame), ClassObject.class).getResolvedSuperclass();
            assert lookupClass != null;
            return dispatchNode.execute(frame, lookupClass, receiver);
        }

        public abstract static class DirectedSuperDispatch0Node extends AbstractDispatchNode {
            DirectedSuperDispatch0Node(final NativeObject selector) {
                super(selector);
            }

            public abstract Object execute(VirtualFrame frame, ClassObject lookupClass, Object receiver);

            @Specialization(guards = "lookupClass == cachedLookupClass", assumptions = {"cachedLookupClass.getClassHierarchyAndMethodDictStable()",
                            "dispatchDirectNode.getAssumptions()"}, limit = "3")
            protected static final Object doCached(final VirtualFrame frame, @SuppressWarnings("unused") final ClassObject lookupClass, final Object receiver,
                            @SuppressWarnings("unused") @Cached("lookupClass") final ClassObject cachedLookupClass,
                            @Cached("create(selector, cachedLookupClass)") final DispatchDirect0Node dispatchDirectNode) {
                return dispatchDirectNode.execute(frame, receiver);
            }
        }
    }

    public abstract static class DispatchDirect0Node extends AbstractDispatchDirectNode {
        DispatchDirect0Node(final Assumption[] assumptions) {
            super(assumptions);
        }

        public abstract Object execute(VirtualFrame frame, Object receiver);

        @NeverDefault
        protected static final DispatchDirect0Node create(final NativeObject selector, final LookupClassGuard guard) {
            return create(selector, guard, false);
        }

        @NeverDefault
        public static final DispatchDirect0Node create(final NativeObject selector, final LookupClassGuard guard, final boolean canPrimFail) {
            final ClassObject receiverClass = guard.getSqueakClassInternal(null);
            return create(selector, receiverClass, canPrimFail);
        }

        @NeverDefault
        public static final DispatchDirect0Node create(final NativeObject selector, final ClassObject lookupClass) {
            return create(selector, lookupClass, false);
        }

        @NeverDefault
        public static final DispatchDirect0Node create(final CompiledCodeObject method, final LookupClassGuard guard) {
            final ClassObject receiverClass = guard.getSqueakClassInternal(null);
            final Assumption[] assumptions = DispatchUtils.createAssumptions(receiverClass, method);
            return create(assumptions, method);
        }

        @NeverDefault
        public static final DispatchDirect0Node create(final NativeObject selector, final ClassObject lookupClass, final boolean canPrimFail) {
            final Object lookupResult = lookupClass.lookupInMethodDictSlow(selector);
            final Assumption[] assumptions = DispatchUtils.createAssumptions(lookupClass, lookupResult);
            if (lookupResult == null) {
                return createDNUNode(selector, assumptions, lookupClass);
            } else if (lookupResult instanceof final CompiledCodeObject lookupMethod) {
                if (lookupMethod.getNumArgs() == 0) {
                    return create(assumptions, lookupMethod);
                } else {
                    // argument count mismatch
                    if (canPrimFail) {
                        return new DispatchDirectPrimitiveBadArguments0Node(assumptions);
                    } else {
                        return create(assumptions, lookupMethod);
                    }
                }
            } else {
                final ClassObject lookupResultClass = SqueakObjectClassNode.executeUncached(lookupResult);
                final Object runWithInLookupResult = LookupMethodNode.executeUncached(lookupResultClass, SqueakImageContext.getSlow().runWithInSelector);
                if (runWithInLookupResult instanceof final CompiledCodeObject runWithInMethod) {
                    return new DispatchDirectObjectAsMethod0Node(assumptions, selector, runWithInMethod, lookupResult);
                } else {
                    assert runWithInLookupResult == null : "runWithInLookupResult should not be another Object";
                    return createDNUNode(selector, assumptions, lookupResultClass);
                }
            }
        }

        private static DispatchDirect0Node create(final Assumption[] assumptions, final CompiledCodeObject method) {
            assert checkArgumentCount(method, 0);
            if (method.hasPrimitive()) {
                final AbstractPrimitiveNode primitiveNode = PrimitiveNodeFactory.getOrCreateIndexedOrNamed(method);
                if (primitiveNode instanceof final Primitive0 primitive0) {
                    return new DispatchDirectPrimitive0Node(assumptions, method, primitive0);
                }
                DispatchUtils.logMissingPrimitive(primitiveNode, method);
            }
            return new DispatchDirectMethod0Node(assumptions, method);
        }

        private static DispatchDirectDoesNotUnderstand0Node createDNUNode(final NativeObject selector, final Assumption[] assumptions, final ClassObject receiverClass) {
            final Object dnuLookupResult = receiverClass.lookupInMethodDictSlow(SqueakImageContext.getSlow().doesNotUnderstand);
            if (dnuLookupResult instanceof final CompiledCodeObject dnuMethod) {
                return new DispatchDirectDoesNotUnderstand0Node(assumptions, selector, dnuMethod);
            } else {
                throw SqueakException.create("Unable to find DNU method in", receiverClass);
            }
        }
    }

    static final class DispatchDirectPrimitive0Node extends DispatchDirect0Node {
        @Child private Primitive0 primitiveNode;
        @Child private DispatchDirectPrimitiveFallback0Node dispatchFallbackNode;

        DispatchDirectPrimitive0Node(final Assumption[] assumptions, final CompiledCodeObject method, final Primitive0 primitiveNode) {
            super(assumptions);
            this.primitiveNode = primitiveNode;
            dispatchFallbackNode = DispatchDirectPrimitiveFallback0NodeGen.create(method);
        }

        @Override
        public Object execute(final VirtualFrame frame, final Object receiver) {
            try {
                return primitiveNode.execute(frame, receiver);
            } catch (final PrimitiveFailed pf) {
                DispatchUtils.logPrimitiveFailed(primitiveNode);
                return dispatchFallbackNode.execute(frame, receiver, pf);
            }
        }
    }

    static final class DispatchDirectPrimitiveBadArguments0Node extends DispatchDirect0Node {
        DispatchDirectPrimitiveBadArguments0Node(final Assumption[] assumptions) {
            super(assumptions);
        }

        @Override
        public Object execute(final VirtualFrame frame, final Object receiver) {
            CompilerDirectives.transferToInterpreter();
            throw PrimitiveFailed.BAD_NUMBER_OF_ARGUMENTS;
        }
    }

    abstract static class DispatchDirectPrimitiveFallback0Node extends AbstractNode {
        protected final CompiledCodeObject method;

        DispatchDirectPrimitiveFallback0Node(final CompiledCodeObject method) {
            this.method = method;
        }

        protected abstract Object execute(VirtualFrame frame, Object receiver, PrimitiveFailed pf);

        @Specialization
        protected static final Object doFallback(final VirtualFrame frame, final Object receiver, final PrimitiveFailed pf,
                        @Bind final Node node,
                        @Cached("create(method)") final HandlePrimitiveFailedNode handlePrimitiveFailedNode,
                        @Cached(inline = true) final GetOrCreateContextWithoutFrameNode senderNode,
                        @Cached("create(method.getCallTarget())") final DirectCallNode callNode) {
            handlePrimitiveFailedNode.execute(pf);
            return callNode.call(FrameAccess.newWith(senderNode.execute(frame, node), null, receiver));
        }
    }

    abstract static class DispatchDirectWithSender0Node extends DispatchDirect0Node {
        @Child protected GetOrCreateContextWithoutFrameNode senderNode = GetOrCreateContextWithoutFrameNode.create();

        DispatchDirectWithSender0Node(final Assumption[] assumptions) {
            super(assumptions);
        }
    }

    static final class DispatchDirectMethod0Node extends DispatchDirectWithSender0Node {
        @Child private DirectCallNode callNode;

        DispatchDirectMethod0Node(final Assumption[] assumptions, final CompiledCodeObject method) {
            super(assumptions);
            callNode = DirectCallNode.create(method.getCallTarget());
        }

        @Override
        public Object execute(final VirtualFrame frame, final Object receiver) {
            return callNode.call(FrameAccess.newWith(senderNode.execute(frame), null, receiver));
        }
    }

    static final class DispatchDirectDoesNotUnderstand0Node extends DispatchDirectWithSender0Node {
        private final NativeObject selector;
        @Child private DirectCallNode callNode;
        @Child private CreateDoesNotUnderstandMessageNode createDNUMessageNode = CreateDoesNotUnderstandMessageNodeGen.create();

        DispatchDirectDoesNotUnderstand0Node(final Assumption[] assumptions, final NativeObject selector, final CompiledCodeObject dnuMethod) {
            super(assumptions);
            this.selector = selector;
            callNode = DirectCallNode.create(dnuMethod.getCallTarget());
        }

        @Override
        public Object execute(final VirtualFrame frame, final Object receiver) {
            return callNode.call(FrameAccess.newDNUWith(senderNode.execute(frame), receiver, createDNUMessageNode.execute(selector, receiver, ArrayUtils.EMPTY_ARRAY)));
        }
    }

    static final class DispatchDirectObjectAsMethod0Node extends DispatchDirectWithSender0Node {
        private final NativeObject selector;
        private final Object targetObject;
        @Child private DirectCallNode callNode;

        DispatchDirectObjectAsMethod0Node(final Assumption[] assumptions, final NativeObject selector, final CompiledCodeObject runWithInMethod, final Object targetObject) {
            super(assumptions);
            this.selector = selector;
            callNode = DirectCallNode.create(runWithInMethod.getCallTarget());
            this.targetObject = targetObject;
        }

        @Override
        public Object execute(final VirtualFrame frame, final Object receiver) {
            return callNode.call(FrameAccess.newOAMWith(senderNode.execute(frame), targetObject, selector, getContext().asArrayOfObjects(ArrayUtils.EMPTY_ARRAY), receiver));
        }
    }

    @GenerateInline(false)
    public abstract static class DispatchIndirect0Node extends AbstractNode {
        public abstract Object execute(VirtualFrame frame, boolean canPrimFail, NativeObject selector, Object receiver);

        @Specialization
        protected static final Object doIndirect(final VirtualFrame frame, final boolean canPrimFail, final NativeObject selector, final Object receiver,
                        @Bind final Node node,
                        @Cached(inline = true) final SqueakObjectClassNode classNode,
                        @Cached final ResolveMethodNode methodNode,
                        @Cached final TryPrimitive0Node tryPrimitiveNode,
                        @Cached final CreateFrameArgumentsForIndirectCall0Node argumentsNode,
                        @Cached final IndirectCallNode callNode) {
            CompilerAsserts.partialEvaluationConstant(canPrimFail);
            final ClassObject receiverClass = classNode.executeLookup(node, receiver);
            final Object lookupResult = getContext(node).lookup(receiverClass, selector);
            final CompiledCodeObject method = methodNode.execute(node, getContext(node), 0, canPrimFail, receiverClass, lookupResult);
            final Object result = tryPrimitiveNode.execute(frame, method, receiver);
            if (result != null) {
                return result;
            } else {
                return callNode.call(method.getCallTarget(), argumentsNode.execute(frame, node, receiver, receiverClass, lookupResult, selector));
            }
        }

        @GenerateInline(false)
        @ImportStatic(PrimitiveNodeFactory.class)
        protected abstract static class TryPrimitive0Node extends AbstractNode {
            abstract Object execute(VirtualFrame frame, CompiledCodeObject method, Object receiver);

            @SuppressWarnings("unused")
            @Specialization(guards = "method.getPrimitiveNodeOrNull() == null")
            protected static final Object doNoPrimitive(final CompiledCodeObject method, final Object receiver) {
                return null;
            }

            @Specialization(guards = {"method == cachedMethod", "primitiveNode != null"}, limit = "INDIRECT_PRIMITIVE_CACHE_LIMIT")
            protected static final Object doCached(final VirtualFrame frame, @SuppressWarnings("unused") final CompiledCodeObject method, final Object receiver,
                            @Bind final Node node,
                            @SuppressWarnings("unused") @Cached("method") final CompiledCodeObject cachedMethod,
                            @Cached("getOrCreateIndexedOrNamed(cachedMethod)") final AbstractPrimitiveNode primitiveNode,
                            @Cached final InlinedBranchProfile primitiveFailedProfile) {
                try {
                    return uncheckedCast(primitiveNode, Primitive0.class).execute(frame, receiver);
                } catch (final PrimitiveFailed pf) {
                    primitiveFailedProfile.enter(node);
                    DispatchUtils.handlePrimitiveFailedIndirect(node, method, pf);
                    return null;
                }
            }

            @Specialization(replaces = {"doNoPrimitive", "doCached"})
            protected static final Object doUncached(final VirtualFrame frame, final CompiledCodeObject method, final Object receiver,
                            @Bind final Node node,
                            @Cached final InlinedConditionProfile needsFrameProfile) {
                final DispatchPrimitiveNode primitiveNode = method.getPrimitiveNodeOrNull();
                if (primitiveNode != null) {
                    final MaterializedFrame frameOrNull = needsFrameProfile.profile(node, primitiveNode.needsFrame()) ? frame.materialize() : null;
                    return tryPrimitive(primitiveNode, frameOrNull, node, method, receiver);
                } else {
                    return null;
                }
            }

            @TruffleBoundary
            private static Object tryPrimitive(final DispatchPrimitiveNode primitiveNode, final MaterializedFrame frame, final Node node, final CompiledCodeObject method, final Object receiver) {
                try {
                    return uncheckedCast(primitiveNode, DispatchPrimitiveNode.DispatchPrimitive0Node.class).execute(frame, receiver);
                } catch (final PrimitiveFailed pf) {
                    DispatchUtils.handlePrimitiveFailedIndirect(node, method, pf);
                    return null;
                }
            }
        }

        @GenerateInline
        @GenerateCached(false)
        protected abstract static class CreateFrameArgumentsForIndirectCall0Node extends AbstractNode {
            abstract Object[] execute(VirtualFrame frame, Node node, Object receiver, ClassObject receiverClass, Object lookupResult, NativeObject selector);

            @Specialization
            @SuppressWarnings("unused")
            protected static final Object[] doMethod(final VirtualFrame frame, final Node node, final Object receiver, final ClassObject receiverClass,
                            final CompiledCodeObject lookupResult, final NativeObject selector,
                            @Shared("senderNode") @Cached final GetOrCreateContextWithoutFrameNode senderNode) {
                return FrameAccess.newWith(senderNode.execute(frame, node), null, receiver);
            }

            @Specialization(guards = "lookupResult == null")
            protected static final Object[] doDoesNotUnderstand(final VirtualFrame frame, final Node node, final Object receiver, final ClassObject receiverClass,
                            @SuppressWarnings("unused") final Object lookupResult, final NativeObject selector,
                            @Cached final AbstractPointersObjectWriteNode writeNode,
                            @Shared("senderNode") @Cached final GetOrCreateContextWithoutFrameNode senderNode) {
                final Object[] arguments = ArrayUtils.EMPTY_ARRAY;
                final PointersObject message = getContext(node).newMessage(writeNode, node, selector, receiverClass, arguments);
                return FrameAccess.newDNUWith(senderNode.execute(frame, node), receiver, message);
            }

            @Specialization(guards = {"targetObject != null", "!isCompiledCodeObject(targetObject)"})
            protected static final Object[] doObjectAsMethod(final VirtualFrame frame, final Node node, final Object receiver, @SuppressWarnings("unused") final ClassObject receiverClass,
                            final Object targetObject, final NativeObject selector,
                            @Shared("senderNode") @Cached final GetOrCreateContextWithoutFrameNode senderNode) {
                final Object[] arguments = ArrayUtils.EMPTY_ARRAY;
                return FrameAccess.newOAMWith(senderNode.execute(frame, node), targetObject, selector, getContext(node).asArrayOfObjects(arguments), receiver);
            }
        }
    }
}
