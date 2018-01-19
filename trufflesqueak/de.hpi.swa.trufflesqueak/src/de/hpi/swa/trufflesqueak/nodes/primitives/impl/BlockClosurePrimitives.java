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

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.nodes.BlockActivationNode;
import de.hpi.swa.trufflesqueak.nodes.BlockActivationNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.BlockClosurePrimitivesFactory.PrimClosureValue0NodeFactory.PrimClosureValue0NodeGen;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class BlockClosurePrimitives extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return BlockClosurePrimitivesFactory.getFactories();
    }

    /*
     * This primitive is implemented to avoid running the fallback code which sets senders. This in turn
     * flags contexts incorrectly as dirty.
     */
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
                ContextObject currentContext = receiver.getSender();
                while (!currentContext.equals(previousContext)) {
                    ContextObject sendingContext = currentContext.getSender();
                    currentContext.terminate();
                    currentContext = sendingContext;
                }
            }
            receiver.setSender(previousContext);
            return receiver;
        }

        /*
         * Answer whether the receiver is strictly above context on the stack.
         */
        private static boolean hasSender(ContextObject context, ContextObject previousContext) {
            if (context.equals(previousContext)) {
                return false;
            }
            ContextObject sender = context.getSender();
            while (sender != null) {
                if (sender.equals(previousContext)) {
                    return true;
                }
                sender = sender.getSender();
            }
            return false;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 197)
    protected static abstract class PrimNextHandlerContextNode extends AbstractPrimitiveNode {
        private static final int EXCEPTION_HANDLER_MARKER = 199;

        protected PrimNextHandlerContextNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary
        Object findNext(ContextObject receiver) {
            ContextObject handlerContext = null;
// MethodContextObject handlerContext = Truffle.getRuntime().iterateFrames(new
// FrameInstanceVisitor<MethodContextObject>() {
// final Object marker = receiver.getFrameMarker();
//
// @Override
// public MethodContextObject visitFrame(FrameInstance frameInstance) {
// Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_ONLY);
// if (current.getArguments().length < FrameAccess.RCVR_AND_ARGS_START) {
// return null;
// }
// FrameDescriptor frameDescriptor = current.getFrameDescriptor();
// CompiledCodeObject frameMethod = FrameAccess.getMethod(current);
// FrameSlot markerSlot = frameDescriptor.findFrameSlot(CompiledCodeObject.SLOT_IDENTIFIER.MARKER);
// if (markerSlot != null) {
// Object frameMarker = FrameUtil.getObjectSafe(current, markerSlot);
// if (frameMarker == marker) {
// if (frameMethod.primitiveIndex() == EXCEPTION_HANDLER_MARKER) {
// return MethodContextObject.createReadOnlyContextObject(code.image, current);
// }
// }
// }
// return null;
// }
// });
            if (handlerContext == null) {
                printException();
            }
            return handlerContext;
        }

        @TruffleBoundary
        private void printException() {
            code.image.getOutput().println("=== Unhandled Error in PrimNextHandlerContextNode ===");
            Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<Object>() {
                @Override
                public Object visitFrame(FrameInstance frameInstance) {
                    Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_ONLY);
                    if (current.getArguments().length < FrameAccess.RCVR_AND_ARGS_START) {
                        return null;
                    }
                    CompiledCodeObject method = FrameAccess.getMethod(current);
                    code.image.getOutput().println(method);
                    for (Object arg : current.getArguments()) {
                        code.image.getOutput().append("   ");
                        code.image.getOutput().println(arg);
                    }
                    return null;
                }
            });
            code.image.getOutput().println("Letting primitive fail, executing fallback code instead...");
            throw new PrimitiveFailed();
        }
    }

    private static abstract class AbstractClosureValuePrimitiveNode extends AbstractPrimitiveNode {
        @Child protected BlockActivationNode dispatch = BlockActivationNodeGen.create();

        protected AbstractClosureValuePrimitiveNode(CompiledMethodObject method) {
            super(method);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {201, 221})
    public static abstract class PrimClosureValue0Node extends AbstractClosureValuePrimitiveNode {

        public static PrimClosureValue0Node create(CompiledMethodObject method) {
            return PrimClosureValue0NodeGen.create(method);
        }

        protected PrimClosureValue0Node(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object value(VirtualFrame frame, BlockClosureObject block) {
            return dispatch.executeBlock(block, block.getFrameArguments(frame));
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
        int doSize(ContextObject receiver) {
            return receiver.varsize();
        }
    }
}
