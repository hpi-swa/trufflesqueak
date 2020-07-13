/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
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
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodes.AbstractSelfSendNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackPopNNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetContextOrMarkerNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetOrCreateContextNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.CreateFrameArgumentNodes.CreateFrameArgumentsForDNUNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.CreateFrameArgumentNodes.CreateFrameArgumentsForIndirectCallNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.CreateFrameArgumentNodes.CreateFrameArgumentsForOAMNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.CreateFrameArgumentNodes.CreateFrameArgumentsNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.MethodCacheEntry;
import de.hpi.swa.trufflesqueak.util.PrimitiveFailedCounter;

@ReportPolymorphism
@ImportStatic(AbstractSelfSendNode.class)
public abstract class DispatchLookupResultNode extends AbstractNode {
    protected final CompiledCodeObject code;
    protected final NativeObject selector;
    protected final int argumentCount;

    public DispatchLookupResultNode(final CompiledCodeObject code, final NativeObject selector, final int argumentCount) {
        this.code = code;
        this.selector = selector;
        this.argumentCount = argumentCount;
    }

    public static DispatchLookupResultNode create(final CompiledCodeObject code, final NativeObject selector, final int argumentCount) {
        return DispatchLookupResultNodeGen.create(code, selector, argumentCount);
    }

    public abstract Object execute(VirtualFrame frame, ClassObject receiverClass, Object lookupResult);

    @SuppressWarnings("unused")
    @Specialization(guards = "lookupResult == cachedLookupResult", limit = "INLINE_CACHE_SIZE", assumptions = {"dispatchNode.getCallTargetStable()"})
    protected final Object doCached(final VirtualFrame frame, final ClassObject receiverClass, final Object lookupResult,
                    @Cached("lookupResult") final Object cachedLookupResult,
                    @Cached("create(argumentCount, receiverClass, lookupResult)") final CachedSelfDispatchNode dispatchNode) {
        return dispatchNode.execute(frame, selector);
    }

    @Specialization(replaces = "doCached")
    protected final Object doIndirect(final VirtualFrame frame, final ClassObject receiverClass, final Object lookupResult,
                    @Cached final ResolveMethodNode methodNode,
                    @Cached("create(argumentCount)") final CreateFrameArgumentsForIndirectCallNode argumentsNode,
                    @Cached final IndirectCallNode callNode,
                    @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
        final CompiledCodeObject method = methodNode.execute(image, receiverClass, lookupResult);
        return callNode.call(method.getCallTarget(), argumentsNode.execute(frame, lookupResult, receiverClass, method, selector));
    }

    @ReportPolymorphism
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

        private static Object lookupMethod(final SqueakImageContext image, final ClassObject classObject, final NativeObject selector) {
            final MethodCacheEntry cachedEntry = image.findMethodCacheEntry(classObject, selector);
            if (cachedEntry.getResult() == null) {
                cachedEntry.setResult(classObject.lookupInMethodDictSlow(selector));
            }
            return cachedEntry.getResult(); /* `null` return signals a doesNotUnderstand. */
        }
    }

    protected abstract static class CachedSelfDispatchNode extends AbstractNode {
        protected final int argumentCount;
        protected final CompiledCodeObject method;

        public CachedSelfDispatchNode(final int argumentCount, final CompiledCodeObject method) {
            this.argumentCount = argumentCount;
            this.method = method;
            assert getCallTargetStable().isValid() : "callTargetStable must be valid";
        }

        protected static final CachedSelfDispatchNode create(final int argumentCount, final ClassObject receiverClass, final Object lookupResult) {
            if (lookupResult == null) {
                final Object dnuMethod = LookupMethodNode.getUncached().executeLookup(receiverClass, SqueakLanguage.getContext().doesNotUnderstand);
                if (dnuMethod instanceof CompiledCodeObject) {
                    return AbstractCachedDispatchDoesNotUnderstandNode.create(argumentCount, (CompiledCodeObject) dnuMethod);
                } else {
                    throw SqueakException.create("Unable to find DNU method in", receiverClass);
                }
            } else if (lookupResult instanceof CompiledCodeObject) {
                final CompiledCodeObject lookupMethod = (CompiledCodeObject) lookupResult;
                if (lookupMethod.hasPrimitive()) {
                    final AbstractPrimitiveNode primitiveNode = PrimitiveNodeFactory.forIndex(lookupMethod, true, lookupMethod.primitiveIndex());
                    if (primitiveNode != null) {
                        return new CachedDispatchPrimitiveNode(argumentCount, lookupMethod, primitiveNode);
                    }
                }
                return AbstractCachedDispatchMethodNode.create(argumentCount, lookupMethod);
            } else {
                final ClassObject lookupResultClass = SqueakObjectClassNode.getUncached().executeLookup(lookupResult);
                final Object runWithInMethod = LookupMethodNode.getUncached().executeLookup(lookupResultClass, receiverClass.image.runWithInSelector);
                if (runWithInMethod instanceof CompiledCodeObject) {
                    return AbstractCachedDispatchObjectAsMethodNode.create(argumentCount, lookupResult, (CompiledCodeObject) runWithInMethod);
                } else {
                    throw SqueakException.create("Add support for DNU on runWithIn");
                }
            }
        }

        protected final Object peekReceiverSlow(final VirtualFrame frame) {
            CompilerAsserts.neverPartOfCompilation();
            final CompiledCodeObject code = FrameAccess.getBlockOrMethod(frame);
            final int stackPointer = FrameAccess.getStackPointer(frame, code) - 1 - argumentCount;
            return frame.getValue(code.getStackSlot(stackPointer));
        }

        protected final Assumption getCallTargetStable() {
            return method.getCallTargetStable();
        }

        protected abstract Object execute(VirtualFrame frame, NativeObject cachedSelector);
    }

    protected abstract static class CachedDispatchWithDirectCallNode extends CachedSelfDispatchNode {
        @Child protected DirectCallNode callNode;

        public CachedDispatchWithDirectCallNode(final int argumentCount, final CompiledCodeObject method) {
            super(argumentCount, method);
            callNode = DirectCallNode.create(method.getCallTarget());
        }
    }

    protected static final class CachedDispatchPrimitiveNode extends CachedSelfDispatchNode {
        private final PrimitiveFailedCounter failureCounter = new PrimitiveFailedCounter();
        @CompilationFinal FrameSlot stackPointerSlot;
        @CompilationFinal int stackPointer = -1;

        @Child private AbstractPrimitiveNode primitiveNode;

        private CachedDispatchPrimitiveNode(final int argumentCount, final CompiledCodeObject method, final AbstractPrimitiveNode primitiveNode) {
            super(argumentCount, method);
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
                    return replace(AbstractCachedDispatchMethodNode.create(argumentCount, method)).execute(frame, cachedSelector);
                } else {
                    // Slow path send to fallback code.
                    final Object[] receiverAndArguments = FrameStackPopNNode.create(1 + argumentCount).execute(frame);
                    return IndirectCallNode.getUncached().call(method.getCallTarget(),
                                    FrameAccess.newWith(method, FrameAccess.getContextOrMarkerSlow(frame), null, receiverAndArguments));
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

    protected abstract static class AbstractCachedDispatchMethodNode extends CachedDispatchWithDirectCallNode {
        @Child protected CreateFrameArgumentsNode createFrameArgumentsNode;

        private AbstractCachedDispatchMethodNode(final int argumentCount, final CompiledCodeObject method) {
            super(argumentCount, method);
            createFrameArgumentsNode = CreateFrameArgumentsNode.create(argumentCount);
        }

        protected static AbstractCachedDispatchMethodNode create(final int argumentCount, final CompiledCodeObject method) {
            if (method.getDoesNotNeedSenderAssumption().isValid()) {
                return new CachedDispatchMethodWithoutSenderNode(argumentCount, method);
            } else {
                return new CachedDispatchMethodWithSenderNode(argumentCount, method);
            }
        }
    }

    protected static final class CachedDispatchMethodWithoutSenderNode extends AbstractCachedDispatchMethodNode {
        @Child private GetContextOrMarkerNode getContextOrMarkerNode = GetContextOrMarkerNode.create();

        private CachedDispatchMethodWithoutSenderNode(final int argumentCount, final CompiledCodeObject method) {
            super(argumentCount, method);
        }

        @Override
        public Object execute(final VirtualFrame frame, final NativeObject cachedSelector) {
            try {
                method.getDoesNotNeedSenderAssumption().check();
            } catch (final InvalidAssumptionException e) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                return replace(new CachedDispatchMethodWithSenderNode(argumentCount, method)).execute(frame, cachedSelector);
            }
            return callNode.call(createFrameArgumentsNode.execute(frame, method, getContextOrMarkerNode.execute(frame)));
        }
    }

    protected static final class CachedDispatchMethodWithSenderNode extends AbstractCachedDispatchMethodNode {
        @Child private GetOrCreateContextNode getOrCreateContextNode = GetOrCreateContextNode.create(true);

        private CachedDispatchMethodWithSenderNode(final int argumentCount, final CompiledCodeObject method) {
            super(argumentCount, method);
        }

        @Override
        public Object execute(final VirtualFrame frame, final NativeObject cachedSelector) {
            return callNode.call(createFrameArgumentsNode.execute(frame, method, getOrCreateContextNode.executeGet(frame)));
        }
    }

    protected abstract static class AbstractCachedDispatchDoesNotUnderstandNode extends CachedDispatchWithDirectCallNode {
        @Child protected CreateFrameArgumentsForDNUNode createFrameArgumentsForDNUNode;

        private AbstractCachedDispatchDoesNotUnderstandNode(final int argumentCount, final CompiledCodeObject method) {
            super(argumentCount, method);
            createFrameArgumentsForDNUNode = CreateFrameArgumentsForDNUNode.create(argumentCount);
        }

        protected static AbstractCachedDispatchDoesNotUnderstandNode create(final int argumentCount, final CompiledCodeObject method) {
            if (method.getDoesNotNeedSenderAssumption().isValid()) {
                return new CachedDispatchDoesNotUnderstandWithoutSenderNode(argumentCount, method);
            } else {
                return new CachedDispatchDoesNotUnderstandWithSenderNode(argumentCount, method);
            }
        }
    }

    protected static final class CachedDispatchDoesNotUnderstandWithoutSenderNode extends AbstractCachedDispatchDoesNotUnderstandNode {
        @Child private GetContextOrMarkerNode getContextOrMarkerNode = GetContextOrMarkerNode.create();

        private CachedDispatchDoesNotUnderstandWithoutSenderNode(final int argumentCount, final CompiledCodeObject method) {
            super(argumentCount, method);
        }

        @Override
        public Object execute(final VirtualFrame frame, final NativeObject cachedSelector) {
            try {
                method.getDoesNotNeedSenderAssumption().check();
            } catch (final InvalidAssumptionException e) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                return replace(new CachedDispatchDoesNotUnderstandWithSenderNode(argumentCount, method)).execute(frame, cachedSelector);
            }
            return callNode.call(createFrameArgumentsForDNUNode.execute(frame, cachedSelector, method, getContextOrMarkerNode.execute(frame)));
        }
    }

    protected static final class CachedDispatchDoesNotUnderstandWithSenderNode extends AbstractCachedDispatchDoesNotUnderstandNode {
        @Child private GetOrCreateContextNode getOrCreateContextNode = GetOrCreateContextNode.create(true);

        private CachedDispatchDoesNotUnderstandWithSenderNode(final int argumentCount, final CompiledCodeObject method) {
            super(argumentCount, method);
        }

        @Override
        public Object execute(final VirtualFrame frame, final NativeObject cachedSelector) {
            return callNode.call(createFrameArgumentsForDNUNode.execute(frame, cachedSelector, method, getOrCreateContextNode.executeGet(frame)));
        }
    }

    protected abstract static class AbstractCachedDispatchObjectAsMethodNode extends CachedDispatchWithDirectCallNode {
        protected final Object object;
        @Child protected CreateFrameArgumentsForOAMNode createFrameArgumentsForOAMNode;

        private AbstractCachedDispatchObjectAsMethodNode(final int argumentCount, final Object object, final CompiledCodeObject method) {
            super(argumentCount, method);
            this.object = object;
            createFrameArgumentsForOAMNode = CreateFrameArgumentsForOAMNode.create(argumentCount);
        }

        public static CachedSelfDispatchNode create(final int argumentCount, final Object lookupResult, final CompiledCodeObject runWithInMethod) {
            if (runWithInMethod.getDoesNotNeedSenderAssumption().isValid()) {
                return new CachedDispatchObjectAsMethodWithoutSenderNode(argumentCount, lookupResult, runWithInMethod);
            } else {
                return new CachedDispatchObjectAsMethodWithSenderNode(argumentCount, lookupResult, runWithInMethod);
            }
        }
    }

    protected static final class CachedDispatchObjectAsMethodWithoutSenderNode extends AbstractCachedDispatchObjectAsMethodNode {
        @Child private GetContextOrMarkerNode getContextOrMarkerNode = GetContextOrMarkerNode.create();

        private CachedDispatchObjectAsMethodWithoutSenderNode(final int argumentCount, final Object object, final CompiledCodeObject method) {
            super(argumentCount, object, method);
        }

        @Override
        public Object execute(final VirtualFrame frame, final NativeObject cachedSelector) {
            try {
                method.getDoesNotNeedSenderAssumption().check();
            } catch (final InvalidAssumptionException e) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                return replace(new CachedDispatchObjectAsMethodWithSenderNode(argumentCount, object, method)).execute(frame, cachedSelector);
            }
            return callNode.call(createFrameArgumentsForOAMNode.execute(frame, cachedSelector, object, method, getContextOrMarkerNode.execute(frame)));
        }
    }

    protected static final class CachedDispatchObjectAsMethodWithSenderNode extends AbstractCachedDispatchObjectAsMethodNode {
        @Child private GetOrCreateContextNode getOrCreateContextNode = GetOrCreateContextNode.create(true);

        private CachedDispatchObjectAsMethodWithSenderNode(final int argumentCount, final Object object, final CompiledCodeObject method) {
            super(argumentCount, object, method);
        }

        @Override
        public Object execute(final VirtualFrame frame, final NativeObject cachedSelector) {
            return callNode.call(createFrameArgumentsForOAMNode.execute(frame, cachedSelector, object, method, getOrCreateContextNode.executeGet(frame)));
        }
    }
}
