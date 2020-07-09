/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.LookupGuard;
import de.hpi.swa.trufflesqueak.nodes.LookupMethodNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackPopNNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackPopNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetOrCreateContextNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchEagerlyFromStackNode.PrimitiveFailedCounter;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelfSendNodeGen.CreateFrameArgumentsForIndirectCallNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.MethodCacheEntry;

@ReportPolymorphism
@ImportStatic(PrimitiveNodeFactory.class)
public abstract class DispatchSelfSendNode extends AbstractNode {
    protected static final int INLINE_CACHE_SIZE = 6;

    protected final CompiledCodeObject code;
    protected final NativeObject selector;
    protected final int argumentCount;

    @Child protected FrameSlotReadNode peekReceiverNode;

    public DispatchSelfSendNode(final CompiledCodeObject code, final NativeObject selector, final int argumentCount) {
        this.code = code;
        this.selector = selector;
        this.argumentCount = argumentCount;
    }

    public static DispatchSelfSendNode create(final CompiledCodeObject code, final NativeObject selector, final int argumentCount) {
        return DispatchSelfSendNodeGen.create(code, selector, argumentCount);
    }

    public abstract Object execute(VirtualFrame frame);

    @Specialization(guards = {"dispatchNode.guard(getReceiver(frame))"}, //
                    limit = "INLINE_CACHE_SIZE", assumptions = {"dispatchNode.getCallTargetStable()"})
    protected final Object doCached(final VirtualFrame frame,
                    @Cached("createCachedDispatchNode(getReceiver(frame))") final CachedDispatchNode dispatchNode) {
        return dispatchNode.execute(frame, selector);
    }

    @Specialization(replaces = "doCached")
    protected final Object doIndirect(final VirtualFrame frame,
                    @Cached final SqueakObjectClassNode classNode,
                    @Cached final ResolveMethodNode methodNode,
                    @Cached("create(argumentCount)") final CreateFrameArgumentsForIndirectCallNode argumentsNode,
                    @Cached final IndirectCallNode callNode,
                    @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
        final Object receiver = getReceiver(frame);
        final ClassObject receiverClass = classNode.executeLookup(receiver);
        final Object lookupResult = lookupMethod(image, receiverClass, selector);
        final CompiledCodeObject method = methodNode.execute(image, receiverClass, lookupResult);
        return callNode.call(method.getCallTarget(), argumentsNode.execute(frame, lookupResult, receiverClass, method, selector));
    }

    private static Object lookupMethod(final SqueakImageContext image, final ClassObject classObject, final NativeObject selector) {
        final MethodCacheEntry cachedEntry = image.findMethodCacheEntry(classObject, selector);
        if (cachedEntry.getResult() == null) {
            cachedEntry.setResult(classObject.lookupInMethodDictSlow(selector));
        }
        return cachedEntry.getResult(); /* `null` return signals a doesNotUnderstand. */
    }

    protected abstract static class CreateFrameArgumentsForIndirectCallNode extends AbstractNode {
        protected final int argumentCount;

        protected CreateFrameArgumentsForIndirectCallNode(final int argumentCount) {
            this.argumentCount = argumentCount;
        }

        protected static CreateFrameArgumentsForIndirectCallNode create(final int argumentCount) {
            return CreateFrameArgumentsForIndirectCallNodeGen.create(argumentCount);
        }

        protected abstract Object[] execute(VirtualFrame frame, Object lookupResult, ClassObject receiverClass, CompiledCodeObject method, NativeObject cachedSelector);

        @Specialization
        @SuppressWarnings("unused")
        protected static final Object[] doMethod(final VirtualFrame frame, @SuppressWarnings("unused") final CompiledCodeObject lookupResult,
                        final ClassObject receiverClass, final CompiledCodeObject method, final NativeObject cachedSelector,
                        @Cached("getStackPointerSlot(frame)") final FrameSlot stackPointerSlot,
                        @Cached("subtract(getStackPointerSlow(frame), add(1, argumentCount))") final int newStackPointer,
                        @Cached("createReceiverAndArgumentsNodes(frame, newStackPointer, argumentCount)") final FrameSlotReadNode[] receiverAndArgumentsNodes,
                        @Cached("create(true)") final GetOrCreateContextNode getOrCreateContextNode) {
            FrameAccess.setStackPointer(frame, stackPointerSlot, newStackPointer);
            return FrameAccess.newWith(frame, method, getOrCreateContextNode.executeGet(frame), null, receiverAndArgumentsNodes);
        }

        protected static final FrameSlotReadNode[] createReceiverAndArgumentsNodes(final VirtualFrame frame, final int newStackPointer, final int argumentCount) {
            final FrameSlotReadNode[] receiverAndArgumentsNodes = new FrameSlotReadNode[1 + argumentCount];
            assert newStackPointer >= 0 : "Bad stack pointer";
            for (int i = 0; i < receiverAndArgumentsNodes.length; i++) {
                receiverAndArgumentsNodes[i] = FrameSlotReadNode.create(frame, newStackPointer + i);
            }
            return receiverAndArgumentsNodes;
        }

        @Specialization(guards = "lookupResult == null")
        protected static final Object[] doDoesNotUnderstand(final VirtualFrame frame, @SuppressWarnings("unused") final Object lookupResult,
                        final ClassObject receiverClass, final CompiledCodeObject method, final NativeObject cachedSelector,
                        @Cached final AbstractPointersObjectWriteNode writeNode,
                        @Shared("popArgumentsNode") @Cached("create(argumentCount)") final FrameStackPopNNode popArgumentsNode,
                        @Shared("popReceiverNode") @Cached final FrameStackPopNode popReceiverNode,
                        @Cached("create(true)") final GetOrCreateContextNode getOrCreateContextNode,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            final Object[] arguments = popArgumentsNode.execute(frame);
            final Object receiver = popReceiverNode.execute(frame);
            final PointersObject message = image.newMessage(writeNode, cachedSelector, receiverClass, arguments);
            return FrameAccess.newDNUWith(method, getOrCreateContextNode.executeGet(frame), receiver, message);
        }

        @Specialization(guards = {"targetObject != null", "!isCompiledCodeObject(targetObject)"})
        protected static final Object[] doObjectAsMethod(final VirtualFrame frame, final Object targetObject,
                        @SuppressWarnings("unused") final ClassObject receiverClass, final CompiledCodeObject method, final NativeObject cachedSelector,
                        @Shared("popArgumentsNode") @Cached("create(argumentCount)") final FrameStackPopNNode popArgumentsNode,
                        @Shared("popReceiverNode") @Cached final FrameStackPopNode popReceiverNode,
                        @Cached("create(true)") final GetOrCreateContextNode getOrCreateContextNode) {
            final Object[] arguments = popArgumentsNode.execute(frame);
            final Object receiver = popReceiverNode.execute(frame);
            return FrameAccess.newOAMWith(method, getOrCreateContextNode.executeGet(frame), targetObject, cachedSelector, method.image.asArrayOfObjects(arguments), receiver);
        }
    }

    protected abstract static class ResolveMethodNode extends AbstractNode {

        protected abstract CompiledCodeObject execute(SqueakImageContext image, ClassObject receiverClass, Object lookupResult);

        @Specialization
        @SuppressWarnings("unused")
        protected static final CompiledCodeObject doMethod(final SqueakImageContext image, final ClassObject receiverClass, final CompiledCodeObject method) {
            return method;
        }

        @Specialization(guards = "lookupResult == null")
        protected static final CompiledCodeObject doDoesNotUnderstand(final SqueakImageContext image, final ClassObject receiverClass, @SuppressWarnings("unused") final Object lookupResult) {
            final Object dnuMethod = lookupMethod(image, receiverClass, image.doesNotUnderstand);
            if (dnuMethod instanceof CompiledCodeObject) {
                return (CompiledCodeObject) dnuMethod;
            } else {
                throw SqueakException.create("Unable to find DNU method in", receiverClass);
            }
        }

        @Specialization(guards = {"targetObject != null", "!isCompiledCodeObject(targetObject)"})
        protected static final CompiledCodeObject doObjectAsMethod(final SqueakImageContext image, @SuppressWarnings("unused") final ClassObject receiverClass, final Object targetObject,
                        @Cached final SqueakObjectClassNode classNode) {
            final ClassObject targetObjectClass = classNode.executeLookup(targetObject);
            final Object runWithInMethod = lookupMethod(image, targetObjectClass, image.runWithInSelector);
            if (runWithInMethod instanceof CompiledCodeObject) {
                return (CompiledCodeObject) runWithInMethod;
            } else {
                throw SqueakException.create("Add support for DNU on runWithIn");
            }
        }
    }

    protected final CachedDispatchNode createCachedDispatchNode(final Object receiver) {
        final LookupGuard guard = LookupGuard.create(receiver);
        final ClassObject receiverClass = SqueakObjectClassNode.getUncached().executeLookup(receiver);
        final Object lookupResult = LookupMethodNode.getUncached().executeLookup(receiverClass, selector);
        if (lookupResult == null) {
            final Object dnuMethod = LookupMethodNode.getUncached().executeLookup(receiverClass, SqueakLanguage.getContext().doesNotUnderstand);
            if (dnuMethod instanceof CompiledCodeObject) {
                return new CachedDispatchDoesNotUnderstandNode(guard, (CompiledCodeObject) dnuMethod, argumentCount);
            } else {
                throw SqueakException.create("Unable to find DNU method in", receiverClass);
            }
        } else if (lookupResult instanceof CompiledCodeObject) {
            final CompiledCodeObject method = (CompiledCodeObject) lookupResult;
            if (method.hasPrimitive()) {
                final AbstractPrimitiveNode primitiveNode = PrimitiveNodeFactory.forIndex(method, true, method.primitiveIndex());
                if (primitiveNode != null) {
                    return new CachedDispatchPrimitiveNode(guard, method, argumentCount, primitiveNode);
                }
            }
            return new CachedDispatchMethodNode(guard, method, argumentCount);
        } else {
            final ClassObject lookupResultClass = SqueakObjectClassNode.getUncached().executeLookup(lookupResult);
            final Object runWithInMethod = LookupMethodNode.getUncached().executeLookup(lookupResultClass, selector.image.runWithInSelector);
            if (runWithInMethod instanceof CompiledCodeObject) {
                return new CachedDispatchObjectAsMethodNode(guard, lookupResult, (CompiledCodeObject) runWithInMethod, argumentCount);
            } else {
                throw SqueakException.create("Add support for DNU on runWithIn");
            }
        }
    }

    protected final Object getReceiver(final VirtualFrame frame) {
        if (peekReceiverNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            final int stackPointer = FrameAccess.getStackPointer(frame, code) - 1 - argumentCount;
            peekReceiverNode = insert(FrameSlotReadNode.create(code.getStackSlot(stackPointer)));
        }
        return peekReceiverNode.executeRead(frame);
    }

    protected abstract static class CachedDispatchNode extends AbstractNode {
        protected final LookupGuard guard;
        protected final CompiledCodeObject cachedMethod;

        public CachedDispatchNode(final LookupGuard guard, final CompiledCodeObject cachedMethod) {
            this.guard = guard;
            this.cachedMethod = cachedMethod;
        }

        protected final boolean guard(final Object receiver) {
            return guard.check(receiver);
        }

        protected final Assumption getCallTargetStable() {
            return cachedMethod.getCallTargetStable();
        }

        protected abstract Object execute(VirtualFrame frame, NativeObject cachedSelector);
    }

    protected static final class CachedDispatchPrimitiveNode extends CachedDispatchNode {
        private final int argumentCount;
        private final PrimitiveFailedCounter failureCounter = new PrimitiveFailedCounter();
        @CompilationFinal FrameSlot stackPointerSlot;
        @CompilationFinal int stackPointer = -1;

        @Child private AbstractPrimitiveNode primitiveNode;

        private CachedDispatchPrimitiveNode(final LookupGuard guard, final CompiledCodeObject primitiveMethod, final int argumentCount, final AbstractPrimitiveNode primitiveNode) {
            super(guard, primitiveMethod);
            this.argumentCount = argumentCount;
            this.primitiveNode = primitiveNode;
        }

        @Override
        public Object execute(final VirtualFrame frame, final NativeObject cachedSelector) {
            /**
             * Pretend that values are popped off the stack. Primitive nodes will read them using
             * ArgumentOnStackNodes.
             */
            FrameAccess.setStackPointer(frame, getStackPointerSlot(frame), getStackPointer(frame) - 1 - argumentCount);
            try {
                return primitiveNode.executePrimitive(frame);
            } catch (final PrimitiveFailed pf) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                // Restore stackPointer.
                FrameAccess.setStackPointer(frame, stackPointerSlot, stackPointer);
                if (failureCounter.shouldNoLongerSendEagerly()) {
                    return replace(new CachedDispatchMethodNode(guard, cachedMethod, argumentCount)).execute(frame, cachedSelector);
                } else {
                    // Slow path send to fallback code.
                    final Object[] receiverAndArguments = FrameStackPopNNode.create(1 + argumentCount).execute(frame);
                    return IndirectCallNode.getUncached().call(cachedMethod.getCallTarget(),
                                    FrameAccess.newWith(cachedMethod, FrameAccess.getContextOrMarkerSlow(frame), null, receiverAndArguments));
                }
            }
        }

        private FrameSlot getStackPointerSlot(final VirtualFrame frame) {
            if (stackPointerSlot == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                stackPointerSlot = FrameAccess.getStackPointerSlot(frame);
            }
            return stackPointerSlot;
        }

        private int getStackPointer(final VirtualFrame frame) {
            if (stackPointer == -1) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                stackPointer = FrameAccess.getStackPointerSlow(frame);
            }
            return stackPointer;
        }
    }

    protected static final class CachedDispatchMethodNode extends CachedDispatchNode {
        @Child public DirectCallNode callNode;
        @Child protected CreateFrameArgumentsNode createFrameArgumentsNode;
        @Child private GetOrCreateContextNode getOrCreateContextNode = GetOrCreateContextNode.create(true);

        private CachedDispatchMethodNode(final LookupGuard guard, final CompiledCodeObject method, final int argumentCount) {
            super(guard, method);
            callNode = DirectCallNode.create(cachedMethod.getCallTarget());
            createFrameArgumentsNode = CreateFrameArgumentsNode.create(argumentCount);
        }

        @Override
        public Object execute(final VirtualFrame frame, final NativeObject cachedSelector) {
            return callNode.call(createFrameArgumentsNode.execute(frame, cachedMethod, getOrCreateContextNode.executeGet(frame)));
        }
    }

    protected static final class CachedDispatchDoesNotUnderstandNode extends CachedDispatchNode {
        @Child public DirectCallNode callNode;
        @Child protected CreateFrameArgumentsForDNUNode createFrameArgumentsForDNUNode;
        @Child private GetOrCreateContextNode getOrCreateContextNode = GetOrCreateContextNode.create(true);

        private CachedDispatchDoesNotUnderstandNode(final LookupGuard guard, final CompiledCodeObject method, final int argumentCount) {
            super(guard, method);
            callNode = DirectCallNode.create(cachedMethod.getCallTarget());
            createFrameArgumentsForDNUNode = CreateFrameArgumentsForDNUNode.create(argumentCount);
        }

        @Override
        public Object execute(final VirtualFrame frame, final NativeObject cachedSelector) {
            return callNode.call(createFrameArgumentsForDNUNode.execute(frame, cachedSelector, cachedMethod, getOrCreateContextNode.executeGet(frame)));
        }
    }

    protected static final class CachedDispatchObjectAsMethodNode extends CachedDispatchNode {
        public final Object cachedObject;
        @Child public DirectCallNode callNode;
        @Child protected CreateFrameArgumentsForOAMNode createFrameArgumentsForOAMNode;
        @Child private GetOrCreateContextNode getOrCreateContextNode = GetOrCreateContextNode.create(true);

        private CachedDispatchObjectAsMethodNode(final LookupGuard guard, final Object object, final CompiledCodeObject method, final int argumentCount) {
            super(guard, method);
            cachedObject = object;
            callNode = DirectCallNode.create(cachedMethod.getCallTarget());
            createFrameArgumentsForOAMNode = CreateFrameArgumentsForOAMNode.create(argumentCount);
        }

        @Override
        public Object execute(final VirtualFrame frame, final NativeObject cachedSelector) {
            return callNode.call(createFrameArgumentsForOAMNode.execute(frame, cachedSelector, cachedObject, cachedMethod, getOrCreateContextNode.executeGet(frame)));
        }
    }

    protected static final DispatchIndirectNode createDispatchIndirectData(final Object receiver, final NativeObject selector, final int argumentCount) {
        return DispatchIndirectNode.create(receiver, selector, argumentCount);
    }

    protected static final class DispatchIndirectNode extends Node {
        public final LookupGuard guard;
        public final CompiledCodeObject cachedMethod;
        @Child public DirectCallNode callNode;
        @Child protected CreateFrameArgumentsNode createFrameArgumentsNode;
        @Child private GetOrCreateContextNode getOrCreateContextNode = GetOrCreateContextNode.create(true);

        private DispatchIndirectNode(final LookupGuard guard, final CompiledCodeObject primitiveMethod, final int argumentCount) {
            this.guard = guard;
            cachedMethod = primitiveMethod;
            callNode = DirectCallNode.create(cachedMethod.getCallTarget());
            createFrameArgumentsNode = CreateFrameArgumentsNode.create(argumentCount);
        }

        public Object execute(final VirtualFrame frame) {
            return callNode.call(createFrameArgumentsNode.execute(frame, cachedMethod, getOrCreateContextNode.executeGet(frame)));
        }

        protected static DispatchIndirectNode create(final Object receiver, final NativeObject selector, final int argumentCount) {
            final LookupGuard guard = LookupGuard.create(receiver);
            final ClassObject receiverClass = SqueakObjectClassNode.getUncached().executeLookup(receiver);
            final Object lookupResult = LookupMethodNode.getUncached().executeLookup(receiverClass, selector);
            if (!(lookupResult instanceof CompiledCodeObject)) {
                return null; /* Not a method. */
            }
            final CompiledCodeObject method = (CompiledCodeObject) lookupResult;
            return new DispatchIndirectNode(guard, method, argumentCount);
        }
    }

    public static final class CreateFrameArgumentsNode extends AbstractNode {
        @CompilationFinal private FrameSlot stackPointerSlot;
        @CompilationFinal private int stackPointer;
        @Children private FrameSlotReadNode[] receiverAndArgumentsNodes;

        private CreateFrameArgumentsNode(final int argumentCount) {
            receiverAndArgumentsNodes = new FrameSlotReadNode[1 + argumentCount];
        }

        public static CreateFrameArgumentsNode create(final int argumentCount) {
            return new CreateFrameArgumentsNode(argumentCount);
        }

        public Object[] execute(final VirtualFrame frame, final CompiledCodeObject method, final Object sender) {
            if (stackPointerSlot == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                stackPointerSlot = FrameAccess.getStackPointerSlot(frame);
                stackPointer = FrameAccess.getStackPointer(frame, stackPointerSlot) - receiverAndArgumentsNodes.length;
                assert stackPointer >= 0 : "Bad stack pointer";
                for (int i = 0; i < receiverAndArgumentsNodes.length; i++) {
                    receiverAndArgumentsNodes[i] = insert(FrameSlotReadNode.create(frame, stackPointer + i));
                }
            }
            FrameAccess.setStackPointer(frame, stackPointerSlot, stackPointer);
            return FrameAccess.newWith(frame, method, sender, null, receiverAndArgumentsNodes);
        }
    }

    public static final class CreateFrameArgumentsForDNUNode extends AbstractNode {
        @Child private FrameStackPopNNode popArguments;
        @Child private FrameStackPopNode popReceiver = FrameStackPopNode.create();
        @Child private AbstractPointersObjectWriteNode writeNode = AbstractPointersObjectWriteNode.create();
        @Child private SqueakObjectClassNode classNode = SqueakObjectClassNode.create();

        private CreateFrameArgumentsForDNUNode(final int argumentCount) {
            popArguments = FrameStackPopNNode.create(argumentCount);
        }

        public static CreateFrameArgumentsForDNUNode create(final int argumentCount) {
            return new CreateFrameArgumentsForDNUNode(argumentCount);
        }

        public Object[] execute(final VirtualFrame frame, final NativeObject cachedSelector, final CompiledCodeObject method, final Object sender) {
            final Object[] arguments = popArguments.execute(frame);
            final Object receiver = popReceiver.execute(frame);
            final ClassObject receiverClass = classNode.executeLookup(receiver);
            final PointersObject message = method.image.newMessage(writeNode, cachedSelector, receiverClass, arguments);
            return FrameAccess.newDNUWith(method, sender, receiver, message);
        }
    }

    public static final class CreateFrameArgumentsForOAMNode extends AbstractNode {
        @Child private FrameStackPopNNode popArguments;
        @Child private FrameStackPopNode popReceiver = FrameStackPopNode.create();

        private CreateFrameArgumentsForOAMNode(final int argumentCount) {
            popArguments = FrameStackPopNNode.create(argumentCount);
        }

        public static CreateFrameArgumentsForOAMNode create(final int argumentCount) {
            return new CreateFrameArgumentsForOAMNode(argumentCount);
        }

        public Object[] execute(final VirtualFrame frame, final NativeObject cachedSelector, final Object cachedObject, final CompiledCodeObject method, final Object sender) {
            final Object[] arguments = popArguments.execute(frame);
            final Object receiver = popReceiver.execute(frame);
            return FrameAccess.newOAMWith(method, sender, cachedObject, cachedSelector, method.image.asArrayOfObjects(arguments), receiver);
        }
    }
}
