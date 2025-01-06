/*
 * Copyright (c) 2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedExactClassProfile;

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
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector0NodeFactory.Dispatch0NodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector0NodeFactory.DispatchDirectPrimitiveFallback0NodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector0NodeFactory.DispatchDirectedSuper0NodeFactory.DirectedSuperDispatch0NodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector0NodeFactory.DispatchSuper0NodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class DispatchSelector0Node extends DispatchSelectorNode {
    @Child private FrameStackReadNode receiverNode;
    @Child private AbstractDispatch0Node dispatchNode;

    DispatchSelector0Node(final VirtualFrame frame, final AbstractDispatch0Node dispatchNode) {
        final int stackPointer = FrameAccess.getStackPointer(frame);
        receiverNode = FrameStackReadNode.create(frame, stackPointer - 1, true);
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

        @NeverDefault
        public static Dispatch0Node create(final NativeObject selector) {
            return Dispatch0NodeGen.create(selector);
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
                        @Bind("this") final Node node,
                        @Cached final SqueakObjectClassNode classNode,
                        @Cached final ResolveMethodNode methodNode,
                        @Cached final InlinedExactClassProfile primitiveNodeProfile,
                        @Cached final CreateFrameArgumentsForIndirectCall0Node argumentsNode,
                        @Cached final IndirectCallNode callNode) {
            final ClassObject receiverClass = classNode.executeLookup(node, receiver);
            final Object lookupResult = getContext(node).lookup(receiverClass, selector);
            final CompiledCodeObject method = methodNode.execute(node, getContext(node), receiverClass, lookupResult);
            if (method.hasPrimitive()) {
                final Primitive0 primitiveNode = (Primitive0) primitiveNodeProfile.profile(node, method.getPrimitiveNode());
                if (primitiveNode != null) {
                    try {
                        return primitiveNode.execute(frame, receiver);
                    } catch (final PrimitiveFailed pf) {
                        DispatchUtils.handlePrimitiveFailedIndirect(node, method, pf);
                    }
                }
            }
            return callNode.call(method.getCallTarget(), argumentsNode.execute(frame, node, receiver, receiverClass, lookupResult, method, selector));
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
                        @Cached("create(selector, methodClass.getSuperclassOrNull())") final DispatchDirect0Node dispatchDirectNode) {
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
            final ClassObject lookupClass = CompilerDirectives.castExact(directedClassNode.executeRead(frame), ClassObject.class).getSuperclassOrNull();
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
            final ClassObject receiverClass = guard.getSqueakClassInternal(null);
            final Object lookupResult = receiverClass.lookupInMethodDictSlow(selector);
            final Assumption[] assumptions = DispatchUtils.createAssumptions(receiverClass, lookupResult, guard.getIsValidAssumption());
            if (lookupResult == null) {
                return createDNUNode(selector, assumptions, receiverClass);
            } else if (lookupResult instanceof final CompiledCodeObject lookupMethod) {
                return create(assumptions, lookupMethod);
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

        @NeverDefault
        public static DispatchDirect0Node create(final NativeObject selector, final ClassObject lookupClass) {
            final Object lookupResult = lookupClass.lookupInMethodDictSlow(selector);
            if (lookupResult instanceof final CompiledCodeObject lookupMethod) {
                final Assumption[] assumptions = DispatchUtils.createAssumptions(lookupClass, lookupResult, null);
                return create(assumptions, lookupMethod);
            } else {
                throw SqueakException.create("superSend should resolve to method, not DNU or OAM");
            }
        }

        @NeverDefault
        public static final DispatchDirect0Node create(final CompiledCodeObject method, final LookupClassGuard guard) {
            final ClassObject receiverClass = guard.getSqueakClassInternal(null);
            final Assumption[] assumptions = DispatchUtils.createAssumptions(receiverClass, method, guard.getIsValidAssumption());
            return create(assumptions, method);
        }

        private static DispatchDirect0Node create(final Assumption[] assumptions, final CompiledCodeObject method) {
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

    abstract static class DispatchDirectPrimitiveFallback0Node extends AbstractNode {
        protected final CompiledCodeObject method;

        DispatchDirectPrimitiveFallback0Node(final CompiledCodeObject method) {
            this.method = method;
        }

        protected abstract Object execute(VirtualFrame frame, Object receiver, PrimitiveFailed pf);

        @Specialization
        protected static final Object doFallback(final VirtualFrame frame, final Object receiver, final PrimitiveFailed pf,
                        @Cached("create(method)") final HandlePrimitiveFailedNode handlePrimitiveFailedNode,
                        @Cached("create(method)") final SenderNode senderNode,
                        @Cached("create(method.getCallTarget())") final DirectCallNode callNode) {
            handlePrimitiveFailedNode.execute(pf);
            return callNode.call(FrameAccess.newWith(senderNode.execute(frame), null, receiver));
        }
    }

    abstract static class DispatchDirectWithSender0Node extends DispatchDirect0Node {
        @Child protected SenderNode senderNode;

        DispatchDirectWithSender0Node(final Assumption[] assumptions, final CompiledCodeObject method) {
            super(assumptions);
            senderNode = SenderNodeGen.create(method);
        }
    }

    static final class DispatchDirectMethod0Node extends DispatchDirectWithSender0Node {
        @Child private DirectCallNode callNode;

        DispatchDirectMethod0Node(final Assumption[] assumptions, final CompiledCodeObject method) {
            super(assumptions, method);
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
            super(assumptions, dnuMethod);
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
            super(assumptions, runWithInMethod);
            this.selector = selector;
            callNode = DirectCallNode.create(runWithInMethod.getCallTarget());
            this.targetObject = targetObject;
        }

        @Override
        public Object execute(final VirtualFrame frame, final Object receiver) {
            return callNode.call(FrameAccess.newOAMWith(senderNode.execute(frame), targetObject, selector, getContext().asArrayOfObjects(ArrayUtils.EMPTY_ARRAY), receiver));
        }
    }

    @GenerateInline
    @GenerateCached(false)
    public abstract static class CreateFrameArgumentsForIndirectCall0Node extends AbstractNode {
        public abstract Object[] execute(VirtualFrame frame, Node node, Object receiver, ClassObject receiverClass, Object lookupResult, CompiledCodeObject method, NativeObject selector);

        @Specialization
        @SuppressWarnings("unused")
        protected static final Object[] doMethod(final VirtualFrame frame, final Node node, final Object receiver, final ClassObject receiverClass,
                        final CompiledCodeObject lookupResult, final CompiledCodeObject method, final NativeObject selector,
                        @Shared("senderNode") @Cached final GetOrCreateContextOrMarkerNode senderNode) {
            return FrameAccess.newWith(senderNode.execute(frame, node, method), null, receiver);
        }

        @Specialization(guards = "lookupResult == null")
        protected static final Object[] doDoesNotUnderstand(final VirtualFrame frame, final Node node, final Object receiver, final ClassObject receiverClass,
                        @SuppressWarnings("unused") final Object lookupResult, final CompiledCodeObject method, final NativeObject selector,
                        @Cached final AbstractPointersObjectWriteNode writeNode,
                        @Shared("senderNode") @Cached final GetOrCreateContextOrMarkerNode senderNode) {
            final Object[] arguments = ArrayUtils.EMPTY_ARRAY;
            final PointersObject message = getContext(node).newMessage(writeNode, node, selector, receiverClass, arguments);
            return FrameAccess.newDNUWith(senderNode.execute(frame, node, method), receiver, message);
        }

        @Specialization(guards = {"targetObject != null", "!isCompiledCodeObject(targetObject)"})
        protected static final Object[] doObjectAsMethod(final VirtualFrame frame, final Node node, final Object receiver, @SuppressWarnings("unused") final ClassObject receiverClass,
                        final Object targetObject, final CompiledCodeObject method, final NativeObject selector,
                        @Shared("senderNode") @Cached final GetOrCreateContextOrMarkerNode senderNode) {
            final Object[] arguments = ArrayUtils.EMPTY_ARRAY;
            return FrameAccess.newOAMWith(senderNode.execute(frame, node, method), targetObject, selector, getContext(node).asArrayOfObjects(arguments), receiver);
        }
    }
}
