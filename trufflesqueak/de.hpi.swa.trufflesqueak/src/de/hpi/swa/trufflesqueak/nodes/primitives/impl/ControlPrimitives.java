package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;
import java.util.List;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.exceptions.SqueakQuit;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
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
import de.hpi.swa.trufflesqueak.util.Constants.BLOCK_CONTEXT;
import de.hpi.swa.trufflesqueak.util.Constants.CONTEXT;
import de.hpi.swa.trufflesqueak.util.Constants.PROCESS;
import de.hpi.swa.trufflesqueak.util.Constants.SEMAPHORE;
import de.hpi.swa.trufflesqueak.util.Constants.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.trufflesqueak.util.ProcessManager;

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
    @SqueakPrimitive(index = 80, numArguments = 2)
    public static abstract class PrimBlockCopyNode extends AbstractPrimitiveNode {
        public PrimBlockCopyNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        BaseSqueakObject doCopy(BaseSqueakObject receiver, int argCount) { // TODO: fixme
            PointersObject context = (PointersObject) receiver;
            if (context.at0(CONTEXT.METHOD) instanceof Integer) {
                context = (PointersObject) context.at0(BLOCK_CONTEXT.HOME);
            }
            ClassObject blockContextClass = (ClassObject) code.image.specialObjectsArray.at0(SPECIAL_OBJECT_INDEX.ClassBlockContext);
            BaseSqueakObject newBlock = blockContextClass.newInstance(context.size() + context.instsize());
            newBlock.atput0(BLOCK_CONTEXT.INITIAL_PC, -1); // TODO: calculate pc
            newBlock.atput0(CONTEXT.INSTRUCTION_POINTER, -1);
            newBlock.atput0(CONTEXT.STACKPOINTER, 0);
            newBlock.atput0(BLOCK_CONTEXT.ARGUMENT_COUNT, argCount);
            newBlock.atput0(BLOCK_CONTEXT.HOME, context);
            newBlock.atput0(CONTEXT.SENDER, code.image.nil); // claim not needed; just initialized
            return newBlock;
        }
    }

    private static abstract class AbstractPerformPrimitiveNode extends AbstractPrimitiveNode {
        @Child protected SqueakLookupClassNode lookupClassNode;
        @Child protected LookupNode lookupNode = LookupNodeGen.create();
        @Child protected DispatchNode dispatchNode = DispatchNodeGen.create();

        public AbstractPerformPrimitiveNode(CompiledMethodObject method) {
            super(method);
            lookupClassNode = SqueakLookupClassNodeGen.create(code);
        }

        protected ClassObject lookup(Object receiver) {
            try {
                return SqueakTypesGen.expectClassObject(lookupClassNode.executeLookup(receiver));
            } catch (UnexpectedResultException e) {
                throw new RuntimeException("receiver has no class");
            }
        }

        protected Object dispatch(Object receiver, Object selector, Object arguments, ClassObject rcvrClass) {
            Object lookupResult = lookupNode.executeLookup(rcvrClass, selector);
            Object[] rcvrAndArgs;
            if (arguments instanceof ListObject) {
                ListObject list = (ListObject) arguments;
                int numArgs = list.size();
                rcvrAndArgs = new Object[1 + numArgs];
                rcvrAndArgs[0] = receiver;
                for (int i = 0; i < numArgs; i++) {
                    rcvrAndArgs[1 + i] = list.at0(i);
                }
            } else if (arguments != null) {
                rcvrAndArgs = new Object[]{receiver, arguments};
            } else {
                rcvrAndArgs = new Object[]{receiver};
            }
            return dispatchNode.executeDispatch(lookupResult, rcvrAndArgs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 83, variableArguments = true)
    public static abstract class PrimPerformNode extends AbstractPerformPrimitiveNode {
        @Child private FrameReceiverAndArgumentsNode rcvrAndArgsNode = new FrameReceiverAndArgumentsNode();

        public PrimPerformNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        public Object perform(Object[] rcvrAndArgs) {
            int numRcvrAndArgs = rcvrAndArgs.length;
            if (numRcvrAndArgs != 2 && numRcvrAndArgs != 3) {
                throw new PrimitiveFailed();
            }
            Object receiver = rcvrAndArgs[0];
            Object selector = rcvrAndArgs[1];
            ClassObject rcvrClass = lookup(receiver);
            if (numRcvrAndArgs == 2) {
                return dispatch(receiver, selector, null, rcvrClass);
            }
            return dispatch(receiver, selector, rcvrAndArgs[2], rcvrClass);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 84, numArguments = 3)
    public static abstract class PrimPerformWithArgumentsNode extends AbstractPerformPrimitiveNode {
        public PrimPerformWithArgumentsNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        public Object perform(Object receiver, Object selector, ListObject arguments) {
            return dispatch(receiver, selector, arguments, lookup(receiver));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 85)
    public static abstract class PrimSignalNode extends AbstractPrimitiveNode {
        public PrimSignalNode(CompiledMethodObject method) {
            super(method);
        }

        protected boolean isSemaphore(PointersObject receiver) {
            return receiver.isSpecialKindAt(SPECIAL_OBJECT_INDEX.ClassSemaphore);
        }

        @Specialization(guards = "isSemaphore(receiver)")
        BaseSqueakObject doSignal(VirtualFrame frame, PointersObject receiver) {
            ProcessManager manager = code.image.process;
            if (manager.isEmptyList(receiver)) {
                // no process is waiting on this semaphore
                receiver.atput0(SEMAPHORE.EXCESS_SIGNALS, (int) receiver.at0(SEMAPHORE.EXCESS_SIGNALS) + 1);
            } else {
                manager.resumeProcess(frame, manager.removeFirstLinkOfList(receiver));
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 86)
    public static abstract class PrimWaitNode extends AbstractPrimitiveNode {
        public PrimWaitNode(CompiledMethodObject method) {
            super(method);
        }

        protected boolean isSemaphore(PointersObject receiver) {
            return receiver.isSpecialKindAt(SPECIAL_OBJECT_INDEX.ClassSemaphore);
        }

        @Specialization(guards = "isSemaphore(receiver)")
        BaseSqueakObject doWait(VirtualFrame frame, PointersObject receiver) {
            int excessSignals = (int) receiver.at0(SEMAPHORE.EXCESS_SIGNALS);
            if (excessSignals > 0)
                receiver.atput0(SEMAPHORE.EXCESS_SIGNALS, excessSignals - 1);
            else {
                ProcessManager manager = code.image.process;
                PointersObject activeProcess = manager.activeProcess();
                manager.linkProcessToList(activeProcess, receiver);
                manager.transferTo(frame, activeProcess, manager.wakeHighestPriority());
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 87)
    public static abstract class PrimResumeNode extends AbstractPrimitiveNode {
        public PrimResumeNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        BaseSqueakObject doResume(VirtualFrame frame, PointersObject receiver) {
            ProcessManager manager = code.image.process;
            manager.resumeProcess(frame, receiver);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 88)
    public static abstract class PrimSuspendNode extends AbstractPrimitiveNode {
        public PrimSuspendNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        BaseSqueakObject doSuspend(VirtualFrame frame, PointersObject receiver) {
            ProcessManager manager = code.image.process;
            PointersObject activeProcess = manager.activeProcess();
            if (receiver.equals(activeProcess)) {
                // popNandPush(1, code.image.nil);
                manager.transferTo(frame, activeProcess, manager.wakeHighestPriority());
            } else {
                BaseSqueakObject oldList = (BaseSqueakObject) receiver.at0(PROCESS.LIST);
                if (oldList.equals(code.image.nil)) {
                    throw new PrimitiveFailed();
                }
                manager.removeProcessFromList(receiver, oldList);
                receiver.atput0(PROCESS.LIST, code.image.nil);
                return oldList;
            }
            throw new RuntimeException("Failed to suspend process: " + receiver.toString());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 100, numArguments = 4)
    public static abstract class PrimPerformWithArgumentsInSuperclassNode extends AbstractPerformPrimitiveNode {

        public PrimPerformWithArgumentsInSuperclassNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        public Object perform(Object receiver, Object selector, ListObject arguments, ClassObject superClass) {
            return dispatch(receiver, selector, arguments, superClass);
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
    @SqueakPrimitive(index = 112)
    public static abstract class PrimBytesLeftNode extends AbstractPrimitiveNode {

        public PrimBytesLeftNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        BaseSqueakObject get(@SuppressWarnings("unused") BaseSqueakObject receiver) {
            return code.image.wrap(Runtime.getRuntime().freeMemory());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 113, variableArguments = true)
    public static abstract class PrimQuitNode extends AbstractPrimitiveNode {
        public PrimQuitNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        public Object quit(Object[] rcvrAndArgs) {
            int errorCode;
            try {
                errorCode = rcvrAndArgs.length > 1 ? (int) rcvrAndArgs[1] : 0;
            } catch (ClassCastException e) {
                errorCode = 1;
            }
            throw new SqueakQuit(errorCode);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 114)
    public static abstract class PrimExitToDebuggerNode extends AbstractPrimitiveNode {
        public PrimExitToDebuggerNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        public Object debugger(@SuppressWarnings("unused") VirtualFrame frame) {
            throw new RuntimeException("EXIT TO DEBUGGER");
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 115, numArguments = 2)
    public static abstract class PrimChangeClassNode extends AbstractPrimitiveNode {
        public PrimChangeClassNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        public Object changeClass(BaseSqueakObject receiver, BaseSqueakObject argument) {
            receiver.setSqClass(argument.getSqClass());
            return null;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 117, variableArguments = true)
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
                    return replace(PrimitiveNodeFactory.forName((CompiledMethodObject) code, modulename, functionname)).executeGeneric(frame);
                }
            }
            return replace(PrimitiveFailedNode.create((CompiledMethodObject) code)).executeGeneric(frame);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {130, 131})
    public static abstract class PrimFullGCNode extends AbstractPrimitiveNode {

        public PrimFullGCNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        BaseSqueakObject get(@SuppressWarnings("unused") BaseSqueakObject receiver) {
            System.gc();
            return code.image.wrap(Runtime.getRuntime().freeMemory());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 231)
    public static abstract class PrimForceDisplayUpdateNode extends AbstractPrimitiveNode {
        public PrimForceDisplayUpdateNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        BaseSqueakObject doForceUpdate(BaseSqueakObject receiver) {
            code.image.display.forceUpdate();
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 233, numArguments = 2)
    public static abstract class PrimSetFullScreenNode extends AbstractPrimitiveNode {
        public PrimSetFullScreenNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        BaseSqueakObject doFullScreen(BaseSqueakObject receiver, boolean enable) {
            code.image.display.setFullscreen(enable);
            return receiver;
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
