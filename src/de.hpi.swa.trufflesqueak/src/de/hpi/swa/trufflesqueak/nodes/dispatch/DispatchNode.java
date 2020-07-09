/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

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
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
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
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

@ReportPolymorphism
@ImportStatic(PrimitiveNodeFactory.class)
public abstract class DispatchNode extends AbstractNode {
    protected static final int INLINE_CACHE_SIZE = 6;

    protected final CompiledCodeObject code;
    protected final NativeObject selector;
    protected final int argumentCount;

    @Child protected FrameSlotReadNode peekReceiverNode;

    public DispatchNode(final CompiledCodeObject code, final NativeObject selector, final int argumentCount) {
        this.code = code;
        this.selector = selector;
        this.argumentCount = argumentCount;
    }

    public static DispatchNode create(final CompiledCodeObject code, final NativeObject selector, final int argumentCount) {
        return DispatchNodeGen.create(code, selector, argumentCount);
    }

    public abstract Object execute(VirtualFrame frame);

    @Specialization(guards = {"data.guard.check(getReceiver(frame))", "data != null"}, //
                    limit = "INLINE_CACHE_SIZE", assumptions = {"data.primitiveMethod.getCallTargetStable()"}, rewriteOn = PrimitiveFailed.class)
    protected final Object doPrimitive(final VirtualFrame frame,
                    @Cached("createDispatchPrimitiveData(getReceiver(frame), selector)") final DispatchPrimitiveNode data,
                    @Cached("getStackPointerSlot(frame)") final FrameSlot stackPointerSlot,
                    @Cached("getStackPointer(frame, stackPointerSlot)") final int stackPointer,
                    @Cached final PrimitiveFailedCounter failureCounter) {
        /**
         * Pretend that values are popped off the stack. Primitive nodes will read them using
         * ArgumentOnStackNodes.
         */
        FrameAccess.setStackPointer(frame, stackPointerSlot, stackPointer - 1 - argumentCount);
        try {
            return data.primitiveNode.executePrimitive(frame);
        } catch (final PrimitiveFailed pf) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            // Restore stackPointer.
            FrameAccess.setStackPointer(frame, stackPointerSlot, stackPointer);
            if (failureCounter.shouldNoLongerSendEagerly()) {
                throw pf; // Rewrite specialization.
            } else {
                // Slow path send to fallback code.
                final Object[] receiverAndArguments = FrameStackPopNNode.create(1 + argumentCount).execute(frame);
                return IndirectCallNode.getUncached().call(data.primitiveMethod.getCallTarget(),
                                FrameAccess.newWith(data.primitiveMethod, FrameAccess.getContextOrMarkerSlow(frame), null, receiverAndArguments));
            }
        }
    }

    @Specialization(guards = {"data.guard.check(getReceiver(frame))", "data != null"}, //
                    limit = "INLINE_CACHE_SIZE", assumptions = {"data.cachedMethod.getCallTargetStable()"})
    protected static final Object doDirect(final VirtualFrame frame,
                    @Cached("createDispatchDirectData(getReceiver(frame), selector, argumentCount)") final DispatchDirectNode data) {
        return data.execute(frame);
    }

    @Specialization(guards = {"data.guard.check(getReceiver(frame))"}, replaces = "doDirect", //
                    limit = "INLINE_CACHE_SIZE", assumptions = {"data.cachedMethod.getCallTargetStable()"})
    protected static final Object doIndirect(final VirtualFrame frame,
                    @Cached("createDispatchIndirectData(getReceiver(frame), selector, argumentCount)") final DispatchIndirectNode data) {
        return data.execute(frame);
    }

    @Specialization(guards = {"data.guard.check(getReceiver(frame))", "data != null"}, //
                    limit = "INLINE_CACHE_SIZE", assumptions = {"data.cachedMethod.getCallTargetStable()"})
    protected final Object doDoesNotUnderstand(final VirtualFrame frame,
                    @Cached("createDispatchDNUData(getReceiver(frame), selector, argumentCount)") final DispatchDNUNode data) {
        return data.execute(frame, selector);
    }

    @Specialization(guards = {"data.guard.check(getReceiver(frame))", "data != null"}, //
                    limit = "INLINE_CACHE_SIZE", assumptions = {"data.cachedMethod.getCallTargetStable()"})
    protected final Object doObjectAsMethod(final VirtualFrame frame,
                    @Cached("createDispatchOAMData(getReceiver(frame), selector, argumentCount)") final DispatchOAMNode data) {
        return data.execute(frame, selector);
    }

    protected final Object getReceiver(final VirtualFrame frame) {
        if (peekReceiverNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            final int stackPointer = FrameAccess.getStackPointer(frame, code) - 1 - argumentCount;
            peekReceiverNode = insert(FrameSlotReadNode.create(code.getStackSlot(stackPointer)));
        }
        return peekReceiverNode.executeRead(frame);
    }

    protected static final DispatchPrimitiveNode createDispatchPrimitiveData(final Object receiver, final NativeObject selector) {
        return DispatchPrimitiveNode.create(receiver, selector);
    }

    protected static final class DispatchPrimitiveNode extends Node {
        public final LookupGuard guard;
        public final CompiledCodeObject primitiveMethod;
        @Child public AbstractPrimitiveNode primitiveNode;

        private DispatchPrimitiveNode(final LookupGuard guard, final CompiledCodeObject primitiveMethod, final AbstractPrimitiveNode primitiveNode) {
            this.guard = guard;
            this.primitiveMethod = primitiveMethod;
            this.primitiveNode = primitiveNode;
        }

        protected static DispatchPrimitiveNode create(final Object receiver, final NativeObject selector) {
            final LookupGuard guard = LookupGuard.create(receiver);
            final ClassObject receiverClass = SqueakObjectClassNode.getUncached().executeLookup(receiver);
            final Object lookupResult = LookupMethodNode.getUncached().executeLookup(receiverClass, selector);
            if (!(lookupResult instanceof CompiledCodeObject || !((CompiledCodeObject) lookupResult).hasPrimitive())) {
                return null; /* Not a primitive method. */
            }
            final CompiledCodeObject primitiveMethod = (CompiledCodeObject) lookupResult;
            final AbstractPrimitiveNode primitiveNode = PrimitiveNodeFactory.forIndex(primitiveMethod, true, primitiveMethod.primitiveIndex());
            if (primitiveNode == null) {
                return null; /* Primitive not found or implemented. */
            }
            return new DispatchPrimitiveNode(guard, primitiveMethod, primitiveNode);
        }

        @Override
        public NodeCost getCost() {
            return NodeCost.NONE;
        }
    }

    protected static final DispatchDirectNode createDispatchDirectData(final Object receiver, final NativeObject selector, final int argumentCount) {
        return DispatchDirectNode.create(receiver, selector, argumentCount);
    }

    protected static final class DispatchDirectNode extends Node {
        public final LookupGuard guard;
        public final CompiledCodeObject cachedMethod;
        @Child public DirectCallNode callNode;
        @Child protected CreateFrameArgumentsNode createFrameArgumentsNode;
        @Child private GetOrCreateContextNode getOrCreateContextNode = GetOrCreateContextNode.create(true);

        private DispatchDirectNode(final LookupGuard guard, final CompiledCodeObject primitiveMethod, final int argumentCount) {
            this.guard = guard;
            cachedMethod = primitiveMethod;
            callNode = DirectCallNode.create(cachedMethod.getCallTarget());
            createFrameArgumentsNode = CreateFrameArgumentsNode.create(argumentCount);
        }

        public Object execute(final VirtualFrame frame) {
            return callNode.call(createFrameArgumentsNode.execute(frame, cachedMethod, getOrCreateContextNode.executeGet(frame)));
        }

        protected static DispatchDirectNode create(final Object receiver, final NativeObject selector, final int argumentCount) {
            final LookupGuard guard = LookupGuard.create(receiver);
            final ClassObject receiverClass = SqueakObjectClassNode.getUncached().executeLookup(receiver);
            final Object lookupResult = LookupMethodNode.getUncached().executeLookup(receiverClass, selector);
            if (!(lookupResult instanceof CompiledCodeObject)) {
                return null; /* Not a method. */
            }
            final CompiledCodeObject method = (CompiledCodeObject) lookupResult;
            return new DispatchDirectNode(guard, method, argumentCount);
        }

        @Override
        public NodeCost getCost() {
            return NodeCost.NONE;
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

        @Override
        public NodeCost getCost() {
            return NodeCost.NONE;
        }
    }

    protected static final DispatchDNUNode createDispatchDNUData(final Object receiver, final NativeObject selector, final int argumentCount) {
        return DispatchDNUNode.create(receiver, selector, argumentCount);
    }

    protected static final class DispatchDNUNode extends Node {
        public final LookupGuard guard;
        public final CompiledCodeObject cachedMethod;
        @Child public DirectCallNode callNode;
        @Child protected CreateFrameArgumentsForDNUNode createFrameArgumentsForDNUNode;
        @Child private GetOrCreateContextNode getOrCreateContextNode = GetOrCreateContextNode.create(true);

        private DispatchDNUNode(final LookupGuard guard, final CompiledCodeObject primitiveMethod, final int argumentCount) {
            this.guard = guard;
            cachedMethod = primitiveMethod;
            callNode = DirectCallNode.create(cachedMethod.getCallTarget());
            createFrameArgumentsForDNUNode = CreateFrameArgumentsForDNUNode.create(argumentCount);
        }

        public Object execute(final VirtualFrame frame, final NativeObject cachedSelector) {
            return callNode.call(createFrameArgumentsForDNUNode.execute(frame, cachedSelector, cachedMethod, getOrCreateContextNode.executeGet(frame)));
        }

        protected static DispatchDNUNode create(final Object receiver, final NativeObject selector, final int argumentCount) {
            final LookupGuard guard = LookupGuard.create(receiver);
            final ClassObject receiverClass = SqueakObjectClassNode.getUncached().executeLookup(receiver);
            final Object lookupResult = LookupMethodNode.getUncached().executeLookup(receiverClass, selector);
            if (lookupResult != null) {
                return null; /* Not a DNU send. */
            }
            final CompiledCodeObject method = (CompiledCodeObject) lookupResult;
            return new DispatchDNUNode(guard, method, argumentCount);
        }

        @Override
        public NodeCost getCost() {
            return NodeCost.NONE;
        }
    }

    protected static final DispatchOAMNode createDispatchOAMData(final Object receiver, final NativeObject selector, final int argumentCount) {
        return DispatchOAMNode.create(receiver, selector, argumentCount);
    }

    protected static final class DispatchOAMNode extends Node {
        public final LookupGuard guard;
        public final Object cachedObject;
        public final CompiledCodeObject cachedMethod;
        @Child public DirectCallNode callNode;
        @Child protected CreateFrameArgumentsForOAMNode createFrameArgumentsForOAMNode;
        @Child private GetOrCreateContextNode getOrCreateContextNode = GetOrCreateContextNode.create(true);

        private DispatchOAMNode(final LookupGuard guard, final Object object, final CompiledCodeObject method, final int argumentCount) {
            this.guard = guard;
            cachedObject = object;
            cachedMethod = method;
            callNode = DirectCallNode.create(cachedMethod.getCallTarget());
            createFrameArgumentsForOAMNode = CreateFrameArgumentsForOAMNode.create(argumentCount);
        }

        public Object execute(final VirtualFrame frame, final NativeObject cachedSelector) {
            return callNode.call(createFrameArgumentsForOAMNode.execute(frame, cachedSelector, cachedObject, cachedMethod, getOrCreateContextNode.executeGet(frame)));
        }

        protected static DispatchOAMNode create(final Object receiver, final NativeObject selector, final int argumentCount) {
            final LookupGuard guard = LookupGuard.create(receiver);
            final ClassObject receiverClass = SqueakObjectClassNode.getUncached().executeLookup(receiver);
            final Object lookupResult = LookupMethodNode.getUncached().executeLookup(receiverClass, selector);
            if (!(lookupResult != null || lookupResult instanceof CompiledCodeObject)) {
                return null; /* Not a method. */
            }
            final ClassObject lookupResultClass = SqueakObjectClassNode.getUncached().executeLookup(lookupResult);
            final Object runWithInResult = LookupMethodNode.getUncached().executeLookup(lookupResultClass, selector.image.runWithInSelector);
            if (!(runWithInResult instanceof CompiledCodeObject)) {
                throw SqueakException.create("Add support for DNU on runWithIn");
            }
            return new DispatchOAMNode(guard, lookupResult, (CompiledCodeObject) runWithInResult, argumentCount);
        }

        @Override
        public NodeCost getCost() {
            return NodeCost.NONE;
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
