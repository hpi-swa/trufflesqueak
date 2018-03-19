package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveExceptions.PrimitiveWithoutResultException;
import de.hpi.swa.trufflesqueak.exceptions.SqueakException;
import de.hpi.swa.trufflesqueak.exceptions.SqueakQuit;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.MUTEX;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.SEMAPHORE;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.DispatchNode;
import de.hpi.swa.trufflesqueak.nodes.DispatchNodeGen;
import de.hpi.swa.trufflesqueak.nodes.LookupNode;
import de.hpi.swa.trufflesqueak.nodes.LookupNodeGen;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtNode;
import de.hpi.swa.trufflesqueak.nodes.context.ReceiverAndArgumentsNode;
import de.hpi.swa.trufflesqueak.nodes.context.ReceiverNode;
import de.hpi.swa.trufflesqueak.nodes.context.SqueakLookupClassNode;
import de.hpi.swa.trufflesqueak.nodes.context.SqueakLookupClassNodeGen;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PushStackNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ControlPrimitivesFactory.PrimQuickReturnReceiverVariableNodeFactory;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ControlPrimitivesFactory.PrimitiveFailedNodeFactory;
import de.hpi.swa.trufflesqueak.nodes.process.GetActiveProcessNode;
import de.hpi.swa.trufflesqueak.nodes.process.IsEmptyListNode;
import de.hpi.swa.trufflesqueak.nodes.process.LinkProcessToListNode;
import de.hpi.swa.trufflesqueak.nodes.process.RemoveFirstLinkOfListNode;
import de.hpi.swa.trufflesqueak.nodes.process.RemoveProcessFromListNode;
import de.hpi.swa.trufflesqueak.nodes.process.ResumeProcessNode;
import de.hpi.swa.trufflesqueak.nodes.process.SignalSemaphoreNode;
import de.hpi.swa.trufflesqueak.nodes.process.WakeHighestPriorityNode;
import de.hpi.swa.trufflesqueak.nodes.process.YieldProcessNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public class ControlPrimitives extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return ControlPrimitivesFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 19)
    public static abstract class PrimitiveFailedNode extends AbstractPrimitiveNode {

        protected PrimitiveFailedNode(CompiledMethodObject method) {
            super(method);
        }

        public static PrimitiveFailedNode create(CompiledMethodObject method) {
            return PrimitiveFailedNodeFactory.create(method, null);
        }

        @Specialization
        protected Object fail(@SuppressWarnings("unused") VirtualFrame frame) {
            if (code.image.config.isVerbose()) {
                System.out.println("Primitive not yet written: " + code.toString());
            }
            throw new PrimitiveFailed();
        }
    }

    // primitiveBlockCopy / primitiveBlockValue: (#80, #81, #82) no longer needed.

    private static abstract class AbstractPerformPrimitiveNode extends AbstractPrimitiveNode {
        @Child protected SqueakLookupClassNode lookupClassNode;
        @Child protected LookupNode lookupNode = LookupNodeGen.create();
        @Child protected DispatchNode dispatchNode = DispatchNodeGen.create();

        protected AbstractPerformPrimitiveNode(CompiledMethodObject method) {
            super(method);
            lookupClassNode = SqueakLookupClassNodeGen.create(code);
        }

        protected ClassObject lookup(Object receiver) {
            return lookupClassNode.executeLookup(receiver);
        }

        protected Object dispatch(VirtualFrame frame, Object receiver, Object selector, Object arguments, ClassObject rcvrClass) {
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
            CompiledCodeObject lookupResult = (CompiledCodeObject) lookupNode.executeLookup(rcvrClass, selector);
            Object contextOrMarker = FrameAccess.getContextOrMarker(frame);
            if (lookupResult.isDoesNotUnderstand()) {
                Object[] rcvrAndSelector = new Object[]{rcvrAndArgs[0], selector};
                return dispatchNode.executeDispatch(frame, lookupResult, rcvrAndSelector, contextOrMarker);
            } else {
                return dispatchNode.executeDispatch(frame, lookupResult, rcvrAndArgs, contextOrMarker);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 83, variableArguments = true)
    protected static abstract class PrimPerformNode extends AbstractPerformPrimitiveNode {
        @Child private ReceiverAndArgumentsNode rcvrAndArgsNode;

        protected PrimPerformNode(CompiledMethodObject method) {
            super(method);
            rcvrAndArgsNode = ReceiverAndArgumentsNode.create(method);
        }

        @Override
        public final Object executeWithArguments(VirtualFrame frame, Object... rcvrAndArgs) {
            return perform(frame, rcvrAndArgs);
        }

        @Specialization
        protected Object perform(VirtualFrame frame, Object[] rcvrAndArgs) {
            long numRcvrAndArgs = rcvrAndArgs.length;
            if (numRcvrAndArgs != 2 && numRcvrAndArgs != 3) {
                throw new PrimitiveFailed();
            }
            Object receiver = rcvrAndArgs[0];
            Object selector = rcvrAndArgs[1];
            ClassObject rcvrClass = lookup(receiver);
            if (numRcvrAndArgs == 2) {
                return dispatch(frame, receiver, selector, null, rcvrClass);
            }
            return dispatch(frame, receiver, selector, rcvrAndArgs[2], rcvrClass);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 84, numArguments = 3)
    protected static abstract class PrimPerformWithArgumentsNode extends AbstractPerformPrimitiveNode {
        protected PrimPerformWithArgumentsNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object perform(VirtualFrame frame, Object receiver, Object selector, ListObject arguments) {
            return dispatch(frame, receiver, selector, arguments, lookup(receiver));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 85)
    protected static abstract class PrimSignalNode extends AbstractPrimitiveNode {
        @Child private SignalSemaphoreNode signalSemaphoreNode;

        protected PrimSignalNode(CompiledMethodObject method) {
            super(method);
            signalSemaphoreNode = SignalSemaphoreNode.create(method.image);
        }

        @Specialization(guards = "isSemaphore(receiver)")
        protected BaseSqueakObject doSignal(VirtualFrame frame, PointersObject receiver) {
            signalSemaphoreNode.executeSignal(frame, receiver);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 86)
    protected static abstract class PrimWaitNode extends AbstractPrimitiveNode {
        @Child private WakeHighestPriorityNode wakeHighestPriorityNode;
        @Child private LinkProcessToListNode linkProcessToListNode;
        @Child private GetActiveProcessNode getActiveProcessNode;

        protected PrimWaitNode(CompiledMethodObject method) {
            super(method);
            linkProcessToListNode = LinkProcessToListNode.create(method.image);
            wakeHighestPriorityNode = WakeHighestPriorityNode.create(method.image);
            getActiveProcessNode = GetActiveProcessNode.create(method.image);
        }

        @Specialization(guards = "isSemaphore(receiver)")
        protected BaseSqueakObject doWait(VirtualFrame frame, PointersObject receiver) {
            long excessSignals = (long) receiver.at0(SEMAPHORE.EXCESS_SIGNALS);
            if (excessSignals > 0) {
                receiver.atput0(SEMAPHORE.EXCESS_SIGNALS, excessSignals - 1);
            } else {
                PointersObject activeProcess = getActiveProcessNode.executeGet();
                linkProcessToListNode.executeLink(activeProcess, receiver);
                wakeHighestPriorityNode.executeWake(frame);
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 87)
    protected static abstract class PrimResumeNode extends AbstractPrimitiveNode {
        @Child private ResumeProcessNode resumeProcessNode;

        protected PrimResumeNode(CompiledMethodObject method) {
            super(method);
            resumeProcessNode = ResumeProcessNode.create(method.image);
        }

        @Specialization
        protected BaseSqueakObject doResume(VirtualFrame frame, PointersObject receiver) {
            resumeProcessNode.executeResume(frame, receiver);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 88)
    protected static abstract class PrimSuspendNode extends AbstractPrimitiveNode {
        @Child private WakeHighestPriorityNode wakeHighestPriorityNode;
        @Child private RemoveProcessFromListNode removeProcessNode;
        @Child private GetActiveProcessNode getActiveProcessNode;
        @Child private PushStackNode pushStackNode;

        protected PrimSuspendNode(CompiledMethodObject method) {
            super(method);
            removeProcessNode = RemoveProcessFromListNode.create(method.image);
            wakeHighestPriorityNode = WakeHighestPriorityNode.create(method.image);
            getActiveProcessNode = GetActiveProcessNode.create(method.image);
            pushStackNode = PushStackNode.create(method);
        }

        @Specialization
        protected BaseSqueakObject doSuspend(VirtualFrame frame, PointersObject receiver) {
            PointersObject activeProcess = getActiveProcessNode.executeGet();
            if (receiver == activeProcess) {
                pushStackNode.executeWrite(frame, code.image.nil);
                wakeHighestPriorityNode.executeWake(frame);
            } else {
                BaseSqueakObject oldList = (BaseSqueakObject) receiver.at0(PROCESS.LIST);
                if (oldList == code.image.nil) {
                    throw new PrimitiveFailed("PrimErrBadReceiver");
                }
                removeProcessNode.executeRemove(receiver, oldList);
                receiver.atput0(PROCESS.LIST, code.image.nil);
                pushStackNode.executeWrite(frame, oldList);
            }
            throw new PrimitiveWithoutResultException(); // result already pushed above
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 89)
    protected static abstract class PrimFlushCacheNode extends AbstractPrimitiveNode {

        public PrimFlushCacheNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final BaseSqueakObject doFlush(BaseSqueakObject receiver) {
            // TODO: actually flush caches once there are some
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 100, numArguments = 4)
    protected static abstract class PrimPerformWithArgumentsInSuperclassNode extends AbstractPerformPrimitiveNode {

        protected PrimPerformWithArgumentsInSuperclassNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object perform(VirtualFrame frame, Object receiver, Object selector, ListObject arguments, ClassObject superClass) {
            return dispatch(frame, receiver, selector, arguments, superClass);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 110, numArguments = 2)
    protected static abstract class PrimIdenticalNode extends AbstractPrimitiveNode {
        protected PrimIdenticalNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final static boolean doBoolean(final boolean a, final boolean b) {
            return a == b;
        }

        @Specialization
        protected final static boolean doChar(final char a, final char b) {
            return a == b;
        }

        @Specialization
        protected final static boolean doLong(final long a, final long b) {
            return a == b;
        }

        @Specialization
        protected final static boolean doDouble(final double a, final double b) {
            return a == b;
        }

        @Specialization
        protected final static boolean doObject(final Object a, final Object b) {
            return a.equals(b);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 111)
    protected static abstract class PrimClassNode extends AbstractPrimitiveNode {
        private @Child SqueakLookupClassNode node;

        protected PrimClassNode(CompiledMethodObject method) {
            super(method);
            node = SqueakLookupClassNode.create(code);
        }

        @Specialization
        protected ClassObject doClass(Object arg) {
            return node.executeLookup(arg);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 112)
    protected static abstract class PrimBytesLeftNode extends AbstractPrimitiveNode {

        protected PrimBytesLeftNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        Object get(@SuppressWarnings("unused") BaseSqueakObject receiver) {
            return code.image.wrap(Runtime.getRuntime().freeMemory());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 113, variableArguments = true)
    protected static abstract class PrimQuitNode extends AbstractPrimitiveNode {
        protected PrimQuitNode(CompiledMethodObject method) {
            super(method);
        }

        @Override
        public final Object executeWithArguments(VirtualFrame frame, Object... rcvrAndArgs) {
            return doQuit(rcvrAndArgs);
        }

        @Specialization
        protected Object doQuit(Object[] rcvrAndArgs) {
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
    protected static abstract class PrimExitToDebuggerNode extends AbstractPrimitiveNode {
        protected PrimExitToDebuggerNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object debugger(@SuppressWarnings("unused") VirtualFrame frame) {
            throw new SqueakException("EXIT TO DEBUGGER");
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 115, numArguments = 2)
    protected static abstract class PrimChangeClassNode extends AbstractPrimitiveNode {
        protected PrimChangeClassNode(CompiledMethodObject method) {
            super(method);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isSmallInteger(receiver)", "isSmallInteger(argument)"})
        protected Object doSmallInteger(final long receiver, final long argument) {
            throw new PrimitiveFailed();
        }

        @Specialization
        protected Object doNativeObject(NativeObject receiver, NativeObject argument) {
            receiver.setSqClass(argument.getSqClass());
            receiver.convertStorage(argument);
            throw new PrimitiveWithoutResultException();
        }

        @Specialization
        protected Object doSqueakObject(BaseSqueakObject receiver, BaseSqueakObject argument) {
            receiver.setSqClass(argument.getSqClass());
            throw new PrimitiveWithoutResultException();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 116)
    protected static abstract class PrimFlushCacheByMethodNode extends AbstractPrimitiveNode {

        public PrimFlushCacheByMethodNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final BaseSqueakObject doFlush(BaseSqueakObject receiver) {
            // TODO: actually flush caches once there are some
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 117, variableArguments = true)
    protected static abstract class PrimExternalCallNode extends AbstractPrimitiveNode {
        protected PrimExternalCallNode(CompiledMethodObject method) {
            super(method);
        }

        @Override
        public final Object executeWithArguments(VirtualFrame frame, Object... receiverAndArguments) {
            BaseSqueakObject descriptor = code.getLiteral(0) instanceof BaseSqueakObject ? (BaseSqueakObject) code.getLiteral(0) : null;
            if (descriptor != null && descriptor.getSqClass() != null && descriptor.size() >= 2) {
                Object descriptorAt0 = descriptor.at0(0);
                Object descriptorAt1 = descriptor.at0(1);
                if (descriptorAt0 != null && descriptorAt1 != null) {
                    String moduleName = descriptorAt0.toString();
                    String functionName = descriptorAt1.toString();
                    return replace(PrimitiveNodeFactory.forName((CompiledMethodObject) code, moduleName, functionName)).executeWithArguments(frame, receiverAndArguments);
                }
            }
            return replace(PrimitiveFailedNode.create((CompiledMethodObject) code)).executePrimitive(frame);
        }

        @Specialization
        protected Object doExternalCall(VirtualFrame frame) {
            BaseSqueakObject descriptor = code.getLiteral(0) instanceof BaseSqueakObject ? (BaseSqueakObject) code.getLiteral(0) : null;
            if (descriptor != null && descriptor.getSqClass() != null && descriptor.size() >= 2) {
                Object descriptorAt0 = descriptor.at0(0);
                Object descriptorAt1 = descriptor.at0(1);
                if (descriptorAt0 != null && descriptorAt1 != null) {
                    String moduleName = descriptorAt0.toString();
                    String functionName = descriptorAt1.toString();
                    return replace(PrimitiveNodeFactory.forName((CompiledMethodObject) code, moduleName, functionName)).executePrimitive(frame);
                }
            }
            return replace(PrimitiveFailedNode.create((CompiledMethodObject) code)).executePrimitive(frame);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 119)
    protected static abstract class PrimFlushCacheSelectiveNode extends AbstractPrimitiveNode {

        public PrimFlushCacheSelectiveNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final BaseSqueakObject doFlush(BaseSqueakObject receiver) {
            // TODO: actually flush caches once there are some
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {130, 131})
    protected static abstract class PrimFullGCNode extends AbstractPrimitiveNode {

        protected PrimFullGCNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object get(@SuppressWarnings("unused") BaseSqueakObject receiver) {
            System.gc();
            return code.image.wrap(Runtime.getRuntime().freeMemory());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 167)
    protected static abstract class PrimYieldNode extends AbstractPrimitiveNode {
        @Child private YieldProcessNode yieldProcessNode;

        public PrimYieldNode(CompiledMethodObject method) {
            super(method);
            yieldProcessNode = YieldProcessNode.create(method.image);
        }

        @Specialization
        protected final Object doYield(VirtualFrame frame, PointersObject scheduler) {
            yieldProcessNode.executeYield(frame, scheduler);
            throw new SqueakException("Yield failed");
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 169, numArguments = 2) // complements 110
    protected static abstract class PrimNotIdenticalNode extends AbstractPrimitiveNode {
        protected PrimNotIdenticalNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final static boolean doBoolean(final boolean a, final boolean b) {
            return a != b;
        }

        @Specialization
        protected final static boolean doChar(final char a, final char b) {
            return a != b;
        }

        @Specialization
        protected final static boolean doLong(final long a, final long b) {
            return a != b;
        }

        @Specialization
        protected final static boolean doDouble(final double a, final double b) {
            return a != b;
        }

        @Specialization
        protected final static boolean doObject(final Object a, final Object b) {
            return !a.equals(b);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 185)
    protected static abstract class PrimExitCriticalSectionNode extends AbstractPrimitiveNode {
        @Child private IsEmptyListNode isEmptyListNode;
        @Child private RemoveFirstLinkOfListNode removeFirstLinkOfListNode;
        @Child private ResumeProcessNode resumeProcessNode;

        public PrimExitCriticalSectionNode(CompiledMethodObject method) {
            super(method);
            isEmptyListNode = IsEmptyListNode.create(method.image);
            removeFirstLinkOfListNode = RemoveFirstLinkOfListNode.create(method.image);
            resumeProcessNode = ResumeProcessNode.create(method.image);
        }

        @Specialization
        protected Object doExit(VirtualFrame frame, PointersObject mutex) {
            if (isEmptyListNode.executeIsEmpty(mutex)) {
                mutex.atput0(MUTEX.OWNER, code.image.nil);
            } else {
                BaseSqueakObject owningProcess = removeFirstLinkOfListNode.executeRemove(mutex);
                mutex.atput0(MUTEX.OWNER, owningProcess);
                resumeProcessNode.executeResume(frame, owningProcess);
            }
            return mutex;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 186)
    protected static abstract class PrimEnterCriticalSectionNode extends AbstractPrimitiveNode {
        @Child private GetActiveProcessNode getActiveProcessNode;
        @Child private LinkProcessToListNode linkProcessToListNode;
        @Child private WakeHighestPriorityNode wakeHighestPriorityNode;
        @Child private PushStackNode pushStackNode;

        public PrimEnterCriticalSectionNode(CompiledMethodObject method) {
            super(method);
            getActiveProcessNode = GetActiveProcessNode.create(method.image);
            linkProcessToListNode = LinkProcessToListNode.create(method.image);
            wakeHighestPriorityNode = WakeHighestPriorityNode.create(method.image);
            pushStackNode = PushStackNode.create(method);
        }

        @Override
        public final Object executeWithArguments(VirtualFrame frame, Object... arguments) {
            return doEnter(frame, arguments);
        }

        @Specialization
        protected Object doEnter(VirtualFrame frame, Object[] rcvrAndArguments) {
            PointersObject mutex = (PointersObject) rcvrAndArguments[0];
            PointersObject activeProcess;
            if (rcvrAndArguments.length == 2) {
                activeProcess = (PointersObject) rcvrAndArguments[1];
            } else {
                activeProcess = getActiveProcessNode.executeGet();
            }
            Object owner = mutex.at0(MUTEX.OWNER);
            if (owner == code.image.nil) {
                mutex.atput0(MUTEX.OWNER, activeProcess);
                pushStackNode.executeWrite(frame, code.image.sqFalse);
            } else if (owner == activeProcess) {
                pushStackNode.executeWrite(frame, code.image.sqTrue);
            } else {
                pushStackNode.executeWrite(frame, code.image.sqFalse);
                linkProcessToListNode.executeLink(activeProcess, mutex);
                wakeHighestPriorityNode.executeWake(frame);
            }
            throw new PrimitiveWithoutResultException();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 187)
    protected static abstract class PrimTestAndSetOwnershipOfCriticalSectionNode extends AbstractPrimitiveNode {
        @Child private GetActiveProcessNode getActiveProcessNode;

        public PrimTestAndSetOwnershipOfCriticalSectionNode(CompiledMethodObject method) {
            super(method);
            getActiveProcessNode = GetActiveProcessNode.create(method.image);
        }

        @Specialization
        protected Object doTest(PointersObject rcvrMutex) {
            PointersObject activeProcess = getActiveProcessNode.executeGet();
            Object owner = rcvrMutex.at0(MUTEX.OWNER);
            if (owner == code.image.nil) {
                rcvrMutex.atput0(MUTEX.OWNER, activeProcess);
                return code.image.sqFalse;
            } else if (owner == activeProcess) {
                return code.image.sqTrue;
            }
            return code.image.nil;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 188, variableArguments = true)
    protected static abstract class PrimExecuteMethodArgsArray extends AbstractPerformPrimitiveNode {
        @Child private DispatchNode dispatchNode = DispatchNode.create();

        protected PrimExecuteMethodArgsArray(CompiledMethodObject method) {
            super(method);
        }

        @Override
        public final Object executeWithArguments(VirtualFrame frame, Object... rcvrAndArgs) {
            return doExecute(frame, rcvrAndArgs);
        }

        @Specialization
        protected Object doExecute(VirtualFrame frame, Object[] rcvrAndArgs) {
            if (3 < rcvrAndArgs.length || rcvrAndArgs.length > 5) {
                throw new PrimitiveFailed();
            }
            if (!(rcvrAndArgs[1] instanceof ListObject) || !(rcvrAndArgs[2] instanceof CompiledMethodObject)) {
                throw new PrimitiveFailed();
            }
            Object receiver = rcvrAndArgs[0];
            ListObject argArray = (ListObject) rcvrAndArgs[1];
            CompiledCodeObject codeObject = (CompiledCodeObject) rcvrAndArgs[2];
            int numArgs = argArray.size();
            Object[] dispatchRcvrAndArgs = new Object[1 + numArgs];
            dispatchRcvrAndArgs[0] = receiver;
            for (int i = 0; i < numArgs; i++) {
                dispatchRcvrAndArgs[1 + i] = argArray.at0(i);
            }
            Object thisContext = FrameAccess.getContextOrMarker(frame);
            return dispatchNode.executeDispatch(frame, codeObject, dispatchRcvrAndArgs, thisContext);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 230, numArguments = 2)
    protected static abstract class PrimRelinquishProcessorNode extends AbstractPrimitiveNode {

        public PrimRelinquishProcessorNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected BaseSqueakObject doRelinquish(VirtualFrame frame, BaseSqueakObject receiver, long timeMicroseconds) {
            code.image.interrupt.executeCheck(frame);
            try {
                TimeUnit.MICROSECONDS.sleep(timeMicroseconds);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 231)
    protected static abstract class PrimForceDisplayUpdateNode extends AbstractPrimitiveNode {
        protected PrimForceDisplayUpdateNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected BaseSqueakObject doForceUpdate(BaseSqueakObject receiver) {
            code.image.display.forceUpdate();
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 233, numArguments = 2)
    protected static abstract class PrimSetFullScreenNode extends AbstractPrimitiveNode {
        protected PrimSetFullScreenNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected BaseSqueakObject doFullScreen(BaseSqueakObject receiver, boolean enable) {
            code.image.display.setFullscreen(enable);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 256)
    protected static abstract class PrimQuickReturnSelfNode extends AbstractPrimitiveNode {
        protected PrimQuickReturnSelfNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object returnValue(Object receiver) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 257)
    protected static abstract class PrimQuickReturnTrueNode extends AbstractPrimitiveNode {
        protected PrimQuickReturnTrueNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object returnValue(@SuppressWarnings("unused") Object receiver) {
            return code.image.sqTrue;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 258)
    protected static abstract class PrimQuickReturnFalseNode extends AbstractPrimitiveNode {
        protected PrimQuickReturnFalseNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object returnValue(@SuppressWarnings("unused") Object receiver) {
            return code.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 259)
    protected static abstract class PrimQuickReturnNilNode extends AbstractPrimitiveNode {
        protected PrimQuickReturnNilNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object returnValue(@SuppressWarnings("unused") Object receiver) {
            return code.image.nil;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 260)
    protected static abstract class PrimQuickReturnMinusOneNode extends AbstractPrimitiveNode {
        protected PrimQuickReturnMinusOneNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object returnValue(@SuppressWarnings("unused") Object receiver) {
            return -1L;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 261)
    protected static abstract class PrimQuickReturnZeroNode extends AbstractPrimitiveNode {
        protected PrimQuickReturnZeroNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object returnValue(@SuppressWarnings("unused") Object receiver) {
            return 0L;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 262)
    protected static abstract class PrimQuickReturnOneNode extends AbstractPrimitiveNode {
        protected PrimQuickReturnOneNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object returnValue(@SuppressWarnings("unused") Object receiver) {
            return 1L;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 263)
    protected static abstract class PrimQuickReturnTwoNode extends AbstractPrimitiveNode {
        protected PrimQuickReturnTwoNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object returnValue(@SuppressWarnings("unused") Object receiver) {
            return 2L;
        }
    }

    @GenerateNodeFactory
    public static abstract class PrimQuickReturnReceiverVariableNode extends AbstractPrimitiveNode {
        @Child private ObjectAtNode receiverVariableNode;

        public static PrimQuickReturnReceiverVariableNode create(CompiledMethodObject method, long variableIndex) {
            return PrimQuickReturnReceiverVariableNodeFactory.create(method, variableIndex, new SqueakNode[0]);
        }

        protected PrimQuickReturnReceiverVariableNode(CompiledMethodObject method, long variableIndex) {
            super(method);
            receiverVariableNode = ObjectAtNode.create(variableIndex, ReceiverNode.create(method));
        }

        @Specialization
        protected Object receiverVariable(VirtualFrame frame) {
            return receiverVariableNode.executeGeneric(frame);
        }
    }
}
