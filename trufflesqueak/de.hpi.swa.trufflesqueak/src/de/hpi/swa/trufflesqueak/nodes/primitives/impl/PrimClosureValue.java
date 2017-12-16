package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.BlockClosure;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.nodes.BlockActivationNode;
import de.hpi.swa.trufflesqueak.nodes.BlockActivationNodeGen;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.context.ReceiverNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.BottomNStackNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNode;

public abstract class PrimClosureValue extends PrimitiveNode {
    @Child protected BlockActivationNode dispatch;

    public PrimClosureValue(CompiledMethodObject code) {
        super(code);
        dispatch = BlockActivationNodeGen.create();
    }

    @NodeChildren({@NodeChild(value = "receiver", type = SqueakNode.class)})
    public static abstract class PrimClosureValue0 extends PrimClosureValue {
        @Child ReceiverNode receiverNode;

        public PrimClosureValue0(CompiledMethodObject method) {
            super(method);
            receiverNode = new ReceiverNode(method);
        }

        @Specialization
        protected Object value(BlockClosure block) {
            return dispatch.executeBlock(block, block.getFrameArguments());
        }
    }

    @NodeChildren({@NodeChild(value = "receiver", type = SqueakNode.class), @NodeChild(value = "arg1", type = SqueakNode.class)})
    public static abstract class PrimClosureValue1 extends PrimClosureValue {
        @Child BottomNStackNode bottomNNode;

        public PrimClosureValue1(CompiledMethodObject method) {
            super(method);
            bottomNNode = new BottomNStackNode(method, 2);
        }

        @Specialization
        protected Object value(BlockClosure block, Object arg) {
            return dispatch.executeBlock(block, block.getFrameArguments(arg));
        }
    }

    @NodeChildren({@NodeChild(value = "receiver", type = SqueakNode.class), @NodeChild(value = "arg1", type = SqueakNode.class), @NodeChild(value = "arg2", type = SqueakNode.class)})
    public static abstract class PrimClosureValue2 extends PrimClosureValue {
        @Child BottomNStackNode bottomNNode;

        public PrimClosureValue2(CompiledMethodObject method) {
            super(method);
            bottomNNode = new BottomNStackNode(method, 3);
        }

        @Specialization
        protected Object value(BlockClosure block, Object arg1, Object arg2) {
            return dispatch.executeBlock(block, block.getFrameArguments(arg1, arg2));
        }
    }

    @NodeChildren({@NodeChild(value = "receiver", type = SqueakNode.class), @NodeChild(value = "arg1", type = SqueakNode.class), @NodeChild(value = "arg2", type = SqueakNode.class),
                    @NodeChild(value = "arg3", type = SqueakNode.class)})
    public static abstract class PrimClosureValue3 extends PrimClosureValue {
        @Child BottomNStackNode bottomNNode;

        public PrimClosureValue3(CompiledMethodObject method) {
            super(method);
            bottomNNode = new BottomNStackNode(method, 4);
        }

        public abstract Object executeGeneric(Object receiver, Object arg1, Object arg2, Object arg3);

        @Specialization
        protected Object value(BlockClosure block, Object arg1, Object arg2, Object arg3) {
            return dispatch.executeBlock(block, block.getFrameArguments(arg1, arg2, arg3));
        }
    }

    @NodeChildren({@NodeChild(value = "receiver", type = SqueakNode.class), @NodeChild(value = "arg1", type = SqueakNode.class), @NodeChild(value = "arg2", type = SqueakNode.class),
                    @NodeChild(value = "arg3", type = SqueakNode.class), @NodeChild(value = "arg4", type = SqueakNode.class)})
    public static abstract class PrimClosureValue4 extends PrimClosureValue {
        @Child BottomNStackNode bottomNNode;

        public PrimClosureValue4(CompiledMethodObject method) {
            super(method);
            bottomNNode = new BottomNStackNode(method, 5);
        }

        @Specialization
        protected Object value(BlockClosure block, Object arg1, Object arg2, Object arg3, Object arg4) {
            return dispatch.executeBlock(block, block.getFrameArguments(arg1, arg2, arg3, arg4));
        }
    }

    @NodeChildren({@NodeChild(value = "receiver", type = SqueakNode.class), @NodeChild(value = "argArray", type = SqueakNode.class)})
    public static abstract class PrimClosureValueAry extends PrimClosureValue {
        @Child BottomNStackNode bottomNNode;

        public PrimClosureValueAry(CompiledMethodObject method) {
            super(method);
            bottomNNode = new BottomNStackNode(method, 2);
        }

        @Specialization
        protected Object value(BlockClosure block, ListObject argArray) {
            return dispatch.executeBlock(block, block.getFrameArguments(argArray.getPointers()));
        }
    }
}
