/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.LookupMethodNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.trufflesqueak.nodes.context.GetOrCreateContextWithoutFrameNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.Dis0NodeFactory.GenericGuardNodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.Dis0NodeFactory.IndirectDis0NodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector0Node.DispatchDirectPrimitiveFallback0Node;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector0Node.DispatchIndirect0Node.CreateFrameArgumentsForIndirectCall0Node;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector0Node.DispatchIndirect0Node.TryPrimitive0Node;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector0NodeFactory.DispatchDirectPrimitiveFallback0NodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class Dis0Node extends AbstractDispatchNode {
    private static final int LOOKUP_CACHE_SIZE = 8;
    private static final int DISPATCH_CACHE_SIZE = 4;

    @Child private AbstractDis0Node dispatchNode = new DirectDis0Node();

    Dis0Node(final NativeObject selector) {
        super(selector);
    }

    @NeverDefault
    public static Dis0Node create(final NativeObject selector) {
        return new Dis0Node(selector);
    }

    public Object execute(final VirtualFrame frame, final Object receiver) {
        if (dispatchNode instanceof final DirectDis0Node directNode) {
            return directNode.execute(frame, receiver);
        } else {
            return ((IndirectDis0Node) dispatchNode).execute(frame, selector, receiver);
        }
    }

    abstract static class AbstractDis0Node extends AbstractNode {
    }

    static class DirectDisData0Node extends AbstractNode {
        private final CompiledCodeObject method;

        @CompilationFinal(dimensions = 1) Assumption[] assumptions;

        @Child AbstractGuardNode guardChainNode;
        @Child Dispatch0Node dispatchDirectNode;
        @Child DirectDisData0Node next;

        DirectDisData0Node(final Object receiver, final LookupResult result) {
            guardChainNode = new GuardChainNode(receiver);
            this.method = result.method();
            this.dispatchDirectNode = Dispatch0Node.create(result);
            this.assumptions = DispatchUtils.createAssumptions(result.receiverClass, method);
        }
    }

    public abstract static class Dispatch0Node extends AbstractNode {
        static Dispatch0Node create(final LookupResult result) {
            return switch (result.kind()) {
                case STANDARD_METHOD -> {
                    final CompiledCodeObject method = result.method();
                    if (method.hasPrimitive()) {
                        final AbstractPrimitiveNode primitiveNode = PrimitiveNodeFactory.getOrCreateIndexedOrNamed(method);
                        if (primitiveNode instanceof final Primitive0 primitive0) {
                            yield new DispatchDirectPrimitive0Node(method, primitive0);
                        }
                        DispatchUtils.logMissingPrimitive(primitiveNode, method);
                    }
                    yield new DispatchDirectMethod0Node(method);
                }
                case DOES_NOT_UNDERSTAND -> new DispatchDirectMessageFallback0Node(result);
                case OBJECT_AS_METHOD -> new DispatchDirectObjectAsMethod0Node(result);
            };
        }

        public abstract Object execute(VirtualFrame frame, Object receiver);

        static final class DispatchDirectPrimitive0Node extends Dispatch0Node {
            @Child private Primitive0 primitiveNode;
            @Child private DispatchDirectPrimitiveFallback0Node dispatchFallbackNode;

            DispatchDirectPrimitive0Node(final CompiledCodeObject method, final Primitive0 primitiveNode) {
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

        abstract static class DispatchWithSender0Node extends Dispatch0Node {
            @Child protected GetOrCreateContextWithoutFrameNode senderNode = GetOrCreateContextWithoutFrameNode.create();
        }

        static final class DispatchDirectMethod0Node extends DispatchWithSender0Node {
            @Child private DirectCallNode callNode;

            DispatchDirectMethod0Node(final CompiledCodeObject method) {
                callNode = DirectCallNode.create(method.getCallTarget());
            }

            @Override
            public Object execute(final VirtualFrame frame, final Object receiver) {
                return callNode.call(FrameAccess.newWith(senderNode.execute(frame), null, receiver));
            }
        }

        static final class DispatchDirectMessageFallback0Node extends DispatchWithSender0Node {
            private final NativeObject selector;
            @Child private DirectCallNode callNode;
            @Child private CreateMessageNode createMessageNode = CreateMessageNodeGen.create();

            DispatchDirectMessageFallback0Node(final LookupResult result) {
                this.selector = result.selector();
                callNode = DirectCallNode.create(result.method().getCallTarget());
            }

            @Override
            public Object execute(final VirtualFrame frame, final Object receiver) {
                final PointersObject message = createMessageNode.execute(selector, receiver, ArrayUtils.EMPTY_ARRAY);
                return callNode.call(FrameAccess.newMessageFallbackWith(senderNode.execute(frame), receiver, message));
            }
        }

        static final class DispatchDirectObjectAsMethod0Node extends DispatchWithSender0Node {
            private final NativeObject selector;
            private final Object targetObject;
            @Child private DirectCallNode callNode;

            DispatchDirectObjectAsMethod0Node(final LookupResult result) {
                this.selector = result.selector();
                callNode = DirectCallNode.create(result.method().getCallTarget());
                this.targetObject = result.targetObject();
            }

            @Override
            public Object execute(final VirtualFrame frame, final Object receiver) {
                return callNode.call(FrameAccess.newOAMWith(senderNode.execute(frame), targetObject, selector, getContext().asArrayOfObjects(ArrayUtils.EMPTY_ARRAY), receiver));
            }
        }
    }

    static class DirectDis0Node extends AbstractDis0Node {
        @Child DirectDisData0Node head;

        @ExplodeLoop
        Object execute(final VirtualFrame frame, final Object receiver) {
            DirectDisData0Node current = head;
            while (current != null) {
                if (!Assumption.isValidAssumption(current.assumptions)) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    remove(current);
                    return executeAndSpecialize(frame, receiver);
                }
                if (current.guardChainNode.execute(receiver)) {
                    return current.dispatchDirectNode.execute(frame, receiver);
                }
                current = current.next;
            }
            CompilerDirectives.transferToInterpreterAndInvalidate();
            return executeAndSpecialize(frame, receiver);
        }

        Object executeAndSpecialize(final VirtualFrame frame, final Object receiver) {
            final NativeObject selector = ((Dis0Node) getParent()).selector;
            final LookupResult result = resolveTargetMethod(receiver, selector);

            DirectDisData0Node previous = null;
            DirectDisData0Node current = head;
            int count = 0;
            while (current != null) {
                if (current.method == result.method()) {
                    if (current.guardChainNode.append(receiver)) {
                        final Set<Assumption> assumptions = new HashSet<>(Arrays.asList(current.assumptions));
                        Collections.addAll(assumptions, DispatchUtils.createAssumptions(result.receiverClass, result.lookupResult));
                        current.assumptions = assumptions.toArray(new Assumption[0]);
                    } else {
                        current.guardChainNode = current.insert(GenericGuardNodeGen.create(selector, current.method));
                    }
                    return current.dispatchDirectNode.execute(frame, receiver);
                }
                previous = current;
                current = current.next;
                count++;
            }
            if (count < DISPATCH_CACHE_SIZE) {
                final DirectDisData0Node newNext = new DirectDisData0Node(receiver, result);
                if (previous == null) {
                    head = insert(newNext);
                } else {
                    previous.next = previous.insert(newNext);
                }
                return newNext.dispatchDirectNode.execute(frame, receiver);
            } else {
                this.reportPolymorphicSpecialize();
                return replace(IndirectDis0NodeGen.create()).execute(frame, selector, receiver);
            }
        }

        LookupResult resolveTargetMethod(final Object receiver, final NativeObject selector) {
            final ClassObject receiverClass = SqueakObjectClassNode.executeUncached(receiver);
            final Object lookupResult = receiverClass.lookupInMethodDictSlow(selector);
            if (lookupResult instanceof final CompiledCodeObject lookupMethod) {
                return new LookupResult(selector, receiverClass, lookupResult, lookupMethod, LookupKind.STANDARD_METHOD, null);
            } else if (lookupResult == null) {
                return new LookupResult(selector, receiverClass, lookupResult, receiverClass.resolveDispatchFailure(selector), LookupKind.DOES_NOT_UNDERSTAND, null);
            } else {
                final ClassObject lookupResultClass = SqueakObjectClassNode.executeUncached(lookupResult);
                final Object runWithInLookupResult = LookupMethodNode.executeUncached(lookupResultClass, SqueakImageContext.getSlow().runWithInSelector);
                if (runWithInLookupResult instanceof final CompiledCodeObject runWithInMethod) {
                    return new LookupResult(selector, receiverClass, lookupResult, runWithInMethod, LookupKind.OBJECT_AS_METHOD, lookupResult);
                } else {
                    assert runWithInLookupResult == null;
                    return new LookupResult(selector, receiverClass, lookupResult, lookupResultClass.resolveDispatchFailure(selector), LookupKind.DOES_NOT_UNDERSTAND, null);
                }
            }
        }

        void remove(final DirectDisData0Node target) {
            assert head != null;
            DirectDisData0Node previous = null;
            DirectDisData0Node current = head;
            while (current != null) {
                if (current == target) {
                    if (previous == null) {
                        head = null;
                    } else {
                        previous.next = target.next;
                    }
                    return;
                }
                previous = current;
                current = current.next;
            }
        }
    }

    abstract static class AbstractGuardNode extends AbstractNode {
        abstract boolean execute(Object receiver);

        abstract boolean append(Object receiver);
    }

    static class GuardChainNode extends AbstractGuardNode {
        final LookupClassGuard guard;
        @Child private GuardChainNode next;

        GuardChainNode(final Object receiver) {
            this.guard = LookupClassGuard.create(receiver);
        }

        @Override
        @ExplodeLoop
        boolean execute(final Object receiver) {
            GuardChainNode current = this;
            while (current != null) {
                if (current.guard.check(receiver)) {
                    return true;
                }
                current = current.next;
            }
            return false;
        }

        @Override
        boolean append(final Object receiver) {
            GuardChainNode current = this;
            int count = 0;
            while (current.next != null) {
                current = current.next;
                count++;
            }
            if (count < LOOKUP_CACHE_SIZE) {
                current.next = insert(new GuardChainNode(receiver));
                return true;
            } else {
                return false;
            }
        }
    }

    abstract static class GenericGuardNode extends AbstractGuardNode {
        final NativeObject selector;
        final CompiledCodeObject method;

        GenericGuardNode(final NativeObject selector, final CompiledCodeObject method) {
            this.selector = selector;
            this.method = method;
        }

        @Specialization
        boolean doGeneric(final Object receiver,
                        @Bind final Node node,
                        @Bind final SqueakImageContext image,
                        @Cached(inline = true) final SqueakObjectClassNode classNode,
                        @Cached final ResolveMethodNode methodNode) {
            final ClassObject receiverClass = classNode.executeLookup(node, receiver);
            final Object lookupResult = image.lookup(receiverClass, selector);
            final CompiledCodeObject targetMethod = methodNode.execute(node, image, 0, false, selector, receiverClass, lookupResult);
            return method == targetMethod;
        }

        @Override
        boolean append(final Object receiver) {
            return true; /* Always successful */
        }
    }

    @GenerateInline(false)
    abstract static class IndirectDis0Node extends AbstractDis0Node {
        public abstract Object execute(VirtualFrame frame, NativeObject selector, Object receiver);

        @Specialization
        protected static final Object doIndirect(final VirtualFrame frame, final NativeObject selector, final Object receiver,
                        @Bind final Node node,
                        @Bind final SqueakImageContext image,
                        @Cached(inline = true) final SqueakObjectClassNode classNode,
                        @Cached final ResolveMethodNode methodNode,
                        @Cached final TryPrimitive0Node tryPrimitiveNode,
                        @Cached(inline = true) final GetOrCreateContextWithoutFrameNode senderNode,
                        @Cached final CreateFrameArgumentsForIndirectCall0Node argumentsNode,
                        @Cached final IndirectCallNode callNode) {
            final ClassObject receiverClass = classNode.executeLookup(node, receiver);
            final Object lookupResult = image.lookup(receiverClass, selector);
            final CompiledCodeObject method = methodNode.execute(node, image, 0, false, selector, receiverClass, lookupResult);
            final Object result = tryPrimitiveNode.execute(frame, method, receiver);
            if (result != null) {
                return result;
            } else {
                return callNode.call(method.getCallTarget(), argumentsNode.execute(node, senderNode.execute(frame, node), receiver, receiverClass, lookupResult, selector));
            }
        }
    }

    record LookupResult(NativeObject selector, ClassObject receiverClass, Object lookupResult, CompiledCodeObject method, LookupKind kind, Object targetObject) {
    }

    enum LookupKind {
        STANDARD_METHOD,
        DOES_NOT_UNDERSTAND,
        OBJECT_AS_METHOD,
    }
}
