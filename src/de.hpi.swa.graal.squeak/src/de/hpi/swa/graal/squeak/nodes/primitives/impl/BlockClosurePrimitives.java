package de.hpi.swa.graal.squeak.nodes.primitives.impl;

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.BlockActivationNode;
import de.hpi.swa.graal.squeak.nodes.BlockActivationNodeGen;
import de.hpi.swa.graal.squeak.nodes.GetBlockFrameArgumentsNode;
import de.hpi.swa.graal.squeak.nodes.GetOrCreateContextNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotReadNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class BlockClosurePrimitives extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return BlockClosurePrimitivesFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 195)
    protected abstract static class PrimFindNextUnwindContextUpToNode extends AbstractPrimitiveNode {
        @Child private FrameSlotReadNode contextOrMarkerNode = FrameSlotReadNode.createForContextOrMarker();
        @Child private GetOrCreateContextNode contextNode = GetOrCreateContextNode.create();

        public PrimFindNextUnwindContextUpToNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = {"receiver.hasVirtualSender()"})
        @TruffleBoundary
        protected final Object doFindNextVirtualized(final ContextObject receiver, final ContextObject previousContext) {
            final ContextObject handlerContext = Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<ContextObject>() {
                boolean foundMyself = false;

                @Override
                public ContextObject visitFrame(final FrameInstance frameInstance) {
                    final Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_ONLY);
                    if (current.getArguments().length < FrameAccess.RECEIVER) {
                        return null;
                    }
                    final Object contextOrMarker = contextOrMarkerNode.executeRead(current);
                    if (!foundMyself) {
                        if (receiver == contextOrMarker) {
                            foundMyself = true;
                        }
                    } else {
                        if (previousContext != null && previousContext == contextOrMarker) {
                            return null;
                        } else {
                            final CompiledCodeObject frameMethod = FrameAccess.getMethod(current);
                            if (frameMethod.isUnwindMarked()) {
                                CompilerDirectives.bailout("Finding materializable frames should never be part of compiled code as it triggers deopts");
                                return contextNode.executeGet(frameInstance.getFrame(FrameInstance.FrameAccess.MATERIALIZE));
                            }
                        }
                    }
                    return null;
                }
            });
            if (handlerContext == null) {
                return code.image.nil;
            } else {
                return handlerContext;
            }
        }

        @Specialization(guards = {"receiver.hasVirtualSender()"})
        protected final Object doFindNextVirtualizedNil(final ContextObject receiver, @SuppressWarnings("unused") final NilObject nil) {
            return doFindNextVirtualized(receiver, null);
        }

        @Specialization(guards = {"!receiver.hasVirtualSender()"})
        protected final Object doFindNext(final ContextObject receiver, final AbstractSqueakObject previousContextOrNil) {
            ContextObject current = receiver;
            while (current != previousContextOrNil) {
                final AbstractSqueakObject sender = current.getSender();
                if (sender == code.image.nil || sender == previousContextOrNil) {
                    break;
                } else {
                    current = (ContextObject) sender;
                    if (current.isUnwindContext()) {
                        return current;
                    }
                }
            }
            return code.image.nil;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 196)
    protected abstract static class PrimTerminateToNode extends AbstractPrimitiveNode {

        public PrimTerminateToNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final Object doTerminate(final ContextObject receiver, final ContextObject previousContext) {
            return terminateTo(receiver, previousContext);
        }

        @Specialization
        protected static final Object doTerminate(final ContextObject receiver, final NilObject nil) {
            receiver.atput0(CONTEXT.SENDER_OR_NIL, nil); // flagging context as dirty
            return receiver;
        }

        /*
         * Terminate all the Contexts between me and previousContext, if previousContext is on my
         * Context stack. Make previousContext my sender.
         */
        private Object terminateTo(final ContextObject receiver, final ContextObject previousContext) {
            if (hasSender(receiver, previousContext)) {
                ContextObject currentContext = receiver.getNotNilSender();
                while (currentContext != previousContext) {
                    final ContextObject sendingContext = currentContext.getNotNilSender();
                    currentContext.terminate();
                    currentContext = sendingContext;
                }
            }
            receiver.atput0(CONTEXT.SENDER_OR_NIL, previousContext); // flagging context as dirty
            return receiver;
        }

        /*
         * Answer whether the receiver is strictly above context on the stack (Context>>hasSender:).
         */
        private boolean hasSender(final ContextObject context, final ContextObject previousContext) {
            if (context == previousContext) {
                return false;
            }
            AbstractSqueakObject sender = context.getSender();
            while (sender != code.image.nil) {
                if (sender == previousContext) {
                    return true;
                }
                sender = ((ContextObject) sender).getSender();
            }
            return false;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 197)
    protected abstract static class PrimNextHandlerContextNode extends AbstractPrimitiveNode {
        @Child private FrameSlotReadNode contextOrMarkerNode = FrameSlotReadNode.createForContextOrMarker();
        @Child private GetOrCreateContextNode contextNode = GetOrCreateContextNode.create();

        protected PrimNextHandlerContextNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = {"receiver.hasVirtualSender()"})
        @TruffleBoundary
        protected final Object findNextVirtualized(final ContextObject receiver) {
            final ContextObject handlerContext = Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<ContextObject>() {
                boolean foundMyself = false;

                @Override
                public ContextObject visitFrame(final FrameInstance frameInstance) {
                    final Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_ONLY);
                    if (current.getArguments().length < FrameAccess.RECEIVER) {
                        return null;
                    }
                    if (!foundMyself) {
                        final Object contextOrMarker = contextOrMarkerNode.executeRead(current);
                        if (receiver == contextOrMarker) {
                            foundMyself = true;
                        }
                    } else {
                        final CompiledCodeObject frameMethod = FrameAccess.getMethod(current);
                        if (frameMethod.isExceptionHandlerMarked()) {
                            CompilerDirectives.bailout("Finding materializable frames should never be part of compiled code as it triggers deopts");
                            return contextNode.executeGet(frameInstance.getFrame(FrameInstance.FrameAccess.MATERIALIZE));
                        }
                    }
                    return null;
                }
            });
            if (handlerContext == null) {
                return code.image.nil;
            } else {
                return handlerContext;
            }
        }

        @Specialization(guards = {"!receiver.hasVirtualSender()"})
        protected final Object findNext(final ContextObject receiver) {
            ContextObject context = receiver;
            while (true) {
                if (context.getMethod().isExceptionHandlerMarked()) {
                    return context;
                }
                final AbstractSqueakObject sender = context.getSender();
                if (sender instanceof ContextObject) {
                    context = (ContextObject) sender;
                } else {
                    assert sender == code.image.nil;
                    return code.image.nil;
                }
            }
        }

    }

    private abstract static class AbstractClosureValuePrimitiveNode extends AbstractPrimitiveNode {
        @Child protected BlockActivationNode dispatch = BlockActivationNodeGen.create();

        protected AbstractClosureValuePrimitiveNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 200)
    public abstract static class PrimClosureCopyWithCopiedValuesNode extends AbstractPrimitiveNode {

        protected PrimClosureCopyWithCopiedValuesNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected static final Object doCopy(final VirtualFrame frame, final ContextObject outerContext, final long numArgs, final PointersObject copiedValues) {
            throw new SqueakException("Not implemented and not used in Squeak anymore");
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 201)
    public abstract static class PrimClosureValue0Node extends AbstractClosureValuePrimitiveNode {

        protected PrimClosureValue0Node(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = {"block.getCompiledBlock().getNumArgs() == 0"})
        protected final Object doClosure(final VirtualFrame frame, final BlockClosureObject block,
                        @Cached("create()") final GetBlockFrameArgumentsNode getFrameArguments) {
            return dispatch.executeBlock(block, getFrameArguments.execute(block, getContextOrMarker(frame), new Object[0]));
        }

        // Additional specializations to speed up eager sends
        @Specialization
        protected static final Object doBoolean(final boolean receiver) {
            return receiver;
        }

        @Specialization
        protected static final Object doNilObject(final NilObject receiver) {
            return receiver;
        }

        @Fallback
        protected static final Object doFail(final Object receiver) {
            throw new SqueakException("Unexpected failure in primitiveValue0 (receiver:", receiver, ")");
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 202)
    protected abstract static class PrimClosureValue1Node extends AbstractClosureValuePrimitiveNode {

        protected PrimClosureValue1Node(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = {"block.getCompiledBlock().getNumArgs() == 1"})
        protected final Object doValue(final VirtualFrame frame, final BlockClosureObject block, final Object arg,
                        @Cached("create()") final GetBlockFrameArgumentsNode getFrameArguments) {
            return dispatch.executeBlock(block, getFrameArguments.execute(block, getContextOrMarker(frame), new Object[]{arg}));
        }

        @Fallback
        protected static final Object doFail(final Object receiver, final Object arg) {
            throw new SqueakException("Unexpected failure in primitiveValue1 (receiver:", receiver, ", arg:", arg, ")");
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 203)
    protected abstract static class PrimClosureValue2Node extends AbstractClosureValuePrimitiveNode {

        protected PrimClosureValue2Node(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = {"block.getCompiledBlock().getNumArgs() == 2"})
        protected final Object doValue(final VirtualFrame frame, final BlockClosureObject block, final Object arg1, final Object arg2,
                        @Cached("create()") final GetBlockFrameArgumentsNode getFrameArguments) {
            return dispatch.executeBlock(block, getFrameArguments.execute(block, getContextOrMarker(frame), new Object[]{arg1, arg2}));
        }

        @Fallback
        protected static final Object doFail(final Object receiver, final Object arg1, final Object arg2) {
            throw new SqueakException("Unexpected failure in primitiveValue2 (receiver:", receiver, ", arg1:", arg1, ", arg2:", arg2, ")");
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 204)
    protected abstract static class PrimClosureValue3Node extends AbstractClosureValuePrimitiveNode {

        protected PrimClosureValue3Node(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = {"block.getCompiledBlock().getNumArgs() == 3"})
        protected final Object doValue(final VirtualFrame frame, final BlockClosureObject block, final Object arg1, final Object arg2, final Object arg3,
                        @Cached("create()") final GetBlockFrameArgumentsNode getFrameArguments) {
            return dispatch.executeBlock(block, getFrameArguments.execute(block, getContextOrMarker(frame), new Object[]{arg1, arg2, arg3}));
        }

        @Fallback
        protected static final Object doFail(final Object receiver, final Object arg1, final Object arg2, final Object arg3) {
            throw new SqueakException("Unexpected failure in primitiveValue3 (receiver:", receiver, ", arg1:", arg1, ", arg2:", arg2, ", arg3:", arg3, ")");
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 205)
    protected abstract static class PrimClosureValue4Node extends AbstractClosureValuePrimitiveNode {

        protected PrimClosureValue4Node(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = {"block.getCompiledBlock().getNumArgs() == 4"})
        protected final Object doValue(final VirtualFrame frame, final BlockClosureObject block, final Object arg1, final Object arg2, final Object arg3, final Object arg4,
                        @Cached("create()") final GetBlockFrameArgumentsNode getFrameArguments) {
            return dispatch.executeBlock(block, getFrameArguments.execute(block, getContextOrMarker(frame), new Object[]{arg1, arg2, arg3, arg4}));
        }

        @Fallback
        protected static final Object doFail(final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4) {
            throw new SqueakException("Unexpected failure in primitiveValue4 (receiver:", receiver, ", arg1:", arg1, ", arg2:", arg2, ", arg3:", arg3, ", arg4:", arg4, ")");
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 206)
    protected abstract static class PrimClosureValueAryNode extends AbstractClosureValuePrimitiveNode {

        protected PrimClosureValueAryNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = {"block.getCompiledBlock().getNumArgs() == argArray.size()"})
        protected final Object doValue(final VirtualFrame frame, final BlockClosureObject block, final PointersObject argArray,
                        @Cached("create()") final GetBlockFrameArgumentsNode getFrameArguments) {
            return dispatch.executeBlock(block, getFrameArguments.execute(block, getContextOrMarker(frame), argArray.getPointers()));
        }

        @Fallback
        protected static final Object doFail(final Object receiver, final Object argArray) {
            throw new SqueakException("Unexpected failure in primitiveValueAry (receiver:", receiver, ", array:", argArray, ")");
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 221)
    public abstract static class PrimClosureValueNoContextSwitchNode extends AbstractClosureValuePrimitiveNode {

        protected PrimClosureValueNoContextSwitchNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = {"block.getCompiledBlock().getNumArgs() == 0"})
        protected final Object doValue(final VirtualFrame frame, final BlockClosureObject block,
                        @Cached("create()") final GetBlockFrameArgumentsNode getFrameArguments) {
            final boolean wasDisabled = code.image.interrupt.disabled();
            code.image.interrupt.disable();
            try {
                return dispatch.executeBlock(block, getFrameArguments.execute(block, getContextOrMarker(frame), new Object[0]));
            } finally {
                if (!wasDisabled) {
                    code.image.interrupt.enable();
                }
            }
        }

        @Fallback
        protected static final Object doFail(final Object receiver) {
            throw new SqueakException("Unexpected failure in primitiveValueNoContextSwitch (receiver:", receiver, ")");
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 222)
    protected abstract static class PrimClosureValueAryNoContextSwitchNode extends AbstractClosureValuePrimitiveNode {

        protected PrimClosureValueAryNoContextSwitchNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = {"block.getCompiledBlock().getNumArgs() == argArray.size()"})
        protected final Object doValue(final VirtualFrame frame, final BlockClosureObject block, final PointersObject argArray,
                        @Cached("create()") final GetBlockFrameArgumentsNode getFrameArguments) {
            final boolean wasDisabled = code.image.interrupt.disabled();
            code.image.interrupt.disable();
            try {
                return dispatch.executeBlock(block, getFrameArguments.execute(block, getContextOrMarker(frame), argArray.getPointers()));
            } finally {
                if (!wasDisabled) {
                    code.image.interrupt.enable();
                }
            }
        }

        @Fallback
        protected static final Object doFail(final Object receiver, final Object argArray) {
            throw new SqueakException("Unexpected failure in primitiveValueAryNoContextSwitch (receiver:", receiver, ", array:", argArray, ")");
        }
    }
}
