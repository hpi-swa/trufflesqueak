/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import static de.hpi.swa.trufflesqueak.nodes.dispatch.AbstractDisNode.DISPATCH_CACHE_SIZE;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
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
import de.hpi.swa.trufflesqueak.nodes.dispatch.AbstractDisNode.AbstractGuardNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.AbstractDisNode.GuardChainNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.AbstractDisNode.LookupKind;
import de.hpi.swa.trufflesqueak.nodes.dispatch.AbstractDisNode.LookupResult;
import de.hpi.swa.trufflesqueak.nodes.dispatch.AbstractDisNodeFactory.GenericGuardNodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.Dis1NodeFactory.IndirectDis1NodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector1Node.DispatchDirectPrimitiveFallback1Node;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector1Node.DispatchIndirect1Node.CreateFrameArgumentsForIndirectCall1Node;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector1Node.DispatchIndirect1Node.TryPrimitive1Node;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector1NodeFactory.DispatchDirectPrimitiveFallback1NodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class Dis1Node extends AbstractDispatchNode {
    @Child private AbstractDis1Node dispatchNode = new DirectDis1Node();

    Dis1Node(final NativeObject selector) {
        super(selector);
    }

    @NeverDefault
    public static Dis1Node create(final NativeObject selector) {
        return new Dis1Node(selector);
    }

    public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1) {
        if (dispatchNode instanceof final DirectDis1Node directNode) {
            return directNode.execute(frame, receiver, arg1);
        } else {
            return ((IndirectDis1Node) dispatchNode).execute(frame, selector, receiver, arg1);
        }
    }

    abstract static class AbstractDis1Node extends AbstractNode {
    }

    static class DirectDisData1Node extends AbstractNode {
        private final CompiledCodeObject method;
        private final Assumption assumption;

        @Child AbstractGuardNode guardChainNode;
        @Child Dispatch1Node dispatchDirectNode;
        @Child DirectDisData1Node next;

        DirectDisData1Node(final Object receiver, final LookupResult result) {
            guardChainNode = new GuardChainNode(receiver, result);
            method = result.method();
            assumption = method.getCallTargetStable();
            dispatchDirectNode = Dispatch1Node.create(result);
        }
    }

    public abstract static class Dispatch1Node extends AbstractNode {
        static Dispatch1Node create(final LookupResult result) {
            return switch (result.kind()) {
                case STANDARD_METHOD -> {
                    final CompiledCodeObject method = result.method();
                    if (method.hasPrimitive()) {
                        final AbstractPrimitiveNode primitiveNode = PrimitiveNodeFactory.getOrCreateIndexedOrNamed(method);
                        if (primitiveNode instanceof final Primitive1 primitive1) {
                            yield new DispatchDirectPrimitive1Node(method, primitive1);
                        }
                        DispatchUtils.logMissingPrimitive(primitiveNode, method);
                    }
                    yield new DispatchDirectMethod1Node(method);
                }
                case DOES_NOT_UNDERSTAND -> new DispatchDirectMessageFallback1Node(result);
                case OBJECT_AS_METHOD -> new DispatchDirectObjectAsMethod1Node(result);
            };
        }

        public abstract Object execute(VirtualFrame frame, Object receiver, Object arg1);

        static final class DispatchDirectPrimitive1Node extends Dispatch1Node {
            @Child private Primitive1 primitiveNode;
            @Child private DispatchDirectPrimitiveFallback1Node dispatchFallbackNode;

            DispatchDirectPrimitive1Node(final CompiledCodeObject method, final Primitive1 primitiveNode) {
                this.primitiveNode = primitiveNode;
                dispatchFallbackNode = DispatchDirectPrimitiveFallback1NodeGen.create(method);
            }

            @Override
            public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1) {
                try {
                    return primitiveNode.execute(frame, receiver, arg1);
                } catch (final PrimitiveFailed pf) {
                    DispatchUtils.logPrimitiveFailed(primitiveNode);
                    return dispatchFallbackNode.execute(frame, receiver, arg1, pf);
                }
            }
        }

        abstract static class DispatchWithSender1Node extends Dispatch1Node {
            @Child protected GetOrCreateContextWithoutFrameNode senderNode = GetOrCreateContextWithoutFrameNode.create();
        }

        static final class DispatchDirectMethod1Node extends DispatchWithSender1Node {
            @Child private DirectCallNode callNode;

            DispatchDirectMethod1Node(final CompiledCodeObject method) {
                callNode = DirectCallNode.create(method.getCallTarget());
            }

            @Override
            public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1) {
                return callNode.call(FrameAccess.newWith(senderNode.execute(frame), null, receiver, arg1));
            }
        }

        static final class DispatchDirectMessageFallback1Node extends DispatchWithSender1Node {
            private final NativeObject selector;
            @Child private DirectCallNode callNode;
            @Child private CreateMessageNode createMessageNode = CreateMessageNodeGen.create();

            DispatchDirectMessageFallback1Node(final LookupResult result) {
                this.selector = result.selector();
                callNode = DirectCallNode.create(result.method().getCallTarget());
            }

            @Override
            public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1) {
                final PointersObject message = createMessageNode.execute(selector, receiver, new Object[]{arg1});
                return callNode.call(FrameAccess.newMessageFallbackWith(senderNode.execute(frame), receiver, message));
            }
        }

        static final class DispatchDirectObjectAsMethod1Node extends DispatchWithSender1Node {
            private final NativeObject selector;
            private final Object targetObject;
            @Child private DirectCallNode callNode;

            DispatchDirectObjectAsMethod1Node(final LookupResult result) {
                this.selector = result.selector();
                callNode = DirectCallNode.create(result.method().getCallTarget());
                this.targetObject = result.targetObject();
            }

            @Override
            public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1) {
                return callNode.call(FrameAccess.newOAMWith(senderNode.execute(frame), targetObject, selector, getContext().asArrayOfObjects(arg1), receiver));
            }
        }
    }

    static class DirectDis1Node extends AbstractDis1Node {
        @Child DirectDisData1Node head;

        @ExplodeLoop
        Object execute(final VirtualFrame frame, final Object receiver, final Object arg1) {
            DirectDisData1Node current = head;
            while (current != null) {
                if (!Assumption.isValidAssumption(current.assumption)) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    remove(current);
                    return executeAndSpecialize(frame, receiver, arg1);
                }
                if (current.guardChainNode.execute(receiver)) {
                    return current.dispatchDirectNode.execute(frame, receiver, arg1);
                }
                current = current.next;
            }
            CompilerDirectives.transferToInterpreterAndInvalidate();
            return executeAndSpecialize(frame, receiver, arg1);
        }

        Object executeAndSpecialize(final VirtualFrame frame, final Object receiver, final Object arg1) {
            final NativeObject selector = ((Dis1Node) getParent()).selector;
            final LookupResult result = resolveTargetMethod(receiver, selector);

            DirectDisData1Node previous = null;
            DirectDisData1Node current = head;
            int count = 0;
            while (current != null) {
                if (current.method == result.method()) {
                    if (!current.guardChainNode.append(receiver, result)) {
                        current.guardChainNode = current.insert(GenericGuardNodeGen.create(selector, current.method, 1));
                    }
                    return current.dispatchDirectNode.execute(frame, receiver, arg1);
                }
                previous = current;
                current = current.next;
                count++;
            }
            if (count < DISPATCH_CACHE_SIZE) {
                final DirectDisData1Node newNext = new DirectDisData1Node(receiver, result);
                if (previous == null) {
                    head = insert(newNext);
                } else {
                    previous.next = previous.insert(newNext);
                }
                return newNext.dispatchDirectNode.execute(frame, receiver, arg1);
            } else {
                this.reportPolymorphicSpecialize();
                return replace(IndirectDis1NodeGen.create()).execute(frame, selector, receiver, arg1);
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

        void remove(final DirectDisData1Node target) {
            assert head != null;
            DirectDisData1Node previous = null;
            DirectDisData1Node current = head;
            while (current != null) {
                if (current == target) {
                    if (previous == null) {
                        head = current.next;
                    } else {
                        previous.next = current.next;
                    }
                    return;
                }
                previous = current;
                current = current.next;
            }
        }
    }

    @GenerateInline(false)
    abstract static class IndirectDis1Node extends AbstractDis1Node {
        public abstract Object execute(VirtualFrame frame, NativeObject selector, Object receiver, Object arg1);

        @Specialization
        protected static final Object doIndirect(final VirtualFrame frame, final NativeObject selector, final Object receiver, final Object arg1,
                        @Bind final Node node,
                        @Bind final SqueakImageContext image,
                        @Cached(inline = true) final SqueakObjectClassNode classNode,
                        @Cached final ResolveMethodNode methodNode,
                        @Cached final TryPrimitive1Node tryPrimitiveNode,
                        @Cached(inline = true) final GetOrCreateContextWithoutFrameNode senderNode,
                        @Cached final CreateFrameArgumentsForIndirectCall1Node argumentsNode,
                        @Cached final IndirectCallNode callNode) {
            final ClassObject receiverClass = classNode.executeLookup(node, receiver);
            final Object lookupResult = image.lookup(receiverClass, selector);
            final CompiledCodeObject method = methodNode.execute(node, image, 1, false, selector, receiverClass, lookupResult);
            final Object result = tryPrimitiveNode.execute(frame, method, receiver, arg1);
            if (result != null) {
                return result;
            } else {
                return callNode.call(method.getCallTarget(), argumentsNode.execute(node, senderNode.execute(frame, node), receiver, arg1, receiverClass, lookupResult, selector));
            }
        }
    }
}
