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
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.LookupMethodNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackPopNNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackPopNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetOrCreateContextNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchEagerlyFromStackNode.PrimitiveFailedCounter;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

@ReportPolymorphism
@ImportStatic(PrimitiveNodeFactory.class)
public abstract class DispatchSuperSendNode extends AbstractNode {
    protected static final int INLINE_CACHE_SIZE = 6;

    protected final CompiledCodeObject code;
    protected final NativeObject selector;
    protected final int argumentCount;

    public DispatchSuperSendNode(final CompiledCodeObject code, final NativeObject selector, final int argumentCount) {
        this.code = code;
        this.selector = selector;
        this.argumentCount = argumentCount;
    }

    public static DispatchSuperSendNode create(final CompiledCodeObject code, final NativeObject selector, final int argumentCount) {
        return DispatchSuperSendNodeGen.create(code, selector, argumentCount);
    }

    public abstract Object execute(VirtualFrame frame);

    @Specialization(guards = {"code.getMethodClass(readNode) == cachedMethodClass"}, assumptions = {"cachedMethodClass.getClassHierarchyStable()"})
    protected final Object doCached(final VirtualFrame frame,
                    @Cached final AbstractPointersObjectReadNode readNode,
                    @Cached("code.getMethodClassSlow()") final ClassObject cachedMethodClass,
                    @Cached("createCachedDispatchNode(cachedMethodClass)") final CachedDispatchNode dispatchNode) {
        return dispatchNode.execute(frame, selector);
    }

    protected final CachedDispatchNode createCachedDispatchNode(final ClassObject methodClass) {
        final ClassObject superclass = methodClass.getSuperclassOrNull();
        assert superclass != null;
        final Object lookupResult = LookupMethodNode.getUncached().executeLookup(superclass, selector);
        if (lookupResult == null) {
            final Object dnuMethod = LookupMethodNode.getUncached().executeLookup(superclass, SqueakLanguage.getContext().doesNotUnderstand);
            if (dnuMethod instanceof CompiledCodeObject) {
                return new CachedDispatchDoesNotUnderstandNode((CompiledCodeObject) dnuMethod, argumentCount);
            } else {
                throw SqueakException.create("Unable to find DNU method in", superclass);
            }
        } else if (lookupResult instanceof CompiledCodeObject) {
            final CompiledCodeObject method = (CompiledCodeObject) lookupResult;
            if (method.hasPrimitive()) {
                final AbstractPrimitiveNode primitiveNode = PrimitiveNodeFactory.forIndex(method, true, method.primitiveIndex());
                if (primitiveNode != null) {
                    return new CachedDispatchPrimitiveNode(method, argumentCount, primitiveNode);
                }
            }
            return new CachedDispatchMethodNode(method, argumentCount);
        } else {
            final ClassObject lookupResultClass = SqueakObjectClassNode.getUncached().executeLookup(lookupResult);
            final Object runWithInMethod = LookupMethodNode.getUncached().executeLookup(lookupResultClass, selector.image.runWithInSelector);
            if (runWithInMethod instanceof CompiledCodeObject) {
                return new CachedDispatchObjectAsMethodNode(lookupResult, (CompiledCodeObject) runWithInMethod, argumentCount);
            } else {
                throw SqueakException.create("Add support for DNU on runWithIn");
            }
        }
    }

    protected abstract static class CachedDispatchNode extends AbstractNode {
        protected final CompiledCodeObject cachedMethod;

        public CachedDispatchNode(final CompiledCodeObject cachedMethod) {
            this.cachedMethod = cachedMethod;
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

        private CachedDispatchPrimitiveNode(final CompiledCodeObject primitiveMethod, final int argumentCount, final AbstractPrimitiveNode primitiveNode) {
            super(primitiveMethod);
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
                    return replace(new CachedDispatchMethodNode(cachedMethod, argumentCount)).execute(frame, cachedSelector);
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
                stackPointerSlot = getStackPointerSlot(frame);
            }
            return stackPointerSlot;
        }

        private int getStackPointer(final VirtualFrame frame) {
            if (stackPointer == -1) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                stackPointer = getStackPointer(frame);
            }
            return stackPointer;
        }
    }

    protected static final class CachedDispatchMethodNode extends CachedDispatchNode {
        @Child public DirectCallNode callNode;
        @Child protected CreateFrameArgumentsNode createFrameArgumentsNode;
        @Child private GetOrCreateContextNode getOrCreateContextNode = GetOrCreateContextNode.create(true);

        private CachedDispatchMethodNode(final CompiledCodeObject method, final int argumentCount) {
            super(method);
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

        private CachedDispatchDoesNotUnderstandNode(final CompiledCodeObject method, final int argumentCount) {
            super(method);
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

        private CachedDispatchObjectAsMethodNode(final Object object, final CompiledCodeObject method, final int argumentCount) {
            super(method);
            cachedObject = object;
            callNode = DirectCallNode.create(cachedMethod.getCallTarget());
            createFrameArgumentsForOAMNode = CreateFrameArgumentsForOAMNode.create(argumentCount);
        }

        @Override
        public Object execute(final VirtualFrame frame, final NativeObject cachedSelector) {
            return callNode.call(createFrameArgumentsForOAMNode.execute(frame, cachedSelector, cachedObject, cachedMethod, getOrCreateContextNode.executeGet(frame)));
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
        @Child private SqueakObjectClassNode classNode;

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
