/*
 * Copyright (c) 2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2025 Oracle and/or its affiliates
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
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector4NodeFactory.Dispatch4NodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector4NodeFactory.DispatchDirectPrimitiveFallback4NodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector4NodeFactory.DispatchDirectedSuper4NodeFactory.DirectedSuperDispatch4NodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector4NodeFactory.DispatchSuper4NodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNode.DispatchPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive4;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class DispatchSelector4Node extends DispatchSelectorNode {
    @Child private FrameStackReadNode receiverNode;
    @Child private FrameStackReadNode arg1Node;
    @Child private FrameStackReadNode arg2Node;
    @Child private FrameStackReadNode arg3Node;
    @Child private FrameStackReadNode arg4Node;
    @Child private AbstractDispatch4Node dispatchNode;

    DispatchSelector4Node(final VirtualFrame frame, final int sp, final AbstractDispatch4Node dispatchNode) {
        receiverNode = FrameStackReadNode.create(frame, sp - 5, true);
        arg1Node = FrameStackReadNode.create(frame, sp - 4, true);
        arg2Node = FrameStackReadNode.create(frame, sp - 3, true);
        arg3Node = FrameStackReadNode.create(frame, sp - 2, true);
        arg4Node = FrameStackReadNode.create(frame, sp - 1, true);
        this.dispatchNode = dispatchNode;
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        return dispatchNode.execute(frame, receiverNode.executeRead(frame), arg1Node.executeRead(frame), arg2Node.executeRead(frame), arg3Node.executeRead(frame), arg4Node.executeRead(frame));
    }

    @Override
    public NativeObject getSelector() {
        return dispatchNode.selector;
    }

    static DispatchSelector4Node create(final VirtualFrame frame, final int sp, final NativeObject selector) {
        return new DispatchSelector4Node(frame, sp, Dispatch4NodeGen.create(selector));
    }

    static DispatchSelector4Node createSuper(final VirtualFrame frame, final int sp, final ClassObject methodClass, final NativeObject selector) {
        return new DispatchSelector4Node(frame, sp, DispatchSuper4NodeGen.create(methodClass, selector));
    }

    static DispatchSelector4Node createDirectedSuper(final VirtualFrame frame, final int sp, final NativeObject selector) {
        return new DispatchSelector4Node(frame, sp - 1, new DispatchDirectedSuper4Node(frame, selector, sp));
    }

    protected abstract static class AbstractDispatch4Node extends AbstractDispatchNode {
        AbstractDispatch4Node(final NativeObject selector) {
            super(selector);
        }

        public abstract Object execute(VirtualFrame frame, Object receiver, Object arg1, Object arg2, Object arg3, Object arg4);
    }

    public abstract static class Dispatch4Node extends AbstractDispatch4Node {
        Dispatch4Node(final NativeObject selector) {
            super(selector);
        }

        @Specialization(guards = "guard.check(receiver)", assumptions = "dispatchDirectNode.getAssumptions()", limit = "INLINE_METHOD_CACHE_LIMIT")
        protected static final Object doDirect(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4,
                        @SuppressWarnings("unused") @Cached("create(receiver)") final LookupClassGuard guard,
                        @Cached("create(selector, guard)") final DispatchDirect4Node dispatchDirectNode) {
            return dispatchDirectNode.execute(frame, receiver, arg1, arg2, arg3, arg4);
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(replaces = "doDirect")
        @SuppressWarnings("truffle-static-method")
        protected final Object doIndirect(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4,
                        @Cached final DispatchIndirect4Node dispatchNode) {
            return dispatchNode.execute(frame, false, selector, receiver, arg1, arg2, arg3, arg4);
        }
    }

    public abstract static class DispatchSuper4Node extends AbstractDispatch4Node {
        protected final ClassObject methodClass;

        DispatchSuper4Node(final ClassObject methodClass, final NativeObject selector) {
            super(selector);
            this.methodClass = methodClass;
        }

        @Specialization(assumptions = {"methodClass.getClassHierarchyAndMethodDictStable()", "dispatchDirectNode.getAssumptions()"})
        protected static final Object doCached(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4,
                        @Cached("create(selector, methodClass.getResolvedSuperclass())") final DispatchDirect4Node dispatchDirectNode) {
            return dispatchDirectNode.execute(frame, receiver, arg1, arg2, arg3, arg4);
        }
    }

    public static final class DispatchDirectedSuper4Node extends AbstractDispatch4Node {
        @Child private FrameStackReadNode directedClassNode;
        @Child private DispatchSelector4Node.DispatchDirectedSuper4Node.DirectedSuperDispatch4Node dispatchNode;

        DispatchDirectedSuper4Node(final VirtualFrame frame, final NativeObject selector, final int stackPointer) {
            super(selector);
            directedClassNode = FrameStackReadNode.create(frame, stackPointer - 1, true);
            dispatchNode = DirectedSuperDispatch4NodeGen.create(selector);
        }

        @Override
        public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4) {
            final ClassObject lookupClass = CompilerDirectives.castExact(directedClassNode.executeRead(frame), ClassObject.class).getResolvedSuperclass();
            assert lookupClass != null;
            return dispatchNode.execute(frame, lookupClass, receiver, arg1, arg2, arg3, arg4);
        }

        public abstract static class DirectedSuperDispatch4Node extends AbstractDispatchNode {
            DirectedSuperDispatch4Node(final NativeObject selector) {
                super(selector);
            }

            public abstract Object execute(VirtualFrame frame, ClassObject lookupClass, Object receiver, Object arg1, Object arg2, Object arg3, Object arg4);

            @Specialization(guards = "lookupClass == cachedLookupClass", assumptions = {"cachedLookupClass.getClassHierarchyAndMethodDictStable()",
                            "dispatchDirectNode.getAssumptions()"}, limit = "3")
            protected static final Object doCached(final VirtualFrame frame, @SuppressWarnings("unused") final ClassObject lookupClass, final Object receiver, final Object arg1, final Object arg2,
                            final Object arg3, final Object arg4,
                            @SuppressWarnings("unused") @Cached("lookupClass") final ClassObject cachedLookupClass,
                            @Cached("create(selector, cachedLookupClass)") final DispatchDirect4Node dispatchDirectNode) {
                return dispatchDirectNode.execute(frame, receiver, arg1, arg2, arg3, arg4);
            }
        }
    }

    public abstract static class DispatchDirect4Node extends AbstractDispatchDirectNode {
        DispatchDirect4Node(final Assumption[] assumptions) {
            super(assumptions);
        }

        public abstract Object execute(VirtualFrame frame, Object receiver, Object arg1, Object arg2, Object arg3, Object arg4);

        @NeverDefault
        protected static final DispatchDirect4Node create(final NativeObject selector, final LookupClassGuard guard) {
            return create(selector, guard, true);
        }

        @NeverDefault
        public static final DispatchDirect4Node create(final NativeObject selector, final LookupClassGuard guard, final boolean canPrimFail) {
            final ClassObject receiverClass = guard.getSqueakClassInternal(null);
            return create(selector, receiverClass, canPrimFail);
        }

        @NeverDefault
        public static final DispatchDirect4Node create(final NativeObject selector, final ClassObject lookupClass) {
            return create(selector, lookupClass, true);
        }

        @NeverDefault
        public static final DispatchDirect4Node create(final CompiledCodeObject method, final LookupClassGuard guard) {
            final ClassObject receiverClass = guard.getSqueakClassInternal(null);
            final Assumption[] assumptions = DispatchUtils.createAssumptions(receiverClass, method);
            return create(assumptions, method);
        }

        @NeverDefault
        public static final DispatchDirect4Node create(final NativeObject selector, final ClassObject lookupClass, final boolean canPrimFail) {
            final Object lookupResult = lookupClass.lookupInMethodDictSlow(selector);
            final Assumption[] assumptions = DispatchUtils.createAssumptions(lookupClass, lookupResult);
            if (lookupResult == null) {
                return createDNUNode(selector, assumptions, lookupClass);
            } else if (lookupResult instanceof final CompiledCodeObject lookupMethod) {
                if (lookupMethod.getNumArgs() == 4) {
                    return create(assumptions, lookupMethod);
                } else {
                    // argument count mismatch
                    if (canPrimFail) {
                        return new DispatchDirectPrimitiveBadArguments4Node(assumptions);
                    } else {
                        return create(assumptions, lookupMethod);
                    }
                }
            } else {
                final ClassObject lookupResultClass = SqueakObjectClassNode.executeUncached(lookupResult);
                final Object runWithInLookupResult = LookupMethodNode.executeUncached(lookupResultClass, SqueakImageContext.getSlow().runWithInSelector);
                if (runWithInLookupResult instanceof final CompiledCodeObject runWithInMethod) {
                    return new DispatchDirectObjectAsMethod4Node(assumptions, selector, runWithInMethod, lookupResult);
                } else {
                    assert runWithInLookupResult == null : "runWithInLookupResult should not be another Object";
                    return createDNUNode(selector, assumptions, lookupResultClass);
                }
            }
        }

        private static DispatchDirect4Node create(final Assumption[] assumptions, final CompiledCodeObject method) {
            assert checkArgumentCount(method, 4);
            if (method.hasPrimitive()) {
                final AbstractPrimitiveNode primitiveNode = PrimitiveNodeFactory.getOrCreateIndexedOrNamed(method);
                if (primitiveNode instanceof final Primitive4 primitive4) {
                    return new DispatchDirectPrimitive4Node(assumptions, method, primitive4);
                }
                DispatchUtils.logMissingPrimitive(primitiveNode, method);
            }
            return new DispatchDirectMethod4Node(assumptions, method);
        }

        private static DispatchDirectDoesNotUnderstand4Node createDNUNode(final NativeObject selector, final Assumption[] assumptions, final ClassObject receiverClass) {
            final Object dnuLookupResult = receiverClass.lookupInMethodDictSlow(SqueakImageContext.getSlow().doesNotUnderstand);
            if (dnuLookupResult instanceof final CompiledCodeObject dnuMethod) {
                return new DispatchDirectDoesNotUnderstand4Node(assumptions, selector, dnuMethod);
            } else {
                throw SqueakException.create("Unable to find DNU method in", receiverClass);
            }
        }
    }

    static final class DispatchDirectPrimitive4Node extends DispatchDirect4Node {
        @Child private Primitive4 primitiveNode;
        @Child private DispatchDirectPrimitiveFallback4Node dispatchFallbackNode;

        DispatchDirectPrimitive4Node(final Assumption[] assumptions, final CompiledCodeObject method, final Primitive4 primitiveNode) {
            super(assumptions);
            this.primitiveNode = primitiveNode;
            dispatchFallbackNode = DispatchDirectPrimitiveFallback4NodeGen.create(method);
        }

        @Override
        public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4) {
            try {
                return primitiveNode.execute(frame, receiver, arg1, arg2, arg3, arg4);
            } catch (final PrimitiveFailed pf) {
                DispatchUtils.logPrimitiveFailed(primitiveNode);
                return dispatchFallbackNode.execute(frame, receiver, arg1, arg2, arg3, arg4, pf);
            }
        }
    }

    static final class DispatchDirectPrimitiveBadArguments4Node extends DispatchDirect4Node {
        DispatchDirectPrimitiveBadArguments4Node(final Assumption[] assumptions) {
            super(assumptions);
        }

        @Override
        public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4) {
            CompilerDirectives.transferToInterpreter();
            throw PrimitiveFailed.BAD_NUMBER_OF_ARGUMENTS;
        }
    }

    abstract static class DispatchDirectPrimitiveFallback4Node extends AbstractNode {
        protected final CompiledCodeObject method;

        DispatchDirectPrimitiveFallback4Node(final CompiledCodeObject method) {
            this.method = method;
        }

        protected abstract Object execute(VirtualFrame frame, Object receiver, Object arg1, Object arg2, Object arg3, Object arg4, PrimitiveFailed pf);

        @Specialization
        protected static final Object doFallback(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4,
                        final PrimitiveFailed pf,
                        @Bind final Node node,
                        @Cached("create(method)") final HandlePrimitiveFailedNode handlePrimitiveFailedNode,
                        @Cached(inline = true) final GetOrCreateContextWithoutFrameNode senderNode,
                        @Cached("create(method.getCallTarget())") final DirectCallNode callNode) {
            handlePrimitiveFailedNode.execute(pf);
            return callNode.call(FrameAccess.newWith(senderNode.execute(frame, node), null, receiver, arg1, arg2, arg3, arg4));
        }
    }

    abstract static class DispatchDirectWithSender4Node extends DispatchDirect4Node {
        @Child protected GetOrCreateContextWithoutFrameNode senderNode = GetOrCreateContextWithoutFrameNode.create();

        DispatchDirectWithSender4Node(final Assumption[] assumptions) {
            super(assumptions);
        }
    }

    static final class DispatchDirectMethod4Node extends DispatchDirectWithSender4Node {
        @Child private DirectCallNode callNode;

        DispatchDirectMethod4Node(final Assumption[] assumptions, final CompiledCodeObject method) {
            super(assumptions);
            callNode = DirectCallNode.create(method.getCallTarget());
        }

        @Override
        public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4) {
            return callNode.call(FrameAccess.newWith(senderNode.execute(frame), null, receiver, arg1, arg2, arg3, arg4));
        }
    }

    static final class DispatchDirectDoesNotUnderstand4Node extends DispatchDirectWithSender4Node {
        private final NativeObject selector;
        @Child private DirectCallNode callNode;
        @Child private CreateDoesNotUnderstandMessageNode createDNUMessageNode = CreateDoesNotUnderstandMessageNodeGen.create();

        DispatchDirectDoesNotUnderstand4Node(final Assumption[] assumptions, final NativeObject selector, final CompiledCodeObject dnuMethod) {
            super(assumptions);
            this.selector = selector;
            callNode = DirectCallNode.create(dnuMethod.getCallTarget());
        }

        @Override
        public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4) {
            return callNode.call(FrameAccess.newDNUWith(senderNode.execute(frame), receiver, createDNUMessageNode.execute(selector, receiver, new Object[]{arg1, arg2, arg3, arg4})));
        }
    }

    static final class DispatchDirectObjectAsMethod4Node extends DispatchDirectWithSender4Node {
        private final NativeObject selector;
        private final Object targetObject;
        @Child private DirectCallNode callNode;

        DispatchDirectObjectAsMethod4Node(final Assumption[] assumptions, final NativeObject selector, final CompiledCodeObject runWithInMethod, final Object targetObject) {
            super(assumptions);
            this.selector = selector;
            callNode = DirectCallNode.create(runWithInMethod.getCallTarget());
            this.targetObject = targetObject;
        }

        @Override
        public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4) {
            return callNode.call(FrameAccess.newOAMWith(senderNode.execute(frame), targetObject, selector, getContext().asArrayOfObjects(arg1, arg2, arg3, arg4), receiver));
        }
    }

    @GenerateInline(false)
    public abstract static class DispatchIndirect4Node extends AbstractNode {
        public abstract Object execute(VirtualFrame frame, boolean canPrimFail, NativeObject selector, Object receiver, Object arg1, Object arg2, Object arg3, Object arg4);

        @Specialization
        protected static final Object doIndirect(final VirtualFrame frame, final boolean canPrimFail, final NativeObject selector, final Object receiver, final Object arg1, final Object arg2,
                        final Object arg3, final Object arg4,
                        @Bind final Node node,
                        @Cached final SqueakObjectClassNode classNode,
                        @Cached final ResolveMethodNode methodNode,
                        @Cached final TryPrimitive4Node tryPrimitiveNode,
                        @Cached final CreateFrameArgumentsForIndirectCall4Node argumentsNode,
                        @Cached final IndirectCallNode callNode) {
            CompilerAsserts.partialEvaluationConstant(canPrimFail);
            final ClassObject receiverClass = classNode.executeLookup(node, receiver);
            final Object lookupResult = getContext(node).lookup(receiverClass, selector);
            final CompiledCodeObject method = methodNode.execute(node, getContext(node), 4, canPrimFail, receiverClass, lookupResult);
            final Object result = tryPrimitiveNode.execute(frame, method, receiver, arg1, arg2, arg3, arg4);
            if (result != null) {
                return result;
            } else {
                return callNode.call(method.getCallTarget(), argumentsNode.execute(frame, node, receiver, arg1, arg2, arg3, arg4, receiverClass, lookupResult, selector));
            }
        }

        @GenerateInline(false)
        @ImportStatic(PrimitiveNodeFactory.class)
        protected abstract static class TryPrimitive4Node extends AbstractNode {
            abstract Object execute(VirtualFrame frame, CompiledCodeObject method, Object receiver, Object arg1, Object arg2, Object arg3, Object arg4);

            @SuppressWarnings("unused")
            @Specialization(guards = "method.getPrimitiveNodeOrNull() == null")
            protected static final Object doNoPrimitive(final CompiledCodeObject method, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4) {
                return null;
            }

            @Specialization(guards = {"method == cachedMethod", "primitiveNode != null"}, limit = "INDIRECT_PRIMITIVE_CACHE_LIMIT")
            protected static final Object doCached(final VirtualFrame frame, @SuppressWarnings("unused") final CompiledCodeObject method, final Object receiver, final Object arg1, final Object arg2,
                            final Object arg3, final Object arg4,
                            @Bind final Node node,
                            @SuppressWarnings("unused") @Cached("method") final CompiledCodeObject cachedMethod,
                            @Cached("getOrCreateIndexedOrNamed(cachedMethod)") final AbstractPrimitiveNode primitiveNode,
                            @Cached final InlinedBranchProfile primitiveFailedProfile) {
                try {
                    return ((Primitive4) primitiveNode).execute(frame, receiver, arg1, arg2, arg3, arg4);
                } catch (final PrimitiveFailed pf) {
                    primitiveFailedProfile.enter(node);
                    DispatchUtils.handlePrimitiveFailedIndirect(node, method, pf);
                    return null;
                }
            }

            @Specialization(replaces = {"doNoPrimitive", "doCached"})
            protected static final Object doUncached(final VirtualFrame frame, final CompiledCodeObject method, final Object receiver, final Object arg1, final Object arg2, final Object arg3,
                            final Object arg4,
                            @Bind final Node node,
                            @Cached final InlinedConditionProfile needsFrameProfile) {
                final DispatchPrimitiveNode primitiveNode = method.getPrimitiveNodeOrNull();
                if (primitiveNode != null) {
                    final MaterializedFrame frameOrNull = needsFrameProfile.profile(node, primitiveNode.needsFrame()) ? frame.materialize() : null;
                    return tryPrimitive(primitiveNode, frameOrNull, node, method, receiver, arg1, arg2, arg3, arg4);
                } else {
                    return null;
                }
            }

            @TruffleBoundary
            private static Object tryPrimitive(final DispatchPrimitiveNode primitiveNode, final MaterializedFrame frame, final Node node, final CompiledCodeObject method, final Object receiver,
                            final Object arg1, final Object arg2, final Object arg3, final Object arg4) {
                try {
                    return ((DispatchPrimitiveNode.DispatchPrimitive4Node) primitiveNode).execute(frame, receiver, arg1, arg2, arg3, arg4);
                } catch (final PrimitiveFailed pf) {
                    DispatchUtils.handlePrimitiveFailedIndirect(node, method, pf);
                    return null;
                }
            }
        }

        @GenerateInline
        @GenerateCached(false)
        protected abstract static class CreateFrameArgumentsForIndirectCall4Node extends AbstractNode {
            abstract Object[] execute(VirtualFrame frame, Node node, Object receiver, Object arg1, Object arg2, Object arg3, Object arg4, ClassObject receiverClass, Object lookupResult,
                            NativeObject selector);

            @Specialization
            @SuppressWarnings("unused")
            protected static final Object[] doMethod(final VirtualFrame frame, final Node node, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4,
                            final ClassObject receiverClass, final CompiledCodeObject lookupResult, final NativeObject selector,
                            @Shared("senderNode") @Cached final GetOrCreateContextWithoutFrameNode senderNode) {
                return FrameAccess.newWith(senderNode.execute(frame, node), null, receiver, arg1, arg2, arg3, arg4);
            }

            @Specialization(guards = "lookupResult == null")
            protected static final Object[] doDoesNotUnderstand(final VirtualFrame frame, final Node node, final Object receiver, final Object arg1, final Object arg2, final Object arg3,
                            final Object arg4, final ClassObject receiverClass, @SuppressWarnings("unused") final Object lookupResult, final NativeObject selector,
                            @Cached final AbstractPointersObjectWriteNode writeNode,
                            @Shared("senderNode") @Cached final GetOrCreateContextWithoutFrameNode senderNode) {
                final Object[] arguments = new Object[]{arg1, arg2, arg3, arg4};
                final PointersObject message = getContext(node).newMessage(writeNode, node, selector, receiverClass, arguments);
                return FrameAccess.newDNUWith(senderNode.execute(frame, node), receiver, message);
            }

            @Specialization(guards = {"targetObject != null", "!isCompiledCodeObject(targetObject)"})
            protected static final Object[] doObjectAsMethod(final VirtualFrame frame, final Node node, final Object receiver, final Object arg1, final Object arg2, final Object arg3,
                            final Object arg4,
                            @SuppressWarnings("unused") final ClassObject receiverClass, final Object targetObject, final NativeObject selector,
                            @Shared("senderNode") @Cached final GetOrCreateContextWithoutFrameNode senderNode) {
                final Object[] arguments = new Object[]{arg1, arg2, arg3, arg4};
                return FrameAccess.newOAMWith(senderNode.execute(frame, node), targetObject, selector, getContext(node).asArrayOfObjects(arguments), receiver);
            }
        }
    }
}
