package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.nodes.BlockActivationNode;
import de.hpi.swa.trufflesqueak.nodes.BlockActivationNodeGen;
import de.hpi.swa.trufflesqueak.nodes.GetOrCreateContextNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.FrameMarker;

public final class BlockClosurePrimitives extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return BlockClosurePrimitivesFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 195, numArguments = 2)
    protected static abstract class PrimFindNextUnwindContextUpToNode extends AbstractPrimitiveNode {

        public PrimFindNextUnwindContextUpToNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"receiver.hasVirtualSender()"})
        @TruffleBoundary
        protected Object doFindNextVirtualized(ContextObject receiver, ContextObject previousContext) {
            ContextObject handlerContext = Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<ContextObject>() {
                boolean foundMyself = false;
                final FrameMarker frameMarker = receiver.getFrameMarker();

                @Override
                public ContextObject visitFrame(FrameInstance frameInstance) {
                    Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_ONLY);
                    if (current.getArguments().length < FrameAccess.RCVR_AND_ARGS_START) {
                        return null;
                    }
                    Object contextOrMarker = FrameAccess.getContextOrMarker(current);
                    if (!foundMyself) {
                        if (FrameAccess.isMatchingMarker(frameMarker, contextOrMarker)) {
                            foundMyself = true;
                        }
                    } else {
                        if (previousContext != null && FrameAccess.isMatchingMarker(previousContext.getFrameMarker(), contextOrMarker)) {
                            return null;
                        } else {
                            CompiledCodeObject frameMethod = FrameAccess.getMethod(current);
                            if (frameMethod.isUnwindMarked()) {
                                Frame currentMaterializable = frameInstance.getFrame(FrameInstance.FrameAccess.MATERIALIZE);
                                return GetOrCreateContextNode.getOrCreate(currentMaterializable);
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
        protected Object doFindNextVirtualizedNil(ContextObject receiver, @SuppressWarnings("unused") NilObject nil) {
            return doFindNextVirtualized(receiver, null);
        }

        @Specialization(guards = {"!receiver.hasVirtualSender()"})
        protected Object doFindNext(ContextObject receiver, BaseSqueakObject previousContextOrNil) {
            ContextObject current = receiver;
            while (current != previousContextOrNil) {
                BaseSqueakObject sender = current.getSender();
                if (sender == code.image.nil) {
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
    @SqueakPrimitive(index = 196, numArguments = 2)
    protected static abstract class PrimTerminateToNode extends AbstractPrimitiveNode {

        public PrimTerminateToNode(CompiledMethodObject method) {
            super(method);
        }

        /*
         * Terminate all the Contexts between me and previousContext, if previousContext is on my Context
         * stack. Make previousContext my sender.
         */
        @Specialization
        protected Object doTerminate(ContextObject receiver, ContextObject previousContext) {
            if (hasSender(receiver, previousContext)) {
                ContextObject currentContext = receiver.getNotNilSender();
                while (!currentContext.equals(previousContext)) {
                    ContextObject sendingContext = currentContext.getNotNilSender();
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
        private boolean hasSender(ContextObject context, ContextObject previousContext) {
            if (context.equals(previousContext)) {
                return false;
            }
            BaseSqueakObject sender = context.getSender();
            while (sender != code.image.nil) {
                if (sender.equals(previousContext)) {
                    return true;
                }
                sender = ((ContextObject) sender).getSender();
            }
            return false;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 197)
    protected static abstract class PrimNextHandlerContextNode extends AbstractPrimitiveNode {

        protected PrimNextHandlerContextNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"receiver.hasVirtualSender()"})
        @TruffleBoundary
        Object findNextVirtualized(ContextObject receiver) {
            ContextObject handlerContext = Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<ContextObject>() {
                boolean foundMyself = false;
                final FrameMarker frameMarker = receiver.getFrameMarker();

                @Override
                public ContextObject visitFrame(FrameInstance frameInstance) {
                    Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_ONLY);
                    if (current.getArguments().length < FrameAccess.RCVR_AND_ARGS_START) {
                        return null;
                    }
                    if (!foundMyself) {
                        Object contextOrMarker = FrameAccess.getContextOrMarker(current);
                        if (FrameAccess.isMatchingMarker(frameMarker, contextOrMarker)) {
                            foundMyself = true;
                        }
                    } else {
                        CompiledCodeObject frameMethod = FrameAccess.getMethod(current);
                        if (frameMethod.isExceptionHandlerMarked()) {
                            Frame currentMaterializable = frameInstance.getFrame(FrameInstance.FrameAccess.MATERIALIZE);
                            return GetOrCreateContextNode.getOrCreate(currentMaterializable);
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
        Object findNext(ContextObject receiver) {
            ContextObject context = receiver;
            while (true) {
                if (context.getMethod().isExceptionHandlerMarked()) {
                    return context;
                }
                BaseSqueakObject sender = context.getSender();
                if (sender instanceof ContextObject) {
                    context = (ContextObject) sender;
                } else {
                    assert sender == code.image.nil;
                    return code.image.nil;
                }
            }
        }

    }

    private static abstract class AbstractClosureValuePrimitiveNode extends AbstractPrimitiveNode {
        @Child protected BlockActivationNode dispatch = BlockActivationNodeGen.create();

        protected AbstractClosureValuePrimitiveNode(CompiledMethodObject method) {
            super(method);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 200, numArguments = 3)
    public static abstract class PrimClosureCopyWithCopiedValuesNode extends AbstractPrimitiveNode {

        protected PrimClosureCopyWithCopiedValuesNode(CompiledMethodObject method) {
            super(method);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected Object doCopy(VirtualFrame frame, ContextObject outerContext, long numArgs, ListObject copiedValues) {
            throw new SqueakException("Not implemented and not used in Squeak anymore");
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {201, 221})
    public static abstract class PrimClosureValue0Node extends AbstractClosureValuePrimitiveNode {

        protected PrimClosureValue0Node(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object doClosure(VirtualFrame frame, BlockClosureObject block) {
            return dispatch.executeBlock(block, block.getFrameArguments(frame));
        }

        // Additional specializations to speed up eager sends
        @Specialization
        protected Object doBoolean(boolean receiver) {
            return receiver;
        }

        @Specialization
        protected Object doNilObject(NilObject receiver) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 202, numArguments = 2)
    protected static abstract class PrimClosureValue1Node extends AbstractClosureValuePrimitiveNode {

        protected PrimClosureValue1Node(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object value(VirtualFrame frame, BlockClosureObject block, Object arg) {
            return dispatch.executeBlock(block, block.getFrameArguments(frame, arg));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 203, numArguments = 3)
    protected static abstract class PrimClosureValue2Node extends AbstractClosureValuePrimitiveNode {

        protected PrimClosureValue2Node(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object value(VirtualFrame frame, BlockClosureObject block, Object arg1, Object arg2) {
            return dispatch.executeBlock(block, block.getFrameArguments(frame, arg1, arg2));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 204, numArguments = 4)
    protected static abstract class PrimClosureValue3Node extends AbstractClosureValuePrimitiveNode {

        protected PrimClosureValue3Node(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object value(VirtualFrame frame, BlockClosureObject block, Object arg1, Object arg2, Object arg3) {
            return dispatch.executeBlock(block, block.getFrameArguments(frame, arg1, arg2, arg3));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 205, numArguments = 5)
    protected static abstract class PrimClosureValue4Node extends AbstractClosureValuePrimitiveNode {

        protected PrimClosureValue4Node(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object value(VirtualFrame frame, BlockClosureObject block, Object arg1, Object arg2, Object arg3, Object arg4) {
            return dispatch.executeBlock(block, block.getFrameArguments(frame, arg1, arg2, arg3, arg4));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {206, 222}, numArguments = 2)
    protected static abstract class PrimClosureValueAryNode extends AbstractClosureValuePrimitiveNode {

        protected PrimClosureValueAryNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object value(VirtualFrame frame, BlockClosureObject block, ListObject argArray) {
            return dispatch.executeBlock(block, block.getFrameArguments(frame, argArray.getPointers()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 212)
    protected static abstract class PrimContextSizeNode extends AbstractPrimitiveNode {

        protected PrimContextSizeNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        long doSize(ContextObject receiver) {
            return receiver.varsize();
        }
    }
}
