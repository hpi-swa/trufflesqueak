package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.BlockClosure;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.nodes.BlockActivationNode;
import de.hpi.swa.trufflesqueak.nodes.BlockActivationNodeGen;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNode;

public abstract class PrimClosureValue extends PrimitiveNode {
    @Child protected BlockActivationNode dispatch;

    public PrimClosureValue(CompiledMethodObject code) {
        super(code);
        dispatch = BlockActivationNodeGen.create();
    }

    @NodeChildren({@NodeChild(value = "receiver", type = SqueakNode.class)})
    public static abstract class PrimClosureValue0 extends PrimClosureValue {
        public PrimClosureValue0(CompiledMethodObject method2) {
            super(method2);
        }

        @Override
        public final Object executeGeneric(VirtualFrame frame) {
            return executeGeneric(receiver(frame));
        }

        public abstract Object executeGeneric(Object receiver);

        @Specialization
        protected Object value(BlockClosure block) {
            return dispatch.executeBlock(block, block.getFrameArguments());
        }
    }

    @NodeChildren({@NodeChild(value = "receiver", type = SqueakNode.class), @NodeChild(value = "arg1", type = SqueakNode.class)})
    public static abstract class PrimClosureValue1 extends PrimClosureValue {
        public PrimClosureValue1(CompiledMethodObject method2) {
            super(method2);
        }

        @Override
        public final Object executeGeneric(VirtualFrame frame) {
            Object[] args = bottomN(frame, 2);
            return executeGeneric(args[0], args[1]);
        }

        public abstract Object executeGeneric(Object receiver, Object arg1);

        @Specialization
        protected Object value(BlockClosure block, Object arg) {
            return dispatch.executeBlock(block, block.getFrameArguments(arg));
        }
    }

    @NodeChildren({@NodeChild(value = "receiver", type = SqueakNode.class), @NodeChild(value = "arg1", type = SqueakNode.class), @NodeChild(value = "arg2", type = SqueakNode.class)})
    public static abstract class PrimClosureValue2 extends PrimClosureValue {
        public PrimClosureValue2(CompiledMethodObject method2) {
            super(method2);
        }

        @Override
        public final Object executeGeneric(VirtualFrame frame) {
            Object[] args = bottomN(frame, 3);
            return executeGeneric(args[0], args[1], args[2]);
        }

        public abstract Object executeGeneric(Object receiver, Object arg1, Object arg2);

        @Specialization
        protected Object value(BlockClosure block, Object arg1, Object arg2) {
            return dispatch.executeBlock(block, block.getFrameArguments(arg1, arg2));
        }
    }

    @NodeChildren({@NodeChild(value = "receiver", type = SqueakNode.class), @NodeChild(value = "arg1", type = SqueakNode.class), @NodeChild(value = "arg2", type = SqueakNode.class),
                    @NodeChild(value = "arg3", type = SqueakNode.class)})
    public static abstract class PrimClosureValue3 extends PrimClosureValue {
        public PrimClosureValue3(CompiledMethodObject method2) {
            super(method2);
        }

        @Override
        public final Object executeGeneric(VirtualFrame frame) {
            Object[] args = bottomN(frame, 4);
            return executeGeneric(args[0], args[1], args[2], args[3]);
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
        public PrimClosureValue4(CompiledMethodObject method2) {
            super(method2);
        }

        @Override
        public final Object executeGeneric(VirtualFrame frame) {
            Object[] args = bottomN(frame, 5);
            return executeGeneric(args[0], args[1], args[2], args[3], args[4]);
        }

        public abstract Object executeGeneric(Object receiver, Object arg1, Object arg2, Object arg3, Object arg4);

        @Specialization
        protected Object value(BlockClosure block, Object arg1, Object arg2, Object arg3, Object arg4) {
            return dispatch.executeBlock(block, block.getFrameArguments(arg1, arg2, arg3, arg4));
        }
    }

    @NodeChildren({@NodeChild(value = "receiver", type = SqueakNode.class), @NodeChild(value = "argArray", type = SqueakNode.class)})
    public static abstract class PrimClosureValueAry extends PrimClosureValue {
        public PrimClosureValueAry(CompiledMethodObject method2) {
            super(method2);
        }

        @Override
        public final Object executeGeneric(VirtualFrame frame) {
            // TODO: check this axctually works
            Object[] args = bottomN(frame, 2);
            return executeGeneric(args[0], args[1]);
        }

        public abstract Object executeGeneric(Object receiver, Object argArray);

        @Specialization
        protected Object value(BlockClosure block, ListObject argArray) {
            return dispatch.executeBlock(block, block.getFrameArguments(argArray.getPointers()));
        }
    }
}
