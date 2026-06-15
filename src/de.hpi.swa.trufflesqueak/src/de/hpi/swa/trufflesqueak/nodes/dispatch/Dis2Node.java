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
import de.hpi.swa.trufflesqueak.nodes.dispatch.Dis2NodeFactory.GenericGuardNodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.Dis2NodeFactory.IndirectDis2NodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector2Node.DispatchDirectPrimitiveFallback2Node;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector2Node.DispatchIndirect2Node.CreateFrameArgumentsForIndirectCall2Node;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector2Node.DispatchIndirect2Node.TryPrimitive2Node;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector2NodeFactory.DispatchDirectPrimitiveFallback2NodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive2;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class Dis2Node extends AbstractDispatchNode {
    private static final int LOOKUP_CACHE_SIZE = 8;
    private static final int DISPATCH_CACHE_SIZE = 4;

    @Child private AbstractDis2Node dispatchNode = new DirectDis2Node();

    Dis2Node(final NativeObject selector) {
        super(selector);
    }

    @NeverDefault
    public static Dis2Node create(final NativeObject selector) {
        return new Dis2Node(selector);
    }

    public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2) {
        if (dispatchNode instanceof final DirectDis2Node directNode) {
            return directNode.execute(frame, receiver, arg1, arg2);
        } else {
            return ((IndirectDis2Node) dispatchNode).execute(frame, selector, receiver, arg1, arg2);
        }
    }

    abstract static class AbstractDis2Node extends AbstractNode {
    }

    static class DirectDisData2Node extends AbstractNode {
        private final CompiledCodeObject method;

        @CompilationFinal(dimensions = 1) Assumption[] assumptions;

        @Child AbstractGuardNode guardChainNode;
        @Child Dispatch2Node dispatchDirectNode;
        @Child DirectDisData2Node next;

        DirectDisData2Node(final Object receiver, final LookupResult result) {
            guardChainNode = new GuardChainNode(receiver);
            this.method = result.method();
            this.dispatchDirectNode = Dispatch2Node.create(result);
            this.assumptions = DispatchUtils.createAssumptions(result.receiverClass, method);
        }
    }

    public abstract static class Dispatch2Node extends AbstractNode {
        static Dispatch2Node create(final LookupResult result) {
            return switch (result.kind()) {
                case STANDARD_METHOD -> {
                    final CompiledCodeObject method = result.method();
                    if (method.hasPrimitive()) {
                        final AbstractPrimitiveNode primitiveNode = PrimitiveNodeFactory.getOrCreateIndexedOrNamed(method);
                        if (primitiveNode instanceof final Primitive2 primitive2) {
                            yield new DispatchDirectPrimitive2Node(method, primitive2);
                        }
                        DispatchUtils.logMissingPrimitive(primitiveNode, method);
                    }
                    yield new DispatchDirectMethod2Node(method);
                }
                case DOES_NOT_UNDERSTAND -> new DispatchDirectMessageFallback2Node(result);
                case OBJECT_AS_METHOD -> new DispatchDirectObjectAsMethod2Node(result);
            };
        }

        public abstract Object execute(VirtualFrame frame, Object receiver, Object arg1, Object arg2);

        static final class DispatchDirectPrimitive2Node extends Dispatch2Node {
            @Child private Primitive2 primitiveNode;
            @Child private DispatchDirectPrimitiveFallback2Node dispatchFallbackNode;

            DispatchDirectPrimitive2Node(final CompiledCodeObject method, final Primitive2 primitiveNode) {
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

        abstract static class DispatchWithSender2Node extends Dispatch2Node {
            @Child protected GetOrCreateContextWithoutFrameNode senderNode = GetOrCreateContextWithoutFrameNode.create();
        }

        static final class DispatchDirectMethod2Node extends DispatchWithSender2Node {
            @Child private DirectCallNode callNode;

            DispatchDirectMethod2Node(final CompiledCodeObject method) {
                callNode = DirectCallNode.create(method.getCallTarget());
            }

            @Override
            public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2) {
                return callNode.call(FrameAccess.newWith(senderNode.execute(frame), null, receiver, arg1, arg2));
            }
        }

        static final class DispatchDirectMessageFallback2Node extends DispatchWithSender2Node {
            private final NativeObject selector;
            @Child private DirectCallNode callNode;
            @Child private CreateMessageNode createMessageNode = CreateMessageNodeGen.create();

            DispatchDirectMessageFallback2Node(final LookupResult result) {
                this.selector = result.selector();
                callNode = DirectCallNode.create(result.method().getCallTarget());
            }

            @Override
            public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2) {
                final PointersObject message = createMessageNode.execute(selector, receiver, new Object[]{arg1, arg2});
                return callNode.call(FrameAccess.newMessageFallbackWith(senderNode.execute(frame), receiver, message));
            }
        }

        static final class DispatchDirectObjectAsMethod2Node extends DispatchWithSender2Node {
            private final NativeObject selector;
            private final Object targetObject;
            @Child private DirectCallNode callNode;

            DispatchDirectObjectAsMethod2Node(final LookupResult result) {
                this.selector = result.selector();
                callNode = DirectCallNode.create(result.method().getCallTarget());
                this.targetObject = result.targetObject();
            }

            @Override
            public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2) {
                return callNode.call(FrameAccess.newOAMWith(senderNode.execute(frame), targetObject, selector, getContext().asArrayOfObjects(arg1, arg2), receiver));
            }
        }
    }

    static class DirectDis2Node extends AbstractDis2Node {
        @Child DirectDisData2Node head;

        @ExplodeLoop
        Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2) {
            DirectDisData2Node current = head;
            while (current != null) {
                if (!Assumption.isValidAssumption(current.assumptions)) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    remove(current);
                    return executeAndSpecialize(frame, receiver, arg1, arg2);
                }
                if (current.guardChainNode.execute(receiver)) {
                    return current.dispatchDirectNode.execute(frame, receiver, arg1, arg2);
                }
                current = current.next;
            }
            CompilerDirectives.transferToInterpreterAndInvalidate();
            return executeAndSpecialize(frame, receiver, arg1, arg2);
        }

        Object executeAndSpecialize(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2) {
            final NativeObject selector = ((Dis2Node) getParent()).selector;
            final LookupResult result = resolveTargetMethod(receiver, selector);

            DirectDisData2Node previous = null;
            DirectDisData2Node current = head;
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
                    return current.dispatchDirectNode.execute(frame, receiver, arg1, arg2);
                }
                previous = current;
                current = current.next;
                count++;
            }
            if (count < DISPATCH_CACHE_SIZE) {
                final DirectDisData2Node newNext = new DirectDisData2Node(receiver, result);
                if (previous == null) {
                    head = insert(newNext);
                } else {
                    previous.next = previous.insert(newNext);
                }
                return newNext.dispatchDirectNode.execute(frame, receiver, arg1, arg2);
            } else {
                this.reportPolymorphicSpecialize();
                return replace(IndirectDis2NodeGen.create()).execute(frame, selector, receiver, arg1, arg2);
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

        void remove(final DirectDisData2Node target) {
            assert head != null;
            DirectDisData2Node previous = null;
            DirectDisData2Node current = head;
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
            final CompiledCodeObject targetMethod = methodNode.execute(node, image, 2, false, selector, receiverClass, lookupResult);
            return method == targetMethod;
        }

        @Override
        boolean append(final Object receiver) {
            return true; /* Always successful */
        }
    }

    @GenerateInline(false)
    abstract static class IndirectDis2Node extends AbstractDis2Node {
        public abstract Object execute(VirtualFrame frame, NativeObject selector, Object receiver, Object arg1, Object arg2);

        @Specialization
        protected static final Object doIndirect(final VirtualFrame frame, final NativeObject selector, final Object receiver, final Object arg1, final Object arg2,
                        @Bind final Node node,
                        @Bind final SqueakImageContext image,
                        @Cached(inline = true) final SqueakObjectClassNode classNode,
                        @Cached final ResolveMethodNode methodNode,
                        @Cached final TryPrimitive2Node tryPrimitiveNode,
                        @Cached(inline = true) final GetOrCreateContextWithoutFrameNode senderNode,
                        @Cached final CreateFrameArgumentsForIndirectCall2Node argumentsNode,
                        @Cached final IndirectCallNode callNode) {
            final ClassObject receiverClass = classNode.executeLookup(node, receiver);
            final Object lookupResult = image.lookup(receiverClass, selector);
            final CompiledCodeObject method = methodNode.execute(node, image, 2, false, selector, receiverClass, lookupResult);
            final Object result = tryPrimitiveNode.execute(frame, method, receiver, arg1, arg2);
            if (result != null) {
                return result;
            } else {
                return callNode.call(method.getCallTarget(), argumentsNode.execute(node, senderNode.execute(frame, node), receiver, arg1, arg2, receiverClass, lookupResult, selector));
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
