/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.LookupMethodNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetContextOrMarkerNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetOrCreateContextNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.CreateFrameArgumentNodes.CreateFrameArgumentsForDNUNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.CreateFrameArgumentNodes.CreateFrameArgumentsForOAMNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.PrimitiveFailedCounter;

public abstract class CachedDispatchNode extends AbstractNode {
    protected final CompiledCodeObject method;

    public CachedDispatchNode(final CompiledCodeObject method) {
        this.method = method;
        assert getCallTargetStable().isValid() : "callTargetStable must be valid";
    }

    protected static final CachedDispatchNode create(final VirtualFrame frame, final NativeObject selector, final int argumentCount, final ClassObject receiverClass, final Object lookupResult) {
        final SqueakImageContext image = SqueakLanguage.getContext();
        if (lookupResult == null) {
            return createDNUNode(frame, selector, argumentCount, image, receiverClass);
        } else if (lookupResult instanceof CompiledCodeObject) {
            final CompiledCodeObject lookupMethod = (CompiledCodeObject) lookupResult;
            if (lookupMethod.hasPrimitive()) {
                final AbstractPrimitiveNode primitiveNode = PrimitiveNodeFactory.forIndex(lookupMethod, true, lookupMethod.primitiveIndex(), false);
                if (primitiveNode != null) {
                    return new CachedDispatchPrimitiveNode(argumentCount, lookupMethod, primitiveNode);
                }
            }
            return AbstractCachedDispatchMethodNode.create(frame, argumentCount, lookupMethod);
        } else {
            final ClassObject lookupResultClass = SqueakObjectClassNode.getUncached().executeLookup(lookupResult);
            final Object runWithInMethod = LookupMethodNode.getUncached().executeLookup(lookupResultClass, image.runWithInSelector);
            if (runWithInMethod instanceof CompiledCodeObject) {
                return AbstractCachedDispatchObjectAsMethodNode.create(frame, selector, argumentCount, lookupResult, (CompiledCodeObject) runWithInMethod);
            } else {
                assert runWithInMethod == null : "runWithInMethod should not be another Object";
                return createDNUNode(frame, selector, argumentCount, image, lookupResultClass);
            }
        }
    }

    private static CachedDispatchNode createDNUNode(final VirtualFrame frame, final NativeObject selector, final int argumentCount, final SqueakImageContext image, final ClassObject receiverClass) {
        final Object dnuMethod = LookupMethodNode.getUncached().executeLookup(receiverClass, image.doesNotUnderstand);
        if (dnuMethod instanceof CompiledCodeObject) {
            return AbstractCachedDispatchDoesNotUnderstandNode.create(frame, selector, argumentCount, (CompiledCodeObject) dnuMethod);
        } else {
            throw SqueakException.create("Unable to find DNU method in", receiverClass);
        }
    }

    protected final Assumption getCallTargetStable() {
        return method.getCallTargetStable();
    }

    protected abstract Object execute(VirtualFrame frame);

    protected abstract static class CachedDispatchWithDirectCallNode extends CachedDispatchNode {
        @Child protected DirectCallNode callNode;

        public CachedDispatchWithDirectCallNode(final CompiledCodeObject method) {
            super(method);
            callNode = DirectCallNode.create(method.getCallTarget());
        }
    }

    protected static final class CachedDispatchPrimitiveNode extends CachedDispatchNode {
        private final int argumentCount;
        private final PrimitiveFailedCounter failureCounter = new PrimitiveFailedCounter();

        @Child private AbstractPrimitiveNode primitiveNode;

        private CachedDispatchPrimitiveNode(final int argumentCount, final CompiledCodeObject method, final AbstractPrimitiveNode primitiveNode) {
            super(method);
            this.argumentCount = argumentCount;
            this.primitiveNode = primitiveNode;
        }

        @Override
        public Object execute(final VirtualFrame frame) {
            try {
                return primitiveNode.executePrimitive(frame);
            } catch (final PrimitiveFailed pf) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                if (failureCounter.shouldNoLongerSendEagerly()) {
                    return replace(AbstractCachedDispatchMethodNode.create(frame, argumentCount, method)).execute(frame);
                } else {
                    return slowPathSendToFallbackCode(frame);
                }
            }
        }

        private Object slowPathSendToFallbackCode(final VirtualFrame frame) {
            final CompiledCodeObject code = FrameAccess.getMethodOrBlock(frame);
            final int stackPointer = FrameAccess.getStackPointer(frame, code);
            final Object[] receiverAndArguments = new Object[1 + argumentCount];
            final int numArgs = FrameAccess.getNumArguments(frame);
            for (int i = 0; i < receiverAndArguments.length; i++) {
                final int stackIndex = stackPointer + i;
                if (stackIndex < numArgs) {
                    receiverAndArguments[i] = FrameAccess.getArgument(frame, stackIndex);
                } else {
                    final FrameSlot stackSlot = FrameAccess.findStackSlot(frame, stackIndex);
                    receiverAndArguments[i] = frame.getValue(stackSlot);
                    if (frame.isObject(stackSlot)) {
                        frame.setObject(stackSlot, null); /* Clear stack slot. */
                    }
                }
            }
            return IndirectCallNode.getUncached().call(method.getCallTarget(),
                            FrameAccess.newWith(method, FrameAccess.getContextOrMarkerSlow(frame), null, receiverAndArguments));
        }
    }

    protected abstract static class AbstractCachedDispatchMethodNode extends CachedDispatchWithDirectCallNode {
        @Children protected FrameStackReadNode[] receiverAndArgumentsNodes;

        private AbstractCachedDispatchMethodNode(final VirtualFrame frame, final int argumentCount, final CompiledCodeObject method) {
            super(method);
            receiverAndArgumentsNodes = new FrameStackReadNode[1 + argumentCount];
            final int stackPointer = FrameAccess.getStackPointerSlow(frame);
            for (int i = 0; i < receiverAndArgumentsNodes.length; i++) {
                receiverAndArgumentsNodes[i] = insert(FrameStackReadNode.create(frame, stackPointer + i, true));
            }
        }

        protected static AbstractCachedDispatchMethodNode create(final VirtualFrame frame, final int argumentCount, final CompiledCodeObject method) {
            AbstractPrimitiveNode primitiveNode = null;
            if (method.hasPrimitive()) {
                primitiveNode = PrimitiveNodeFactory.forIndex(method, true, method.primitiveIndex());
            }
            if (primitiveNode != null) {
                return new CachedDispatchPrimitiveMethodWithoutSenderNode(frame, argumentCount, method, primitiveNode);
            } else if (method.getDoesNotNeedSenderAssumption().isValid()) {
                return new CachedDispatchMethodWithoutSenderNode(frame, argumentCount, method);
            } else {
                return new CachedDispatchMethodWithSenderNode(frame, argumentCount, method);
            }
        }

        protected final Object[] createFrameArguments(final VirtualFrame frame, final Object sender) {
            return FrameAccess.newWith(frame, method, sender, receiverAndArgumentsNodes);
        }
    }

    protected static final class CachedDispatchPrimitiveMethodWithoutSenderNode extends AbstractCachedDispatchMethodNode {
        @Child private AbstractPrimitiveNode primitiveNode;
        @Child private GetContextOrMarkerNode getContextOrMarkerNode = GetContextOrMarkerNode.create();

        private CachedDispatchPrimitiveMethodWithoutSenderNode(final VirtualFrame frame, final int argumentCount, final CompiledCodeObject method, final AbstractPrimitiveNode primitiveNode) {
            super(frame, argumentCount, method);
            this.primitiveNode = primitiveNode;
        }

        @Override
        public Object execute(final VirtualFrame frame) {
            try {
                return primitiveNode.executePrimitive(frame);
            } catch (final PrimitiveFailed pf) {
                // FIXME: push prim error code
            }
            try {
                method.getDoesNotNeedSenderAssumption().check();
            } catch (final InvalidAssumptionException e) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                return replace(new CachedDispatchMethodWithSenderNode(frame, receiverAndArgumentsNodes.length - 1, method)).execute(frame);
            }
            return callNode.call(createFrameArguments(frame, getContextOrMarkerNode.execute(frame)));
        }
    }

    protected static final class CachedDispatchMethodWithoutSenderNode extends AbstractCachedDispatchMethodNode {
        @Child private GetContextOrMarkerNode getContextOrMarkerNode = GetContextOrMarkerNode.create();

        private CachedDispatchMethodWithoutSenderNode(final VirtualFrame frame, final int argumentCount, final CompiledCodeObject method) {
            super(frame, argumentCount, method);
        }

        @Override
        public Object execute(final VirtualFrame frame) {
            try {
                method.getDoesNotNeedSenderAssumption().check();
            } catch (final InvalidAssumptionException e) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                return replace(new CachedDispatchMethodWithSenderNode(frame, receiverAndArgumentsNodes.length - 1, method)).execute(frame);
            }
            return callNode.call(createFrameArguments(frame, getContextOrMarkerNode.execute(frame)));
        }
    }

    protected static final class CachedDispatchMethodWithSenderNode extends AbstractCachedDispatchMethodNode {
        @Child private GetOrCreateContextNode getOrCreateContextNode = GetOrCreateContextNode.create(true);

        private CachedDispatchMethodWithSenderNode(final VirtualFrame frame, final int argumentCount, final CompiledCodeObject method) {
            super(frame, argumentCount, method);
        }

        @Override
        public Object execute(final VirtualFrame frame) {
            return callNode.call(createFrameArguments(frame, getOrCreateContextNode.executeGet(frame)));
        }
    }

    protected abstract static class AbstractCachedDispatchDoesNotUnderstandNode extends CachedDispatchWithDirectCallNode {
        @Child protected CreateFrameArgumentsForDNUNode createFrameArgumentsForDNUNode;

        private AbstractCachedDispatchDoesNotUnderstandNode(final VirtualFrame frame, final NativeObject selector, final int argumentCount, final CompiledCodeObject method) {
            super(method);
            assert !method.hasPrimitive();
            createFrameArgumentsForDNUNode = CreateFrameArgumentsForDNUNode.create(frame, selector, argumentCount);
        }

        protected static AbstractCachedDispatchDoesNotUnderstandNode create(final VirtualFrame frame, final NativeObject selector, final int argumentCount, final CompiledCodeObject method) {
            if (method.getDoesNotNeedSenderAssumption().isValid()) {
                return new CachedDispatchDoesNotUnderstandWithoutSenderNode(frame, selector, argumentCount, method);
            } else {
                return new CachedDispatchDoesNotUnderstandWithSenderNode(frame, selector, argumentCount, method);
            }
        }
    }

    protected static final class CachedDispatchDoesNotUnderstandWithoutSenderNode extends AbstractCachedDispatchDoesNotUnderstandNode {
        @Child private GetContextOrMarkerNode getContextOrMarkerNode = GetContextOrMarkerNode.create();

        private CachedDispatchDoesNotUnderstandWithoutSenderNode(final VirtualFrame frame, final NativeObject selector, final int argumentCount, final CompiledCodeObject method) {
            super(frame, selector, argumentCount, method);
        }

        @Override
        public Object execute(final VirtualFrame frame) {
            try {
                method.getDoesNotNeedSenderAssumption().check();
            } catch (final InvalidAssumptionException e) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                return replace(new CachedDispatchDoesNotUnderstandWithSenderNode(frame, createFrameArgumentsForDNUNode.getSelector(), createFrameArgumentsForDNUNode.getArgumentCount(),
                                method)).execute(frame);
            }
            return callNode.call(createFrameArgumentsForDNUNode.execute(frame, method, getContextOrMarkerNode.execute(frame)));
        }
    }

    protected static final class CachedDispatchDoesNotUnderstandWithSenderNode extends AbstractCachedDispatchDoesNotUnderstandNode {
        @Child private GetOrCreateContextNode getOrCreateContextNode = GetOrCreateContextNode.create(true);

        private CachedDispatchDoesNotUnderstandWithSenderNode(final VirtualFrame frame, final NativeObject selector, final int argumentCount, final CompiledCodeObject method) {
            super(frame, selector, argumentCount, method);
        }

        @Override
        public Object execute(final VirtualFrame frame) {
            return callNode.call(createFrameArgumentsForDNUNode.execute(frame, method, getOrCreateContextNode.executeGet(frame)));
        }
    }

    protected abstract static class AbstractCachedDispatchObjectAsMethodNode extends CachedDispatchWithDirectCallNode {
        protected final Object object;

        @Child protected CreateFrameArgumentsForOAMNode createFrameArgumentsForOAMNode;

        private AbstractCachedDispatchObjectAsMethodNode(final VirtualFrame frame, final NativeObject selector, final int argumentCount, final Object object, final CompiledCodeObject method) {
            super(method);
            assert !method.hasPrimitive();
            this.object = object;
            createFrameArgumentsForOAMNode = CreateFrameArgumentsForOAMNode.create(frame, selector, argumentCount);
        }

        public static CachedDispatchNode create(final VirtualFrame frame, final NativeObject selector, final int argumentCount, final Object lookupResult, final CompiledCodeObject runWithInMethod) {
            if (runWithInMethod.getDoesNotNeedSenderAssumption().isValid()) {
                return new CachedDispatchObjectAsMethodWithoutSenderNode(frame, selector, argumentCount, lookupResult, runWithInMethod);
            } else {
                return new CachedDispatchObjectAsMethodWithSenderNode(frame, selector, argumentCount, lookupResult, runWithInMethod);
            }
        }
    }

    protected static final class CachedDispatchObjectAsMethodWithoutSenderNode extends AbstractCachedDispatchObjectAsMethodNode {
        @Child private GetContextOrMarkerNode getContextOrMarkerNode = GetContextOrMarkerNode.create();

        private CachedDispatchObjectAsMethodWithoutSenderNode(final VirtualFrame frame, final NativeObject selector, final int argumentCount, final Object object, final CompiledCodeObject method) {
            super(frame, selector, argumentCount, object, method);
        }

        @Override
        public Object execute(final VirtualFrame frame) {
            try {
                method.getDoesNotNeedSenderAssumption().check();
            } catch (final InvalidAssumptionException e) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                return replace(new CachedDispatchObjectAsMethodWithSenderNode(frame, createFrameArgumentsForOAMNode.getSelector(), createFrameArgumentsForOAMNode.getArgumentCount(), object,
                                method)).execute(frame);
            }
            return callNode.call(createFrameArgumentsForOAMNode.execute(frame, object, method, getContextOrMarkerNode.execute(frame)));
        }
    }

    protected static final class CachedDispatchObjectAsMethodWithSenderNode extends AbstractCachedDispatchObjectAsMethodNode {
        @Child private GetOrCreateContextNode getOrCreateContextNode = GetOrCreateContextNode.create(true);

        private CachedDispatchObjectAsMethodWithSenderNode(final VirtualFrame frame, final NativeObject selector, final int argumentCount, final Object object, final CompiledCodeObject method) {
            super(frame, selector, argumentCount, object, method);
        }

        @Override
        public Object execute(final VirtualFrame frame) {
            return callNode.call(createFrameArgumentsForOAMNode.execute(frame, object, method, getOrCreateContextNode.executeGet(frame)));
        }
    }
}
