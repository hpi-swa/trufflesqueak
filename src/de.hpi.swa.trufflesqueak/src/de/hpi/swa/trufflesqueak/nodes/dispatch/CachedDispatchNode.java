/*
 * Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2023 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
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
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory.ArgumentsLocation;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.PrimitiveFailedCounter;

public abstract class CachedDispatchNode extends AbstractNode {
    protected final CompiledCodeObject method;

    public CachedDispatchNode(final CompiledCodeObject method) {
        this.method = method;
        assert getCallTargetStable().isValid() : "callTargetStable must be valid";
    }

    @NeverDefault
    protected static final CachedDispatchNode create(final VirtualFrame frame, final NativeObject selector, final int argumentCount, final ClassObject receiverClass, final Object lookupResult) {
        final SqueakImageContext image = SqueakImageContext.getSlow();
        if (lookupResult == null) {
            return createDNUNode(frame, selector, argumentCount, image, receiverClass);
        } else if (lookupResult instanceof final CompiledCodeObject lookupMethod) {
            if (lookupMethod.hasPrimitive()) {
                final AbstractPrimitiveNode primitiveNode = PrimitiveNodeFactory.getOrCreateIndexedOrNamed(lookupMethod, ArgumentsLocation.ON_STACK);
                if (primitiveNode != null) {
                    return new CachedDispatchPrimitiveNode(argumentCount, lookupMethod, primitiveNode);
                }
            }
            return AbstractCachedDispatchMethodNode.create(frame, argumentCount, lookupMethod);
        } else {
            final ClassObject lookupResultClass = SqueakObjectClassNode.getUncached().executeLookup(lookupResult);
            final Object runWithInMethod = LookupMethodNode.getUncached().executeLookup(lookupResultClass, image.runWithInSelector);
            if (runWithInMethod instanceof final CompiledCodeObject method) {
                return AbstractCachedDispatchObjectAsMethodNode.create(frame, selector, argumentCount, lookupResult, method);
            } else {
                assert runWithInMethod == null : "runWithInMethod should not be another Object";
                return createDNUNode(frame, selector, argumentCount, image, lookupResultClass);
            }
        }
    }

    private static CachedDispatchNode createDNUNode(final VirtualFrame frame, final NativeObject selector, final int argumentCount, final SqueakImageContext image, final ClassObject receiverClass) {
        final Object dnuMethod = LookupMethodNode.getUncached().executeLookup(receiverClass, image.doesNotUnderstand);
        if (dnuMethod instanceof final CompiledCodeObject method) {
            return AbstractCachedDispatchDoesNotUnderstandNode.create(frame, selector, argumentCount, method);
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
        private final PrimitiveFailedCounter failureCounter;

        @Child private AbstractPrimitiveNode primitiveNode;

        private CachedDispatchPrimitiveNode(final int argumentCount, final CompiledCodeObject method, final AbstractPrimitiveNode primitiveNode) {
            super(method);
            this.argumentCount = argumentCount;
            this.primitiveNode = primitiveNode;
            failureCounter = new PrimitiveFailedCounter(primitiveNode);
        }

        @Override
        public Object execute(final VirtualFrame frame) {
            if (failureCounter.getAssumption().isValid()) {
                try {
                    return primitiveNode.execute(frame);
                } catch (final PrimitiveFailed pf) {
                    CompilerDirectives.transferToInterpreter();
                    if (failureCounter.shouldRewriteToCall()) {
                        return execute(frame);
                    } else {
                        return slowPathSendToFallbackCode(frame);
                    }
                }
            } else {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                return replace(AbstractCachedDispatchMethodNode.create(frame, argumentCount, method)).execute(frame);
            }
        }

        private Object slowPathSendToFallbackCode(final VirtualFrame frame) {
            final int stackPointer = FrameAccess.getStackPointer(frame);
            final Object[] receiverAndArguments = new Object[1 + argumentCount];
            final int numArgs = FrameAccess.getNumArguments(frame);
            for (int i = 0; i < receiverAndArguments.length; i++) {
                receiverAndArguments[i] = FrameAccess.getStackValue(frame, stackPointer + i, numArgs);
            }
            return IndirectCallNode.getUncached().call(method.getCallTarget(), FrameAccess.newWith(FrameAccess.getContextOrMarkerSlow(frame), null, receiverAndArguments));
        }
    }

    protected abstract static class AbstractCachedDispatchMethodNode extends CachedDispatchWithDirectCallNode {
        @Children protected FrameStackReadNode[] receiverAndArgumentsNodes;

        private AbstractCachedDispatchMethodNode(final VirtualFrame frame, final int argumentCount, final CompiledCodeObject method) {
            super(method);
            receiverAndArgumentsNodes = new FrameStackReadNode[1 + argumentCount];
            final int stackPointer = FrameAccess.getStackPointer(frame);
            for (int i = 0; i < receiverAndArgumentsNodes.length; i++) {
                receiverAndArgumentsNodes[i] = insert(FrameStackReadNode.create(frame, stackPointer + i, true));
            }
        }

        protected static AbstractCachedDispatchMethodNode create(final VirtualFrame frame, final int argumentCount, final CompiledCodeObject method) {
            if (method.getDoesNotNeedSenderAssumption().isValid()) {
                return new CachedDispatchMethodWithoutSenderNode(frame, argumentCount, method);
            } else {
                return new CachedDispatchMethodWithSenderNode(frame, argumentCount, method);
            }
        }

        protected final Object[] createFrameArguments(final VirtualFrame frame, final Object sender) {
            return FrameAccess.newWith(frame, sender, receiverAndArgumentsNodes);
        }
    }

    protected static final class CachedDispatchMethodWithoutSenderNode extends AbstractCachedDispatchMethodNode {
        @Child private GetContextOrMarkerNode getContextOrMarkerNode = GetContextOrMarkerNode.create();

        private CachedDispatchMethodWithoutSenderNode(final VirtualFrame frame, final int argumentCount, final CompiledCodeObject method) {
            super(frame, argumentCount, method);
        }

        @Override
        public Object execute(final VirtualFrame frame) {
            if (method.getDoesNotNeedSenderAssumption().isValid()) {
                return callNode.call(createFrameArguments(frame, getContextOrMarkerNode.execute(frame)));
            } else {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                return replace(new CachedDispatchMethodWithSenderNode(frame, receiverAndArgumentsNodes.length - 1, method)).execute(frame);
            }
        }
    }

    protected static final class CachedDispatchMethodWithSenderNode extends AbstractCachedDispatchMethodNode {
        @Child private GetOrCreateContextNode getOrCreateContextNode = GetOrCreateContextNode.create();

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
            if (method.getDoesNotNeedSenderAssumption().isValid()) {
                return callNode.call(createFrameArgumentsForDNUNode.execute(frame, getContextOrMarkerNode.execute(frame)));
            } else {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                return replace(new CachedDispatchDoesNotUnderstandWithSenderNode(frame, createFrameArgumentsForDNUNode.getSelector(), createFrameArgumentsForDNUNode.getArgumentCount(),
                                method)).execute(frame);
            }
        }
    }

    protected static final class CachedDispatchDoesNotUnderstandWithSenderNode extends AbstractCachedDispatchDoesNotUnderstandNode {
        @Child private GetOrCreateContextNode getOrCreateContextNode = GetOrCreateContextNode.create();

        private CachedDispatchDoesNotUnderstandWithSenderNode(final VirtualFrame frame, final NativeObject selector, final int argumentCount, final CompiledCodeObject method) {
            super(frame, selector, argumentCount, method);
        }

        @Override
        public Object execute(final VirtualFrame frame) {
            return callNode.call(createFrameArgumentsForDNUNode.execute(frame, getOrCreateContextNode.executeGet(frame)));
        }
    }

    protected abstract static class AbstractCachedDispatchObjectAsMethodNode extends CachedDispatchWithDirectCallNode {
        protected final Object object;

        @Child protected CreateFrameArgumentsForOAMNode createFrameArgumentsForOAMNode;

        private AbstractCachedDispatchObjectAsMethodNode(final VirtualFrame frame, final NativeObject selector, final int argumentCount, final Object object, final CompiledCodeObject method) {
            super(method);
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
            if (method.getDoesNotNeedSenderAssumption().isValid()) {
                return callNode.call(createFrameArgumentsForOAMNode.execute(frame, object, getContextOrMarkerNode.execute(frame)));
            } else {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                return replace(new CachedDispatchObjectAsMethodWithSenderNode(frame, createFrameArgumentsForOAMNode.getSelector(), createFrameArgumentsForOAMNode.getArgumentCount(), object,
                                method)).execute(frame);
            }
        }
    }

    protected static final class CachedDispatchObjectAsMethodWithSenderNode extends AbstractCachedDispatchObjectAsMethodNode {
        @Child private GetOrCreateContextNode getOrCreateContextNode = GetOrCreateContextNode.create();

        private CachedDispatchObjectAsMethodWithSenderNode(final VirtualFrame frame, final NativeObject selector, final int argumentCount, final Object object, final CompiledCodeObject method) {
            super(frame, selector, argumentCount, object, method);
        }

        @Override
        public Object execute(final VirtualFrame frame) {
            return callNode.call(createFrameArgumentsForOAMNode.execute(frame, object, getOrCreateContextNode.executeGet(frame)));
        }
    }
}
