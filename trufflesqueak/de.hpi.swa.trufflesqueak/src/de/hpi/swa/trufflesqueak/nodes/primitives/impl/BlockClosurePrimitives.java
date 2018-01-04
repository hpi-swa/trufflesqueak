package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;

import de.hpi.swa.trufflesqueak.exceptions.SqueakQuit;
import de.hpi.swa.trufflesqueak.model.BlockClosure;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.nodes.BlockActivationNode;
import de.hpi.swa.trufflesqueak.nodes.BlockActivationNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;

public final class BlockClosurePrimitives extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return BlockClosurePrimitivesFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 197)
    public static abstract class PrimNextHandlerContextNode extends AbstractPrimitiveNode {
        private static final int EXCEPTION_HANDLER_MARKER = 199;

        public PrimNextHandlerContextNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary
        Object findNext(ContextObject receiver) {
            Object handlerContext = Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<Object>() {
                final Object marker = receiver.getFrameMarker();

                @Override
                public Object visitFrame(FrameInstance frameInstance) {
                    Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_ONLY);
                    FrameDescriptor frameDescriptor = current.getFrameDescriptor();
                    FrameSlot methodSlot = frameDescriptor.findFrameSlot(CompiledCodeObject.SLOT_IDENTIFIER.METHOD);
                    FrameSlot markerSlot = frameDescriptor.findFrameSlot(CompiledCodeObject.SLOT_IDENTIFIER.MARKER);
                    if (methodSlot != null && markerSlot != null) {
                        Object frameMethod = FrameUtil.getObjectSafe(current, methodSlot);
                        Object frameMarker = FrameUtil.getObjectSafe(current, markerSlot);
                        if (frameMarker == marker && frameMethod instanceof CompiledCodeObject) {
                            if (((CompiledCodeObject) frameMethod).primitiveIndex() == EXCEPTION_HANDLER_MARKER) {
                                return frameMethod;
                            }
                        }
                    }
                    return null;
                }
            });
            if (handlerContext == null) {
                printException();
            }
            return handlerContext;
        }

        @TruffleBoundary
        private void printException() {
            code.image.getOutput().println("=== Unhandled Error ===");
            Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<Object>() {
                @Override
                public Object visitFrame(FrameInstance frameInstance) {
                    Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_ONLY);
                    FrameDescriptor frameDescriptor = current.getFrameDescriptor();
                    FrameSlot methodSlot = frameDescriptor.findFrameSlot(CompiledCodeObject.SLOT_IDENTIFIER.METHOD);
                    if (methodSlot != null) {
                        code.image.getOutput().println(FrameUtil.getObjectSafe(current, methodSlot));
                        for (Object arg : current.getArguments()) {
                            code.image.getOutput().append("   ");
                            code.image.getOutput().println(arg);
                        }
                    }
                    return null;
                }
            });
            throw new SqueakQuit(1);
        }
    }

    private static abstract class AbstractClosureValuePrimitiveNode extends AbstractPrimitiveNode {
        @Child protected BlockActivationNode dispatch = BlockActivationNodeGen.create();

        public AbstractClosureValuePrimitiveNode(CompiledMethodObject method) {
            super(method);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {201, 221})
    public static abstract class PrimClosureValue0Node extends AbstractClosureValuePrimitiveNode {

        public PrimClosureValue0Node(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object value(BlockClosure block) {
            return dispatch.executeBlock(block, block.getFrameArguments());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 202, numArguments = 2)
    public static abstract class PrimClosureValue1Node extends AbstractClosureValuePrimitiveNode {

        public PrimClosureValue1Node(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object value(BlockClosure block, Object arg) {
            return dispatch.executeBlock(block, block.getFrameArguments(arg));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 203, numArguments = 3)
    public static abstract class PrimClosureValue2Node extends AbstractClosureValuePrimitiveNode {

        public PrimClosureValue2Node(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object value(BlockClosure block, Object arg1, Object arg2) {
            return dispatch.executeBlock(block, block.getFrameArguments(arg1, arg2));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 204, numArguments = 4)
    public static abstract class PrimClosureValue3Node extends AbstractClosureValuePrimitiveNode {

        public PrimClosureValue3Node(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object value(BlockClosure block, Object arg1, Object arg2, Object arg3) {
            return dispatch.executeBlock(block, block.getFrameArguments(arg1, arg2, arg3));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 205, numArguments = 5)
    public static abstract class PrimClosureValue4Node extends AbstractClosureValuePrimitiveNode {

        public PrimClosureValue4Node(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object value(BlockClosure block, Object arg1, Object arg2, Object arg3, Object arg4) {
            return dispatch.executeBlock(block, block.getFrameArguments(arg1, arg2, arg3, arg4));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {206, 222}, numArguments = 2)
    public static abstract class PrimClosureValueAryNode extends AbstractClosureValuePrimitiveNode {

        public PrimClosureValueAryNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object value(BlockClosure block, ListObject argArray) {
            return dispatch.executeBlock(block, block.getFrameArguments(argArray.getPointers()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 212)
    public static abstract class PrimContextSizeNode extends AbstractPrimitiveNode {
        public PrimContextSizeNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        int doSize(ContextObject receiver) {
            return receiver.size();
        }
    }
}
