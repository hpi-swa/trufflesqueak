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
import com.oracle.truffle.api.nodes.ExplodeLoop;
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
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNodeFactory.DispatchDirectPrimitiveFallbackNaryNodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNodeFactory.DispatchDirectedSuperNaryNodeFactory.DirectedSuperDispatchNaryNodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNodeFactory.DispatchNaryNodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNodeFactory.DispatchSuperNaryNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive10;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive11;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive2;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive3;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive4;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive5;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive6;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive7;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive8;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive9;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class DispatchSelectorNaryNode extends DispatchSelectorNode {
    @Child private FrameStackReadNode receiverNode;
    @Children private FrameStackReadNode[] argumentNodes;
    @Child private AbstractDispatchNaryNode dispatchNode;

    DispatchSelectorNaryNode(final VirtualFrame frame, final int numArgs, final AbstractDispatchNaryNode dispatchNode) {
        final int stackPointer = FrameAccess.getStackPointer(frame);
        final int receiverIndex = stackPointer - 1 - numArgs;
        receiverNode = FrameStackReadNode.create(frame, receiverIndex, true);
        argumentNodes = new FrameStackReadNode[numArgs];
        for (int i = 0; i < numArgs; i++) {
            argumentNodes[i] = FrameStackReadNode.create(frame, receiverIndex + 1 + i, true);
        }
        this.dispatchNode = dispatchNode;
    }

    @Override
    @ExplodeLoop
    public Object execute(final VirtualFrame frame) {
        final Object receiver = receiverNode.executeRead(frame);
        final Object[] arguments = new Object[argumentNodes.length];
        for (int i = 0; i < argumentNodes.length; i++) {
            arguments[i] = argumentNodes[i].executeRead(frame);
        }
        return dispatchNode.execute(frame, receiver, arguments);
    }

    @Override
    public NativeObject getSelector() {
        return dispatchNode.selector;
    }

    protected static DispatchSelectorNaryNode create(final VirtualFrame frame, final int numArgs, final NativeObject selector) {
        return new DispatchSelectorNaryNode(frame, numArgs, DispatchNaryNodeGen.create(selector));
    }

    protected static DispatchSelectorNaryNode createSuper(final VirtualFrame frame, final int numArgs, final ClassObject methodClass, final NativeObject selector) {
        return new DispatchSelectorNaryNode(frame, numArgs, DispatchSuperNaryNodeGen.create(methodClass, selector));
    }

    protected static DispatchSelectorNaryNode createDirectedSuper(final VirtualFrame frame, final int numArgs, final NativeObject selector) {
        final int stackPointer = FrameAccess.getStackPointer(frame);
        // Trick: decrement stack pointer so that node uses the right receiver and args
        FrameAccess.setStackPointer(frame, stackPointer - 1);
        final DispatchSelectorNaryNode result = new DispatchSelectorNaryNode(frame, numArgs, new DispatchDirectedSuperNaryNode(frame, selector, stackPointer));
        // Restore stack pointer
        FrameAccess.setStackPointer(frame, stackPointer);
        return result;
    }

    protected abstract static class AbstractDispatchNaryNode extends AbstractDispatchNode {
        AbstractDispatchNaryNode(final NativeObject selector) {
            super(selector);
        }

        public abstract Object execute(VirtualFrame frame, Object receiver, Object[] arguments);
    }

    public abstract static class DispatchNaryNode extends AbstractDispatchNaryNode {
        DispatchNaryNode(final NativeObject selector) {
            super(selector);
        }

        @Specialization(guards = "guard.check(receiver)", assumptions = "dispatchDirectNode.getAssumptions()", limit = "INLINE_METHOD_CACHE_LIMIT")
        protected static final Object doDirect(final VirtualFrame frame, final Object receiver, final Object[] arguments,
                        @SuppressWarnings("unused") @Cached("create(receiver)") final LookupClassGuard guard,
                        @Cached("create(selector, guard)") final DispatchDirectNaryNode dispatchDirectNode) {
            return dispatchDirectNode.execute(frame, receiver, arguments);
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(replaces = "doDirect")
        @SuppressWarnings("truffle-static-method")
        protected final Object doIndirect(final VirtualFrame frame, final Object receiver, final Object[] arguments,
                        @Bind("this") final Node node,
                        @Cached final SqueakObjectClassNode classNode,
                        @Cached final ResolveMethodNode methodNode,
                        @Cached final InlinedExactClassProfile primitiveNodeProfile,
                        @Cached final CreateFrameArgumentsForIndirectCallNaryNode argumentsNode,
                        @Cached final IndirectCallNode callNode) {
            final ClassObject receiverClass = classNode.executeLookup(node, receiver);
            final Object lookupResult = getContext(node).lookup(receiverClass, selector);
            final CompiledCodeObject method = methodNode.execute(node, getContext(node), receiverClass, lookupResult);
            if (method.hasPrimitive()) {
                final AbstractPrimitiveNode primitiveNode = primitiveNodeProfile.profile(node, method.getPrimitiveNode());
                if (primitiveNode != null) {
                    try {
                        return primitiveNode.executeWithArguments(frame, receiver, arguments);
                    } catch (final PrimitiveFailed pf) {
                        DispatchUtils.handlePrimitiveFailedIndirect(node, method, pf);
                    }
                }
            }
            return callNode.call(method.getCallTarget(), argumentsNode.execute(frame, node, receiver, arguments, receiverClass, lookupResult, method, selector));
        }
    }

    public abstract static class DispatchSuperNaryNode extends AbstractDispatchNaryNode {
        protected final ClassObject methodClass;

        DispatchSuperNaryNode(final ClassObject methodClass, final NativeObject selector) {
            super(selector);
            this.methodClass = methodClass;
        }

        @Specialization(assumptions = {"methodClass.getClassHierarchyAndMethodDictStable()", "dispatchDirectNode.getAssumptions()"})
        protected static final Object doCached(final VirtualFrame frame, final Object receiver, final Object[] arguments,
                        @Cached("create(selector, methodClass.getSuperclassOrNull())") final DispatchDirectNaryNode dispatchDirectNode) {
            return dispatchDirectNode.execute(frame, receiver, arguments);
        }
    }

    public static final class DispatchDirectedSuperNaryNode extends AbstractDispatchNaryNode {
        @Child private FrameStackReadNode directedClassNode;
        @Child private DispatchDirectedSuperNaryNode.DirectedSuperDispatchNaryNode dispatchNode;

        DispatchDirectedSuperNaryNode(final VirtualFrame frame, final NativeObject selector, final int stackPointer) {
            super(selector);
            directedClassNode = FrameStackReadNode.create(frame, stackPointer - 1, true);
            dispatchNode = DirectedSuperDispatchNaryNodeGen.create(selector);
        }

        @Override
        public Object execute(final VirtualFrame frame, final Object receiver, final Object[] arguments) {
            final ClassObject lookupClass = CompilerDirectives.castExact(directedClassNode.executeRead(frame), ClassObject.class).getSuperclassOrNull();
            assert lookupClass != null;
            return dispatchNode.execute(frame, lookupClass, receiver, arguments);
        }

        public abstract static class DirectedSuperDispatchNaryNode extends AbstractDispatchNode {
            DirectedSuperDispatchNaryNode(final NativeObject selector) {
                super(selector);
            }

            public abstract Object execute(VirtualFrame frame, ClassObject lookupClass, Object receiver, Object[] arguments);

            @Specialization(guards = "lookupClass == cachedLookupClass", assumptions = {"cachedLookupClass.getClassHierarchyAndMethodDictStable()",
                            "dispatchDirectNode.getAssumptions()"}, limit = "3")
            protected static final Object doCached(final VirtualFrame frame, @SuppressWarnings("unused") final ClassObject lookupClass, final Object receiver, final Object[] arguments,
                            @SuppressWarnings("unused") @Cached("lookupClass") final ClassObject cachedLookupClass,
                            @Cached("create(selector, cachedLookupClass)") final DispatchDirectNaryNode dispatchDirectNode) {
                return dispatchDirectNode.execute(frame, receiver, arguments);
            }
        }
    }

    public abstract static class DispatchDirectNaryNode extends AbstractDispatchDirectNode {
        DispatchDirectNaryNode(final Assumption[] assumptions) {
            super(assumptions);
        }

        public abstract Object execute(VirtualFrame frame, Object receiver, Object[] arguments);

        @NeverDefault
        protected static final DispatchDirectNaryNode create(final NativeObject selector, final LookupClassGuard guard) {
            final ClassObject receiverClass = guard.getSqueakClassInternal(null);
            return create(selector, receiverClass, guard.getIsValidAssumption());
        }

        @NeverDefault
        public static DispatchDirectNaryNode create(final NativeObject selector, final ClassObject lookupClass) {
            return create(selector, lookupClass, null);
        }

        @NeverDefault
        public static final DispatchDirectNaryNode create(final CompiledCodeObject method, final LookupClassGuard guard) {
            final ClassObject receiverClass = guard.getSqueakClassInternal(null);
            final Assumption[] assumptions = DispatchUtils.createAssumptions(receiverClass, method, guard.getIsValidAssumption());
            return create(assumptions, method);
        }

        private static DispatchDirectNaryNode create(final NativeObject selector, final ClassObject lookupClass, final Assumption guardAssumptionOrNull) {
            final Object lookupResult = lookupClass.lookupInMethodDictSlow(selector);
            final Assumption[] assumptions = DispatchUtils.createAssumptions(lookupClass, lookupResult, guardAssumptionOrNull);
            if (lookupResult == null) {
                return createDNUNode(selector, assumptions, lookupClass);
            } else if (lookupResult instanceof final CompiledCodeObject lookupMethod) {
                return create(assumptions, lookupMethod);
            } else {
                final ClassObject lookupResultClass = SqueakObjectClassNode.executeUncached(lookupResult);
                final Object runWithInLookupResult = LookupMethodNode.executeUncached(lookupResultClass, SqueakImageContext.getSlow().runWithInSelector);
                if (runWithInLookupResult instanceof final CompiledCodeObject runWithInMethod) {
                    return new DispatchDirectObjectAsMethodNaryNode(assumptions, selector, runWithInMethod, lookupResult);
                } else {
                    assert runWithInLookupResult == null : "runWithInLookupResult should not be another Object";
                    return createDNUNode(selector, assumptions, lookupResultClass);
                }
            }
        }

        private static DispatchDirectNaryNode create(final Assumption[] assumptions, final CompiledCodeObject method) {
            if (method.hasPrimitive()) {
                final AbstractPrimitiveNode primitiveNode = PrimitiveNodeFactory.getOrCreateIndexedOrNamed(method);
                if (primitiveNode != null) {
                    return new DispatchDirectPrimitiveNaryNode(assumptions, method, primitiveNode);
                }
                DispatchUtils.logMissingPrimitive(primitiveNode, method);
            }
            return new DispatchDirectMethodNaryNode(assumptions, method);
        }

        private static DispatchDirectDoesNotUnderstandNaryNode createDNUNode(final NativeObject selector, final Assumption[] assumptions, final ClassObject receiverClass) {
            final Object dnuLookupResult = receiverClass.lookupInMethodDictSlow(SqueakImageContext.getSlow().doesNotUnderstand);
            if (dnuLookupResult instanceof final CompiledCodeObject dnuMethod) {
                return new DispatchDirectDoesNotUnderstandNaryNode(assumptions, selector, dnuMethod);
            } else {
                throw SqueakException.create("Unable to find DNU method in", receiverClass);
            }
        }
    }

    static final class DispatchDirectPrimitiveNaryNode extends DispatchDirectNaryNode {
        @Child private DispatchPrimitiveNode primitiveNode;
        @Child private DispatchDirectPrimitiveFallbackNaryNode dispatchFallbackNode;

        DispatchDirectPrimitiveNaryNode(final Assumption[] assumptions, final CompiledCodeObject method, final AbstractPrimitiveNode primitiveNode) {
            super(assumptions);
            this.primitiveNode = DispatchPrimitiveNode.create(primitiveNode, method.getNumArgs());
            dispatchFallbackNode = DispatchDirectPrimitiveFallbackNaryNodeGen.create(method);
        }

        @Override
        public Object execute(final VirtualFrame frame, final Object receiver, final Object[] arguments) {
            try {
                return primitiveNode.execute(frame, receiver, arguments);
            } catch (final PrimitiveFailed pf) {
                DispatchUtils.logPrimitiveFailed(primitiveNode);
                return dispatchFallbackNode.execute(frame, receiver, arguments, pf);
            }
        }
    }

    protected abstract static class DispatchPrimitiveNode extends AbstractNode {
        protected abstract Object execute(VirtualFrame frame, Object receiver, Object[] arguments);

        public static DispatchPrimitiveNode create(final AbstractPrimitiveNode primitiveNode, final int numArgs) {
            return switch (numArgs) {
                case 0 -> new DispatchPrimitiveNode.DispatchPrimitive0Node((Primitive0) primitiveNode);
                case 1 -> new DispatchPrimitiveNode.DispatchPrimitive1Node((Primitive1) primitiveNode);
                case 2 -> new DispatchPrimitiveNode.DispatchPrimitive2Node((Primitive2) primitiveNode);
                case 3 -> new DispatchPrimitiveNode.DispatchPrimitive3Node((Primitive3) primitiveNode);
                case 4 -> new DispatchPrimitiveNode.DispatchPrimitive4Node((Primitive4) primitiveNode);
                case 5 -> new DispatchPrimitiveNode.DispatchPrimitive5Node((Primitive5) primitiveNode);
                case 6 -> new DispatchPrimitiveNode.DispatchPrimitive6Node((Primitive6) primitiveNode);
                case 7 -> new DispatchPrimitiveNode.DispatchPrimitive7Node((Primitive7) primitiveNode);
                case 8 -> new DispatchPrimitiveNode.DispatchPrimitive8Node((Primitive8) primitiveNode);
                case 9 -> new DispatchPrimitiveNode.DispatchPrimitive9Node((Primitive9) primitiveNode);
                case 10 -> new DispatchPrimitiveNode.DispatchPrimitive10Node((Primitive10) primitiveNode);
                case 11 -> new DispatchPrimitiveNode.DispatchPrimitive11Node((Primitive11) primitiveNode);
                default -> throw SqueakException.create("Unexpected number of arguments " + numArgs);
            };
        }

        private static final class DispatchPrimitive0Node extends DispatchPrimitiveNode {
            @Child private Primitive0 primitiveNode;

            private DispatchPrimitive0Node(final Primitive0 primitiveNode) {
                this.primitiveNode = primitiveNode;
            }

            @Override
            public Object execute(final VirtualFrame frame, final Object receiver, final Object[] arguments) {
                return primitiveNode.execute(frame, receiver);
            }
        }

        private static final class DispatchPrimitive1Node extends DispatchPrimitiveNode {
            @Child private Primitive1 primitiveNode;

            private DispatchPrimitive1Node(final Primitive1 primitiveNode) {
                this.primitiveNode = primitiveNode;
            }

            @Override
            public Object execute(final VirtualFrame frame, final Object receiver, final Object[] arguments) {
                return primitiveNode.execute(frame, receiver, arguments[0]);
            }
        }

        private static final class DispatchPrimitive2Node extends DispatchPrimitiveNode {
            @Child private Primitive2 primitiveNode;

            private DispatchPrimitive2Node(final Primitive2 primitiveNode) {
                this.primitiveNode = primitiveNode;
            }

            @Override
            public Object execute(final VirtualFrame frame, final Object receiver, final Object[] arguments) {
                return primitiveNode.execute(frame, receiver, arguments[0], arguments[1]);
            }
        }

        private static final class DispatchPrimitive3Node extends DispatchPrimitiveNode {
            @Child private Primitive3 primitiveNode;

            private DispatchPrimitive3Node(final Primitive3 primitiveNode) {
                this.primitiveNode = primitiveNode;
            }

            @Override
            public Object execute(final VirtualFrame frame, final Object receiver, final Object[] arguments) {
                return primitiveNode.execute(frame, receiver, arguments[0], arguments[1], arguments[2]);
            }
        }

        private static final class DispatchPrimitive4Node extends DispatchPrimitiveNode {
            @Child private Primitive4 primitiveNode;

            private DispatchPrimitive4Node(final Primitive4 primitiveNode) {
                this.primitiveNode = primitiveNode;
            }

            @Override
            public Object execute(final VirtualFrame frame, final Object receiver, final Object[] arguments) {
                return primitiveNode.execute(frame, receiver, arguments[0], arguments[1], arguments[2], arguments[3]);
            }
        }

        private static final class DispatchPrimitive5Node extends DispatchPrimitiveNode {
            @Child private Primitive5 primitiveNode;

            private DispatchPrimitive5Node(final Primitive5 primitiveNode) {
                this.primitiveNode = primitiveNode;
            }

            @Override
            public Object execute(final VirtualFrame frame, final Object receiver, final Object[] arguments) {
                return primitiveNode.execute(frame, receiver, arguments[0], arguments[1], arguments[2], arguments[3], arguments[4]);
            }
        }

        private static final class DispatchPrimitive6Node extends DispatchPrimitiveNode {
            @Child private Primitive6 primitiveNode;

            private DispatchPrimitive6Node(final Primitive6 primitiveNode) {
                this.primitiveNode = primitiveNode;
            }

            @Override
            public Object execute(final VirtualFrame frame, final Object receiver, final Object[] arguments) {
                return primitiveNode.execute(frame, receiver, arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5]);
            }
        }

        private static final class DispatchPrimitive7Node extends DispatchPrimitiveNode {
            @Child private Primitive7 primitiveNode;

            private DispatchPrimitive7Node(final Primitive7 primitiveNode) {
                this.primitiveNode = primitiveNode;
            }

            @Override
            public Object execute(final VirtualFrame frame, final Object receiver, final Object[] arguments) {
                return primitiveNode.execute(frame, receiver, arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5], arguments[6]);
            }
        }

        private static final class DispatchPrimitive8Node extends DispatchPrimitiveNode {
            @Child private Primitive8 primitiveNode;

            private DispatchPrimitive8Node(final Primitive8 primitiveNode) {
                this.primitiveNode = primitiveNode;
            }

            @Override
            public Object execute(final VirtualFrame frame, final Object receiver, final Object[] arguments) {
                return primitiveNode.execute(frame, receiver, arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5], arguments[6], arguments[7]);
            }
        }

        private static final class DispatchPrimitive9Node extends DispatchPrimitiveNode {
            @Child private Primitive9 primitiveNode;

            private DispatchPrimitive9Node(final Primitive9 primitiveNode) {
                this.primitiveNode = primitiveNode;
            }

            @Override
            public Object execute(final VirtualFrame frame, final Object receiver, final Object[] arguments) {
                return primitiveNode.execute(frame, receiver, arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5], arguments[6], arguments[7], arguments[8]);
            }
        }

        private static final class DispatchPrimitive10Node extends DispatchPrimitiveNode {
            @Child private Primitive10 primitiveNode;

            private DispatchPrimitive10Node(final Primitive10 primitiveNode) {
                this.primitiveNode = primitiveNode;
            }

            @Override
            public Object execute(final VirtualFrame frame, final Object receiver, final Object[] arguments) {
                return primitiveNode.execute(frame, receiver, arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5], arguments[6], arguments[7], arguments[8],
                                arguments[9]);
            }
        }

        private static final class DispatchPrimitive11Node extends DispatchPrimitiveNode {
            @Child private Primitive11 primitiveNode;

            private DispatchPrimitive11Node(final Primitive11 primitiveNode) {
                this.primitiveNode = primitiveNode;
            }

            @Override
            public Object execute(final VirtualFrame frame, final Object receiver, final Object[] arguments) {
                return primitiveNode.execute(frame, receiver, arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5], arguments[6], arguments[7], arguments[8],
                                arguments[9], arguments[10]);
            }
        }
    }

    abstract static class DispatchDirectPrimitiveFallbackNaryNode extends AbstractNode {
        protected final CompiledCodeObject method;

        DispatchDirectPrimitiveFallbackNaryNode(final CompiledCodeObject method) {
            this.method = method;
        }

        protected abstract Object execute(VirtualFrame frame, Object receiver, Object[] arguments, PrimitiveFailed pf);

        @Specialization
        protected static final Object doFallback(final VirtualFrame frame, final Object receiver, final Object[] arguments, final PrimitiveFailed pf,
                        @Cached("create(method)") final HandlePrimitiveFailedNode handlePrimitiveFailedNode,
                        @Cached("create(method)") final SenderNode senderNode,
                        @Cached("create(method.getCallTarget())") final DirectCallNode callNode) {
            handlePrimitiveFailedNode.execute(pf);
            return callNode.call(FrameAccess.newWith(senderNode.execute(frame), null, receiver, arguments));
        }
    }

    abstract static class DispatchDirectWithSenderNaryNode extends DispatchDirectNaryNode {
        @Child protected SenderNode senderNode;

        DispatchDirectWithSenderNaryNode(final Assumption[] assumptions, final CompiledCodeObject method) {
            super(assumptions);
            senderNode = SenderNodeGen.create(method);
        }
    }

    static final class DispatchDirectMethodNaryNode extends DispatchDirectWithSenderNaryNode {
        @Child private DirectCallNode callNode;

        DispatchDirectMethodNaryNode(final Assumption[] assumptions, final CompiledCodeObject method) {
            super(assumptions, method);
            callNode = DirectCallNode.create(method.getCallTarget());
        }

        @Override
        public Object execute(final VirtualFrame frame, final Object receiver, final Object[] arguments) {
            return callNode.call(FrameAccess.newWith(senderNode.execute(frame), null, receiver, arguments));
        }
    }

    static final class DispatchDirectDoesNotUnderstandNaryNode extends DispatchDirectWithSenderNaryNode {
        private final NativeObject selector;
        @Child private DirectCallNode callNode;
        @Child private CreateDoesNotUnderstandMessageNode createDNUMessageNode = CreateDoesNotUnderstandMessageNodeGen.create();

        DispatchDirectDoesNotUnderstandNaryNode(final Assumption[] assumptions, final NativeObject selector, final CompiledCodeObject dnuMethod) {
            super(assumptions, dnuMethod);
            this.selector = selector;
            callNode = DirectCallNode.create(dnuMethod.getCallTarget());
        }

        @Override
        public Object execute(final VirtualFrame frame, final Object receiver, final Object[] arguments) {
            return callNode.call(FrameAccess.newDNUWith(senderNode.execute(frame), receiver, createDNUMessageNode.execute(selector, receiver, arguments)));
        }
    }

    static final class DispatchDirectObjectAsMethodNaryNode extends DispatchDirectWithSenderNaryNode {
        private final NativeObject selector;
        private final Object targetObject;
        @Child private DirectCallNode callNode;

        DispatchDirectObjectAsMethodNaryNode(final Assumption[] assumptions, final NativeObject selector, final CompiledCodeObject runWithInMethod, final Object targetObject) {
            super(assumptions, runWithInMethod);
            this.selector = selector;
            callNode = DirectCallNode.create(runWithInMethod.getCallTarget());
            this.targetObject = targetObject;
        }

        @Override
        public Object execute(final VirtualFrame frame, final Object receiver, final Object[] arguments) {
            return callNode.call(FrameAccess.newOAMWith(senderNode.execute(frame), targetObject, selector, getContext().asArrayOfObjects(arguments), receiver));
        }
    }

    @GenerateInline
    @GenerateCached(false)
    public abstract static class CreateFrameArgumentsForIndirectCallNaryNode extends AbstractNode {
        public abstract Object[] execute(VirtualFrame frame, Node node, Object receiver, Object[] arguments, ClassObject receiverClass, Object lookupResult, CompiledCodeObject method,
                        NativeObject selector);

        @Specialization
        @SuppressWarnings("unused")
        protected static final Object[] doMethod(final VirtualFrame frame, final Node node, final Object receiver, final Object[] arguments, final ClassObject receiverClass,
                        @SuppressWarnings("unused") final CompiledCodeObject lookupResult,
                        final CompiledCodeObject method, final NativeObject selector,
                        @Shared("senderNode") @Cached final GetOrCreateContextOrMarkerNode senderNode) {
            return FrameAccess.newWith(senderNode.execute(frame, node, method), null, receiver, arguments);
        }

        @Specialization(guards = "lookupResult == null")
        protected static final Object[] doDoesNotUnderstand(final VirtualFrame frame, final Node node, final Object receiver, final Object[] arguments, final ClassObject receiverClass,
                        @SuppressWarnings("unused") final Object lookupResult,
                        final CompiledCodeObject method, final NativeObject selector,
                        @Cached final AbstractPointersObjectWriteNode writeNode,
                        @Shared("senderNode") @Cached final GetOrCreateContextOrMarkerNode senderNode) {
            final PointersObject message = getContext(node).newMessage(writeNode, node, selector, receiverClass, arguments);
            return FrameAccess.newDNUWith(senderNode.execute(frame, node, method), receiver, message);
        }

        @Specialization(guards = {"targetObject != null", "!isCompiledCodeObject(targetObject)"})
        protected static final Object[] doObjectAsMethod(final VirtualFrame frame, final Node node, final Object receiver, final Object[] arguments,
                        @SuppressWarnings("unused") final ClassObject receiverClass,
                        final Object targetObject, final CompiledCodeObject method, final NativeObject selector,
                        @Shared("senderNode") @Cached final GetOrCreateContextOrMarkerNode senderNode) {
            return FrameAccess.newOAMWith(senderNode.execute(frame, node, method), targetObject, selector, getContext(node).asArrayOfObjects(arguments), receiver);
        }
    }
}
