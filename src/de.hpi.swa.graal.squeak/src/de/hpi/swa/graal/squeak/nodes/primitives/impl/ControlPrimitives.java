package de.hpi.swa.graal.squeak.nodes.primitives.impl;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveWithoutResultException;
import de.hpi.swa.graal.squeak.exceptions.SqueakException;
import de.hpi.swa.graal.squeak.exceptions.SqueakQuit;
import de.hpi.swa.graal.squeak.model.BaseSqueakObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.ListObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.ERROR_TABLE;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.MUTEX;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.SEMAPHORE;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.WeakPointersObject;
import de.hpi.swa.graal.squeak.nodes.DispatchNode;
import de.hpi.swa.graal.squeak.nodes.DispatchNodeGen;
import de.hpi.swa.graal.squeak.nodes.LookupNode;
import de.hpi.swa.graal.squeak.nodes.LookupNodeGen;
import de.hpi.swa.graal.squeak.nodes.SqueakNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.SendBytecodes.SendDoesNotUnderstandNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.SendBytecodes.SendObjectAsMethodNode;
import de.hpi.swa.graal.squeak.nodes.context.ObjectAtNode;
import de.hpi.swa.graal.squeak.nodes.context.ReceiverAndArgumentsNode;
import de.hpi.swa.graal.squeak.nodes.context.ReceiverNode;
import de.hpi.swa.graal.squeak.nodes.context.SqueakLookupClassNode;
import de.hpi.swa.graal.squeak.nodes.context.SqueakLookupClassNodeGen;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackWriteNode;
import de.hpi.swa.graal.squeak.nodes.context.stack.StackPushNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.ControlPrimitivesFactory.PrimQuickReturnReceiverVariableNodeFactory;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.ControlPrimitivesFactory.PrimitiveFailedNodeFactory;
import de.hpi.swa.graal.squeak.nodes.process.GetActiveProcessNode;
import de.hpi.swa.graal.squeak.nodes.process.IsEmptyListNode;
import de.hpi.swa.graal.squeak.nodes.process.LinkProcessToListNode;
import de.hpi.swa.graal.squeak.nodes.process.RemoveFirstLinkOfListNode;
import de.hpi.swa.graal.squeak.nodes.process.RemoveProcessFromListNode;
import de.hpi.swa.graal.squeak.nodes.process.ResumeProcessNode;
import de.hpi.swa.graal.squeak.nodes.process.SignalSemaphoreNode;
import de.hpi.swa.graal.squeak.nodes.process.WakeHighestPriorityNode;
import de.hpi.swa.graal.squeak.nodes.process.YieldProcessNode;
import de.hpi.swa.graal.squeak.util.ArrayUtils;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public class ControlPrimitives extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return ControlPrimitivesFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 19)
    public abstract static class PrimitiveFailedNode extends AbstractPrimitiveNode {

        protected PrimitiveFailedNode(final CompiledMethodObject method) {
            super(method);
        }

        public static PrimitiveFailedNode create(final CompiledMethodObject method) {
            return PrimitiveFailedNodeFactory.create(method, null);
        }

        @Specialization
        protected Object fail(@SuppressWarnings("unused") final VirtualFrame frame) {
            if (code.image.config.isVerbose() && !code.image.config.isTracing()) {
                code.image.getOutput().println("Primitive not yet written: " + code.toString());
            }
            throw new PrimitiveFailed();
        }
    }

    // primitiveBlockCopy / primitiveBlockValue: (#80, #81, #82) no longer needed.

    private abstract static class AbstractPerformPrimitiveNode extends AbstractPrimitiveNode {
        @Child protected SqueakLookupClassNode lookupClassNode;
        @Child protected LookupNode lookupNode = LookupNodeGen.create();
        @Child protected DispatchNode dispatchNode = DispatchNodeGen.create();
        @Child private SendDoesNotUnderstandNode sendDoesNotUnderstandNode;
        @Child private SendObjectAsMethodNode sendObjectAsMethodNode;

        protected AbstractPerformPrimitiveNode(final CompiledMethodObject method) {
            super(method);
            lookupClassNode = SqueakLookupClassNodeGen.create(code.image);
            sendDoesNotUnderstandNode = SendDoesNotUnderstandNode.create(code.image);
            sendObjectAsMethodNode = SendObjectAsMethodNode.create(code.image);
        }

        protected ClassObject lookup(final Object receiver) {
            return lookupClassNode.executeLookup(receiver);
        }

        protected Object dispatch(final VirtualFrame frame, final Object selector, final Object[] rcvrAndArgs, final ClassObject rcvrClass) {
            final Object lookupResult = lookupNode.executeLookup(rcvrClass, selector);
            final Object contextOrMarker = FrameAccess.getContextOrMarker(frame);
            if (!(lookupResult instanceof CompiledCodeObject)) {
                return sendObjectAsMethodNode.execute(frame, selector, rcvrAndArgs, lookupResult, contextOrMarker);
            } else if (((CompiledCodeObject) lookupResult).isDoesNotUnderstand()) {
                return sendDoesNotUnderstandNode.execute(frame, selector, rcvrAndArgs, rcvrClass, lookupResult, contextOrMarker);
            } else {
                return dispatchNode.executeDispatch(frame, lookupResult, rcvrAndArgs, contextOrMarker);
            }
        }
    }

    private abstract static class AbstractPrimitiveWithPushNode extends AbstractPrimitiveNode {
        @Child protected StackPushNode pushNode;

        protected AbstractPrimitiveWithPushNode(final CompiledMethodObject method) {
            super(method);
            pushNode = StackPushNode.create(method);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 83, variableArguments = true)
    protected abstract static class PrimPerformNode extends AbstractPerformPrimitiveNode {
        @Child private ReceiverAndArgumentsNode rcvrAndArgsNode;

        protected PrimPerformNode(final CompiledMethodObject method) {
            super(method);
            rcvrAndArgsNode = ReceiverAndArgumentsNode.create(method);
        }

        @Override
        public final Object executeWithArguments(final VirtualFrame frame, final Object... rcvrAndArgs) {
            return perform(frame, rcvrAndArgs);
        }

        @Specialization
        protected Object perform(final VirtualFrame frame, final Object[] rcvrAndArgs) {
            final long numRcvrAndArgs = rcvrAndArgs.length;
            if (numRcvrAndArgs < 2 || numRcvrAndArgs > 8) {
                throw new PrimitiveFailed();
            }
            final Object receiver = rcvrAndArgs[0];
            final Object selector = rcvrAndArgs[1];
            final ClassObject rcvrClass = lookup(receiver);
            if (numRcvrAndArgs == 2) {
                return dispatch(frame, selector, new Object[]{receiver}, rcvrClass);
            } else {
                // remove selector from rcvrAndArgs
                final Object[] newRcvrAndArgs = new Object[rcvrAndArgs.length - 1];
                newRcvrAndArgs[0] = receiver;
                for (int i = 2; i < rcvrAndArgs.length; i++) {
                    newRcvrAndArgs[i - 1] = rcvrAndArgs[i];
                }
                return dispatch(frame, selector, newRcvrAndArgs, rcvrClass);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 84, numArguments = 3)
    protected abstract static class PrimPerformWithArgumentsNode extends AbstractPerformPrimitiveNode {
        protected PrimPerformWithArgumentsNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object perform(final VirtualFrame frame, final Object receiver, final Object selector, final ListObject arguments) {
            return dispatch(frame, selector, arguments.unwrappedWithFirst(receiver), lookup(receiver));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 85)
    protected abstract static class PrimSignalNode extends AbstractPrimitiveWithPushNode {
        @Child private SignalSemaphoreNode signalSemaphoreNode;

        protected PrimSignalNode(final CompiledMethodObject method) {
            super(method);
            signalSemaphoreNode = SignalSemaphoreNode.create(method.image);
        }

        @Specialization(guards = "isSemaphore(receiver)")
        protected BaseSqueakObject doSignal(final VirtualFrame frame, final PointersObject receiver) {
            pushNode.executeWrite(frame, receiver); // keep receiver on stack
            signalSemaphoreNode.executeSignal(frame, receiver);
            throw new PrimitiveWithoutResultException();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 86)
    protected abstract static class PrimWaitNode extends AbstractPrimitiveWithPushNode {
        @Child private WakeHighestPriorityNode wakeHighestPriorityNode;
        @Child private LinkProcessToListNode linkProcessToListNode;
        @Child private GetActiveProcessNode getActiveProcessNode;

        protected PrimWaitNode(final CompiledMethodObject method) {
            super(method);
            linkProcessToListNode = LinkProcessToListNode.create(method.image);
            wakeHighestPriorityNode = WakeHighestPriorityNode.create(method.image);
            getActiveProcessNode = GetActiveProcessNode.create(method.image);
        }

        @Specialization(guards = "isSemaphore(receiver)")
        protected BaseSqueakObject doWait(final VirtualFrame frame, final PointersObject receiver) {
            pushNode.executeWrite(frame, receiver); // keep receiver on stack
            final long excessSignals = (long) receiver.at0(SEMAPHORE.EXCESS_SIGNALS);
            if (excessSignals > 0) {
                receiver.atput0(SEMAPHORE.EXCESS_SIGNALS, excessSignals - 1);
            } else {
                final PointersObject activeProcess = getActiveProcessNode.executeGet();
                linkProcessToListNode.executeLink(activeProcess, receiver);
                wakeHighestPriorityNode.executeWake(frame);
            }
            throw new PrimitiveWithoutResultException();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 87)
    protected abstract static class PrimResumeNode extends AbstractPrimitiveWithPushNode {
        @Child private ResumeProcessNode resumeProcessNode;

        protected PrimResumeNode(final CompiledMethodObject method) {
            super(method);
            resumeProcessNode = ResumeProcessNode.create(method.image);
        }

        @Specialization
        protected BaseSqueakObject doResume(final VirtualFrame frame, final PointersObject receiver) {
            // keep receiver on stack before resuming other process
            pushNode.executeWrite(frame, receiver);
            resumeProcessNode.executeResume(frame, receiver);
            throw new PrimitiveWithoutResultException();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 88)
    protected abstract static class PrimSuspendNode extends AbstractPrimitiveWithPushNode {
        @Child private WakeHighestPriorityNode wakeHighestPriorityNode;
        @Child private RemoveProcessFromListNode removeProcessNode;
        @Child private GetActiveProcessNode getActiveProcessNode;

        protected PrimSuspendNode(final CompiledMethodObject method) {
            super(method);
            removeProcessNode = RemoveProcessFromListNode.create(method.image);
            wakeHighestPriorityNode = WakeHighestPriorityNode.create(method.image);
            getActiveProcessNode = GetActiveProcessNode.create(method.image);
        }

        @Specialization
        protected BaseSqueakObject doSuspend(final VirtualFrame frame, final PointersObject receiver) {
            final PointersObject activeProcess = getActiveProcessNode.executeGet();
            if (receiver == activeProcess) {
                pushNode.executeWrite(frame, code.image.nil);
                wakeHighestPriorityNode.executeWake(frame);
            } else {
                final BaseSqueakObject oldList = (BaseSqueakObject) receiver.at0(PROCESS.LIST);
                if (oldList.isNil()) {
                    throw new PrimitiveFailed(ERROR_TABLE.BAD_RECEIVER);
                }
                removeProcessNode.executeRemove(receiver, oldList);
                receiver.atput0(PROCESS.LIST, code.image.nil);
                pushNode.executeWrite(frame, oldList);
            }
            throw new PrimitiveWithoutResultException(); // result already pushed above
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 89)
    protected abstract static class PrimFlushCacheNode extends AbstractPrimitiveNode {

        public PrimFlushCacheNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final BaseSqueakObject doFlush(final BaseSqueakObject receiver) {
            // TODO: actually flush caches once there are some
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 100, variableArguments = true)
    protected abstract static class PrimPerformWithArgumentsInSuperclassNode extends AbstractPerformPrimitiveNode {

        protected PrimPerformWithArgumentsInSuperclassNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        public final Object executeWithArguments(final VirtualFrame frame, final Object... rcvrAndArgs) {
            return doPerform(frame, rcvrAndArgs);
        }

        @Specialization
        protected final Object doPerform(final VirtualFrame frame, final Object[] rvcrAndArgs) {
            final int numRcvrAndArgs = rvcrAndArgs.length;
            if (numRcvrAndArgs == 4) { // Object>>#perform:withArguments:inSuperclass:
                return dispatchRcvrAndArgs(frame, rvcrAndArgs);
            } else if (numRcvrAndArgs == 5) { // Context>>#object:perform:withArguments:inClass:
                // use first argument as receiver
                return dispatchRcvrAndArgs(frame, ArrayUtils.allButFirst(rvcrAndArgs));
            } else {
                throw new PrimitiveFailed();
            }
        }

        private Object dispatchRcvrAndArgs(final VirtualFrame frame, final Object[] rvcrAndArgs) {
            final Object receiver = rvcrAndArgs[0];
            final Object selector = rvcrAndArgs[1];
            final ListObject arguments = (ListObject) rvcrAndArgs[2];
            final ClassObject superClass = (ClassObject) rvcrAndArgs[3];
            return dispatch(frame, selector, arguments.unwrappedWithFirst(receiver), superClass);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 110, numArguments = 2)
    protected abstract static class PrimIdenticalNode extends AbstractPrimitiveNode {
        protected PrimIdenticalNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final boolean doBoolean(final boolean a, final boolean b) {
            return a == b;
        }

        @Specialization
        protected static final boolean doChar(final char a, final char b) {
            return a == b;
        }

        @Specialization
        protected static final boolean doLong(final long a, final long b) {
            return a == b;
        }

        @Specialization
        protected final boolean doDouble(final double a, final double b) {
            if (Double.isNaN(a) && Double.isNaN(b)) {
                return code.image.sqTrue;
            } else {
                return a == b;
            }
        }

        @Specialization
        protected final boolean doFloat(final FloatObject a, final FloatObject b) {
            return a == b || doDouble(a.getValue(), b.getValue());
        }

        @SuppressWarnings("unused")
        @Specialization
        protected final boolean doObject(final NilObject a, final NilObject b) {
            return code.image.sqTrue;
        }

        @Fallback
        protected static final boolean doSqueakObject(final Object a, final Object b) {
            return a == b;
        }
    }

    /*
     * primitiveClass (see Object>>class and Context>>objectClass:).
     */
    @GenerateNodeFactory
    @SqueakPrimitive(index = 111, variableArguments = true)
    protected abstract static class PrimClassNode extends AbstractPrimitiveNode {
        private @Child SqueakLookupClassNode node;

        protected PrimClassNode(final CompiledMethodObject method) {
            super(method);
            node = SqueakLookupClassNode.create(code.image);
        }

        @Override
        public final Object executeWithArguments(final VirtualFrame frame, final Object... rcvrAndArgs) {
            return doClass(rcvrAndArgs);
        }

        @Specialization
        protected final ClassObject doClass(final Object[] rcvrAndArgs) {
            return node.executeLookup(rcvrAndArgs[rcvrAndArgs.length - 1]);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 112)
    protected abstract static class PrimBytesLeftNode extends AbstractPrimitiveNode {

        protected PrimBytesLeftNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        Object get(@SuppressWarnings("unused") final BaseSqueakObject receiver) {
            return code.image.wrap(Runtime.getRuntime().freeMemory());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 113, variableArguments = true)
    protected abstract static class PrimQuitNode extends AbstractPrimitiveNode {
        protected PrimQuitNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        public final Object executeWithArguments(final VirtualFrame frame, final Object... rcvrAndArgs) {
            return doQuit(rcvrAndArgs);
        }

        @Specialization
        protected Object doQuit(final Object[] rcvrAndArgs) {
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
    protected abstract static class PrimExitToDebuggerNode extends AbstractPrimitiveNode {
        protected PrimExitToDebuggerNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object debugger(@SuppressWarnings("unused") final VirtualFrame frame) {
            throw new SqueakException("EXIT TO DEBUGGER");
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 115, numArguments = 2)
    protected abstract static class PrimChangeClassNode extends AbstractPrimitiveNode {
        protected PrimChangeClassNode(final CompiledMethodObject method) {
            super(method);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isSmallInteger(receiver)", "isSmallInteger(argument)"})
        protected Object doSmallInteger(final long receiver, final long argument) {
            throw new PrimitiveFailed();
        }

        @Specialization
        protected Object doNativeObject(final NativeObject receiver, final NativeObject argument) {
            receiver.setSqClass(argument.getSqClass());
            receiver.convertStorage(argument);
            throw new PrimitiveWithoutResultException();
        }

        @Specialization(guards = "!isNativeObject(receiver)")
        protected Object doSqueakObject(final BaseSqueakObject receiver, final BaseSqueakObject argument) {
            receiver.setSqClass(argument.getSqClass());
            throw new PrimitiveWithoutResultException();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 116)
    protected abstract static class PrimFlushCacheByMethodNode extends AbstractPrimitiveNode {

        public PrimFlushCacheByMethodNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final BaseSqueakObject doFlush(final BaseSqueakObject receiver) {
            // TODO: actually flush caches once there are some
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 117, variableArguments = true)
    protected abstract static class PrimExternalCallNode extends AbstractPrimitiveNode {
        protected PrimExternalCallNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        public final Object executeWithArguments(final VirtualFrame frame, final Object... receiverAndArguments) {
            final BaseSqueakObject descriptor = code.getLiteral(0) instanceof BaseSqueakObject ? (BaseSqueakObject) code.getLiteral(0) : null;
            if (descriptor != null && descriptor.getSqClass() != null && descriptor.size() >= 2) {
                final Object descriptorAt0 = descriptor.at0(0);
                final Object descriptorAt1 = descriptor.at0(1);
                if (descriptorAt0 != null && descriptorAt1 != null) {
                    final String moduleName = descriptorAt0.toString();
                    final String functionName = descriptorAt1.toString();
                    return replace(PrimitiveNodeFactory.forName((CompiledMethodObject) code, moduleName, functionName)).executeWithArguments(frame, receiverAndArguments);
                }
            }
            return replace(PrimitiveFailedNode.create((CompiledMethodObject) code)).executePrimitive(frame);
        }

        @Specialization
        protected Object doExternalCall(final VirtualFrame frame) {
            final BaseSqueakObject descriptor = code.getLiteral(0) instanceof BaseSqueakObject ? (BaseSqueakObject) code.getLiteral(0) : null;
            if (descriptor != null && descriptor.getSqClass() != null && descriptor.size() >= 2) {
                final Object descriptorAt0 = descriptor.at0(0);
                final Object descriptorAt1 = descriptor.at0(1);
                if (descriptorAt0 != null && descriptorAt1 != null) {
                    final String moduleName = descriptorAt0.toString();
                    final String functionName = descriptorAt1.toString();
                    return replace(PrimitiveNodeFactory.forName((CompiledMethodObject) code, moduleName, functionName)).executePrimitive(frame);
                }
            }
            return replace(PrimitiveFailedNode.create((CompiledMethodObject) code)).executePrimitive(frame);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 119)
    protected abstract static class PrimFlushCacheSelectiveNode extends AbstractPrimitiveNode {

        public PrimFlushCacheSelectiveNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final BaseSqueakObject doFlush(final BaseSqueakObject receiver) {
            // TODO: actually flush caches once there are some
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {130, 131})
    protected abstract static class PrimFullGCNode extends AbstractPrimitiveNode {
        @Child FrameStackWriteNode stackWriteNode = FrameStackWriteNode.create();

        protected PrimFullGCNode(final CompiledMethodObject method) {
            super(method);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected final Object doGC(final VirtualFrame frame, final BaseSqueakObject receiver) {
            System.gc();
            finalizeWeakPointersObjects(frame);
            return code.image.wrap(Runtime.getRuntime().freeMemory());
        }

        private void finalizeWeakPointersObjects(final VirtualFrame frame) {
            final ReferenceQueue<Object> queue = WeakPointersObject.weakPointersQueue;
            Reference<? extends Object> element = queue.poll();
            int count = 0;
            while (element != null) {
                count++;
                element = queue.poll();
            }
            code.image.traceVerbose(count + " WeakPointersObjects have been garbage collected.");
            if (count > 0) {
                code.image.interrupt.triggerPendingFinalizations(frame);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 167)
    protected abstract static class PrimYieldNode extends AbstractPrimitiveWithPushNode {
        @Child private YieldProcessNode yieldProcessNode;

        public PrimYieldNode(final CompiledMethodObject method) {
            super(method);
            yieldProcessNode = YieldProcessNode.create(method.image);
        }

        @Specialization
        protected final Object doYield(final VirtualFrame frame, final PointersObject scheduler) {
            pushNode.executeWrite(frame, scheduler); // keep receiver on stack
            yieldProcessNode.executeYield(frame, scheduler);
            throw new PrimitiveWithoutResultException();
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 169, numArguments = 2) // complements 110
    protected abstract static class PrimNotIdenticalNode extends AbstractPrimitiveNode {
        protected PrimNotIdenticalNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final boolean doBoolean(final boolean a, final boolean b) {
            return a != b;
        }

        @Specialization
        protected static final boolean doChar(final char a, final char b) {
            return a != b;
        }

        @Specialization
        protected static final boolean doLong(final long a, final long b) {
            return a != b;
        }

        @Specialization
        protected static final boolean doDouble(final double a, final double b) {
            return a != b;
        }

        @Specialization
        protected static final boolean doFloat(final FloatObject a, final FloatObject b) {
            return a != b && !doDouble(a.getValue(), b.getValue());
        }

        @Specialization
        protected static final boolean doObject(final Object a, final Object b) {
            return !a.equals(b);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 185)
    protected abstract static class PrimExitCriticalSectionNode extends AbstractPrimitiveWithPushNode {
        @Child private IsEmptyListNode isEmptyListNode;
        @Child private RemoveFirstLinkOfListNode removeFirstLinkOfListNode;
        @Child private ResumeProcessNode resumeProcessNode;

        public PrimExitCriticalSectionNode(final CompiledMethodObject method) {
            super(method);
            isEmptyListNode = IsEmptyListNode.create(method.image);
            removeFirstLinkOfListNode = RemoveFirstLinkOfListNode.create(method.image);
            resumeProcessNode = ResumeProcessNode.create(method.image);
        }

        @Specialization
        protected Object doExit(final VirtualFrame frame, final PointersObject mutex) {
            pushNode.executeWrite(frame, mutex); // keep receiver on stack
            if (isEmptyListNode.executeIsEmpty(mutex)) {
                mutex.atput0(MUTEX.OWNER, code.image.nil);
            } else {
                final BaseSqueakObject owningProcess = removeFirstLinkOfListNode.executeRemove(mutex);
                mutex.atput0(MUTEX.OWNER, owningProcess);
                resumeProcessNode.executeResume(frame, owningProcess);
            }
            throw new PrimitiveWithoutResultException();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 186)
    protected abstract static class PrimEnterCriticalSectionNode extends AbstractPrimitiveWithPushNode {
        @Child private GetActiveProcessNode getActiveProcessNode;
        @Child private LinkProcessToListNode linkProcessToListNode;
        @Child private WakeHighestPriorityNode wakeHighestPriorityNode;

        public PrimEnterCriticalSectionNode(final CompiledMethodObject method) {
            super(method);
            getActiveProcessNode = GetActiveProcessNode.create(method.image);
            linkProcessToListNode = LinkProcessToListNode.create(method.image);
            wakeHighestPriorityNode = WakeHighestPriorityNode.create(method.image);
        }

        @Override
        public final Object executeWithArguments(final VirtualFrame frame, final Object... arguments) {
            return doEnter(frame, arguments);
        }

        @Specialization
        protected Object doEnter(final VirtualFrame frame, final Object[] rcvrAndArguments) {
            final PointersObject mutex = (PointersObject) rcvrAndArguments[0];
            final PointersObject activeProcess;
            if (rcvrAndArguments.length == 2) {
                activeProcess = (PointersObject) rcvrAndArguments[1];
            } else {
                activeProcess = getActiveProcessNode.executeGet();
            }
            final Object owner = mutex.at0(MUTEX.OWNER);
            if (owner == code.image.nil) {
                mutex.atput0(MUTEX.OWNER, activeProcess);
                pushNode.executeWrite(frame, code.image.sqFalse);
            } else if (owner == activeProcess) {
                pushNode.executeWrite(frame, code.image.sqTrue);
            } else {
                pushNode.executeWrite(frame, code.image.sqFalse);
                linkProcessToListNode.executeLink(activeProcess, mutex);
                wakeHighestPriorityNode.executeWake(frame);
            }
            throw new PrimitiveWithoutResultException();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 187)
    protected abstract static class PrimTestAndSetOwnershipOfCriticalSectionNode extends AbstractPrimitiveNode {
        @Child private GetActiveProcessNode getActiveProcessNode;

        public PrimTestAndSetOwnershipOfCriticalSectionNode(final CompiledMethodObject method) {
            super(method);
            getActiveProcessNode = GetActiveProcessNode.create(method.image);
        }

        @Specialization
        protected Object doTest(final PointersObject rcvrMutex) {
            final PointersObject activeProcess = getActiveProcessNode.executeGet();
            final Object owner = rcvrMutex.at0(MUTEX.OWNER);
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
    @SqueakPrimitive(index = 188, numArguments = 3)
    protected abstract static class PrimExecuteMethodArgsArray extends AbstractPerformPrimitiveNode {
        @Child private DispatchNode dispatchNode = DispatchNode.create();

        protected PrimExecuteMethodArgsArray(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object doExecute(final VirtualFrame frame, final Object receiver, final ListObject argArray, final CompiledCodeObject codeObject) {
            final int numArgs = argArray.size();
            final Object[] dispatchRcvrAndArgs = new Object[1 + numArgs];
            dispatchRcvrAndArgs[0] = receiver;
            for (int i = 0; i < numArgs; i++) {
                dispatchRcvrAndArgs[1 + i] = argArray.at0(i);
            }
            final Object thisContext = FrameAccess.getContextOrMarker(frame);
            return dispatchNode.executeDispatch(frame, codeObject, dispatchRcvrAndArgs, thisContext);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 230, numArguments = 2)
    protected abstract static class PrimRelinquishProcessorNode extends AbstractPrimitiveNode {

        public PrimRelinquishProcessorNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected BaseSqueakObject doRelinquish(final VirtualFrame frame, final BaseSqueakObject receiver, final long timeMicroseconds) {
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
    protected abstract static class PrimForceDisplayUpdateNode extends AbstractPrimitiveNode {
        protected PrimForceDisplayUpdateNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final BaseSqueakObject doForceUpdate(final BaseSqueakObject receiver) {
            code.image.display.forceUpdate();
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 233, numArguments = 2)
    protected abstract static class PrimSetFullScreenNode extends AbstractPrimitiveNode {
        protected PrimSetFullScreenNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final BaseSqueakObject doFullScreen(final BaseSqueakObject receiver, final boolean enable) {
            code.image.display.setFullscreen(enable);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 256)
    protected abstract static class PrimQuickReturnSelfNode extends AbstractPrimitiveNode {
        protected PrimQuickReturnSelfNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object returnValue(final Object receiver) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 257)
    protected abstract static class PrimQuickReturnTrueNode extends AbstractPrimitiveNode {
        protected PrimQuickReturnTrueNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object returnValue(@SuppressWarnings("unused") final Object receiver) {
            return code.image.sqTrue;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 258)
    protected abstract static class PrimQuickReturnFalseNode extends AbstractPrimitiveNode {
        protected PrimQuickReturnFalseNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object returnValue(@SuppressWarnings("unused") final Object receiver) {
            return code.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 259)
    protected abstract static class PrimQuickReturnNilNode extends AbstractPrimitiveNode {
        protected PrimQuickReturnNilNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object returnValue(@SuppressWarnings("unused") final Object receiver) {
            return code.image.nil;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 260)
    protected abstract static class PrimQuickReturnMinusOneNode extends AbstractPrimitiveNode {
        protected PrimQuickReturnMinusOneNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object returnValue(@SuppressWarnings("unused") final Object receiver) {
            return -1L;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 261)
    protected abstract static class PrimQuickReturnZeroNode extends AbstractPrimitiveNode {
        protected PrimQuickReturnZeroNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object returnValue(@SuppressWarnings("unused") final Object receiver) {
            return 0L;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 262)
    protected abstract static class PrimQuickReturnOneNode extends AbstractPrimitiveNode {
        protected PrimQuickReturnOneNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object returnValue(@SuppressWarnings("unused") final Object receiver) {
            return 1L;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 263)
    protected abstract static class PrimQuickReturnTwoNode extends AbstractPrimitiveNode {
        protected PrimQuickReturnTwoNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object returnValue(@SuppressWarnings("unused") final Object receiver) {
            return 2L;
        }
    }

    @GenerateNodeFactory
    public abstract static class PrimQuickReturnReceiverVariableNode extends AbstractPrimitiveNode {
        @Child private ObjectAtNode receiverVariableNode;

        public static PrimQuickReturnReceiverVariableNode create(final CompiledMethodObject method, final long variableIndex) {
            return PrimQuickReturnReceiverVariableNodeFactory.create(method, variableIndex, new SqueakNode[0]);
        }

        protected PrimQuickReturnReceiverVariableNode(final CompiledMethodObject method, final long variableIndex) {
            super(method);
            receiverVariableNode = ObjectAtNode.create(variableIndex, ReceiverNode.create(method));
        }

        @Specialization
        protected Object receiverVariable(final VirtualFrame frame) {
            return receiverVariableNode.executeGeneric(frame);
        }
    }
}
