package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;
import java.util.List;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.exceptions.SqueakExit;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.DispatchNode;
import de.hpi.swa.trufflesqueak.nodes.DispatchNodeGen;
import de.hpi.swa.trufflesqueak.nodes.LookupNode;
import de.hpi.swa.trufflesqueak.nodes.LookupNodeGen;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.SqueakTypesGen;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtNode;
import de.hpi.swa.trufflesqueak.nodes.context.SqueakLookupClassNode;
import de.hpi.swa.trufflesqueak.nodes.context.SqueakLookupClassNodeGen;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameReceiverAndArgumentsNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameReceiverNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ControlPrimitivesFactory.PrimQuickReturnReceiverVariableNodeFactory;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ControlPrimitivesFactory.PrimitiveFailedNodeFactory;

public class ControlPrimitives extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return ControlPrimitivesFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 19)
    public static abstract class PrimitiveFailedNode extends AbstractPrimitiveNode {

        public PrimitiveFailedNode(CompiledMethodObject method) {
            super(method);
        }

        public static PrimitiveFailedNode create(CompiledMethodObject method) {
            return PrimitiveFailedNodeFactory.create(method, new SqueakNode[0]);
        }

        @Specialization
        public Object fail(@SuppressWarnings("unused") VirtualFrame frame) {
            if (code.image.config.isVerbose()) {
                System.out.println("Primitive not yet written: " + code.toString());
            }
            throw new PrimitiveFailed();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 83)
    public static abstract class PrimPerformNode extends AbstractPrimitiveNode {
        @Child private SqueakLookupClassNode lookupClassNode;
        @Child private LookupNode lookupNode;
        @Child private DispatchNode dispatchNode;
        @Child private FrameReceiverAndArgumentsNode rcvrAndArgsNode;

        public PrimPerformNode(CompiledMethodObject method) {
            super(method);
            lookupClassNode = SqueakLookupClassNodeGen.create(code);
            dispatchNode = DispatchNodeGen.create();
            lookupNode = LookupNodeGen.create();
            rcvrAndArgsNode = new FrameReceiverAndArgumentsNode();
        }

        @Specialization
        public Object perform(@SuppressWarnings("unused") VirtualFrame frame, Object[] rcvrAndArgs) {
            ClassObject rcvrClass;
            try {
                rcvrClass = SqueakTypesGen.expectClassObject(lookupClassNode.executeLookup(rcvrAndArgs[0]));
            } catch (UnexpectedResultException e) {
                throw new RuntimeException("receiver has no class");
            }
            Object selector = rcvrAndArgs[2];
            Object lookupResult = lookupNode.executeLookup(rcvrClass, selector);
            return dispatchNode.executeDispatch(lookupResult, rcvrAndArgs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 110, numArguments = 2)
    public static abstract class PrimEquivalentNode extends AbstractPrimitiveNode {
        public PrimEquivalentNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        boolean equivalent(char a, char b) {
            return a == b;
        }

        @Specialization
        boolean equivalent(int a, int b) {
            return a == b;
        }

        @Specialization
        boolean equivalent(long a, long b) {
            return a == b;
        }

        @Specialization
        boolean equivalent(boolean a, boolean b) {
            return a == b;
        }

        @Specialization
        boolean equivalent(BigInteger a, BigInteger b) {
            return a.equals(b);
        }

        @Specialization
        boolean equivalent(Object a, Object b) {
            return a == b;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 111)
    public static abstract class PrimClassNode extends AbstractPrimitiveNode {
        private @Child SqueakLookupClassNode node;

        public PrimClassNode(CompiledMethodObject method) {
            super(method);
            node = SqueakLookupClassNodeGen.create(code);
        }

        @Specialization
        public Object lookup(Object arg) {
            return node.executeLookup(arg);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 113)
    public static abstract class PrimQuitNode extends AbstractPrimitiveNode {
        public PrimQuitNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        public Object quit(@SuppressWarnings("unused") VirtualFrame frame) {
            throw new SqueakExit(0);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 117, numArguments = 0)
    public static abstract class NamedPrimitiveCallNode extends AbstractPrimitiveNode {
        public NamedPrimitiveCallNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        Object doNamedPrimitive(VirtualFrame frame) {
            BaseSqueakObject descriptor = code.getLiteral(0) instanceof BaseSqueakObject ? (BaseSqueakObject) code.getLiteral(0) : null;
            if (descriptor != null && descriptor.getSqClass() != null && descriptor.size() >= 2) {
                Object descriptorAt0 = descriptor.at0(0);
                Object descriptorAt1 = descriptor.at0(1);
                if (descriptorAt0 != null && descriptorAt1 != null) {
                    String modulename = descriptorAt0.toString();
                    String functionname = descriptorAt1.toString();
                    replace(PrimitiveNodeFactory.forName((CompiledMethodObject) code, modulename, functionname)).executeGeneric(frame);
                }
            }
            return replace(PrimitiveFailedNode.create((CompiledMethodObject) code)).executeGeneric(frame);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 256)
    public static abstract class PrimQuickReturnSelfNode extends AbstractPrimitiveNode {
        @Child private FrameReceiverNode receiverNode = new FrameReceiverNode();

        public PrimQuickReturnSelfNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        public Object returnValue(VirtualFrame frame) {
            return receiverNode.executeGeneric(frame);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 257)
    public static abstract class PrimQuickReturnTrueNode extends AbstractPrimitiveNode {
        public PrimQuickReturnTrueNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        public Object returnValue(@SuppressWarnings("unused") VirtualFrame frame) {
            return code.image.sqTrue;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 258)
    public static abstract class PrimQuickReturnFalseNode extends AbstractPrimitiveNode {
        public PrimQuickReturnFalseNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        public Object returnValue(@SuppressWarnings("unused") VirtualFrame frame) {
            return code.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 259)
    public static abstract class PrimQuickReturnNilNode extends AbstractPrimitiveNode {
        public PrimQuickReturnNilNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        public Object returnValue(@SuppressWarnings("unused") VirtualFrame frame) {
            return code.image.nil;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 260)
    public static abstract class PrimQuickReturnMinusOneNode extends AbstractPrimitiveNode {
        public PrimQuickReturnMinusOneNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        public Object returnValue(@SuppressWarnings("unused") Object receiver) {
            return -1;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 261)
    public static abstract class PrimQuickReturnZeroNode extends AbstractPrimitiveNode {
        public PrimQuickReturnZeroNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        public Object returnValue(@SuppressWarnings("unused") Object receiver) {
            return 0;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 262)
    public static abstract class PrimQuickReturnOneNode extends AbstractPrimitiveNode {
        public PrimQuickReturnOneNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        public Object returnValue(@SuppressWarnings("unused") Object receiver) {
            return 1;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 263)
    public static abstract class PrimQuickReturnTwoNode extends AbstractPrimitiveNode {
        public PrimQuickReturnTwoNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        public Object returnValue(@SuppressWarnings("unused") Object receiver) {
            return 2;
        }
    }

    @GenerateNodeFactory
    public static abstract class PrimQuickReturnReceiverVariableNode extends AbstractPrimitiveNode {
        private @Child ObjectAtNode receiverVariableNode;

        public PrimQuickReturnReceiverVariableNode(CompiledMethodObject method, int variableIndex) {
            super(method);
            receiverVariableNode = ObjectAtNode.create(variableIndex, new FrameReceiverNode());
        }

        public static PrimQuickReturnReceiverVariableNode create(CompiledMethodObject method, int variableIndex) {
            return PrimQuickReturnReceiverVariableNodeFactory.create(method, variableIndex, new SqueakNode[0]);
        }

        @Specialization
        public Object receiverVariable(VirtualFrame frame) {
            return receiverVariableNode.executeGeneric(frame);
        }
    }
}
