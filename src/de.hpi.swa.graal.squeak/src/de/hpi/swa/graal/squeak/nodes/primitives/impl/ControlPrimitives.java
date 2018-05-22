package de.hpi.swa.graal.squeak.nodes.primitives.impl;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveWithoutResultException;
import de.hpi.swa.graal.squeak.exceptions.SqueakException;
import de.hpi.swa.graal.squeak.exceptions.SqueakQuit;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.NotProvided;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.ERROR_TABLE;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.MUTEX;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.SEMAPHORE;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.WeakPointersObject;
import de.hpi.swa.graal.squeak.nodes.DispatchNode;
import de.hpi.swa.graal.squeak.nodes.DispatchSendNode;
import de.hpi.swa.graal.squeak.nodes.LookupNode;
import de.hpi.swa.graal.squeak.nodes.LookupNodeGen;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectSizeNode;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodes.NativeGetBytesNode;
import de.hpi.swa.graal.squeak.nodes.SqueakNode;
import de.hpi.swa.graal.squeak.nodes.context.ObjectAtNode;
import de.hpi.swa.graal.squeak.nodes.context.ReceiverAndArgumentsNode;
import de.hpi.swa.graal.squeak.nodes.context.ReceiverNode;
import de.hpi.swa.graal.squeak.nodes.context.SqueakLookupClassNode;
import de.hpi.swa.graal.squeak.nodes.context.SqueakLookupClassNodeGen;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotReadNode;
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

public class ControlPrimitives extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return ControlPrimitivesFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 19)
    public abstract static class PrimitiveFailedNode extends AbstractPrimitiveNode {

        protected PrimitiveFailedNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        public static PrimitiveFailedNode create(final CompiledMethodObject method) {
            return PrimitiveFailedNodeFactory.create(method, 1, null);
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
        @Child private DispatchSendNode dispatchSendNode;
        @Child private FrameSlotReadNode contextOrMarkerNode = FrameSlotReadNode.createForContextOrMarker();

        protected AbstractPerformPrimitiveNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
            lookupClassNode = SqueakLookupClassNodeGen.create(code.image);
            dispatchSendNode = DispatchSendNode.create(code.image);
        }

        protected final ClassObject lookup(final Object receiver) {
            return lookupClassNode.executeLookup(receiver);
        }

        protected final Object dispatch(final VirtualFrame frame, final NativeObject selector, final Object[] rcvrAndArgs, final ClassObject rcvrClass) {
            final Object lookupResult = lookupNode.executeLookup(rcvrClass, selector);
            final Object contextOrMarker = contextOrMarkerNode.executeRead(frame);
            return dispatchSendNode.executeSend(frame, selector, lookupResult, rcvrClass, rcvrAndArgs, contextOrMarker);
        }
    }

    private abstract static class AbstractPrimitiveWithPushNode extends AbstractPrimitiveNode {
        @Child protected StackPushNode pushNode = StackPushNode.create();

        protected AbstractPrimitiveWithPushNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 83)
    protected abstract static class PrimPerformNode extends AbstractPerformPrimitiveNode {
        @Child private ReceiverAndArgumentsNode rcvrAndArgsNode;

        protected PrimPerformNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
            rcvrAndArgsNode = ReceiverAndArgumentsNode.create();
        }

        @SuppressWarnings("unused")
        @Specialization
        protected Object perform(final VirtualFrame frame, final Object receiver, final NativeObject selector, final NotProvided object1, final NotProvided object2, final NotProvided object3,
                        final NotProvided object4, final NotProvided object5) {
            final ClassObject rcvrClass = lookup(receiver);
            return dispatch(frame, selector, new Object[]{receiver}, rcvrClass);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isNotProvided(object1)"})
        protected Object perform(final VirtualFrame frame, final Object receiver, final NativeObject selector, final Object object1, final NotProvided object2, final NotProvided object3,
                        final NotProvided object4, final NotProvided object5) {
            final ClassObject rcvrClass = lookup(receiver);
            return dispatch(frame, selector, new Object[]{receiver, object1}, rcvrClass);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isNotProvided(object1)", "!isNotProvided(object2)"})
        protected Object perform(final VirtualFrame frame, final Object receiver, final NativeObject selector, final Object object1, final Object object2, final NotProvided object3,
                        final NotProvided object4, final NotProvided object5) {
            final ClassObject rcvrClass = lookup(receiver);
            return dispatch(frame, selector, new Object[]{receiver, object1, object2}, rcvrClass);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isNotProvided(object1)", "!isNotProvided(object2)", "!isNotProvided(object3)"})
        protected Object perform(final VirtualFrame frame, final Object receiver, final NativeObject selector, final Object object1, final Object object2, final Object object3,
                        final NotProvided object4, final NotProvided object5) {
            final ClassObject rcvrClass = lookup(receiver);
            return dispatch(frame, selector, new Object[]{receiver, object1, object2, object3}, rcvrClass);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isNotProvided(object1)", "!isNotProvided(object2)", "!isNotProvided(object3)", "!isNotProvided(object4)"})
        protected Object perform(final VirtualFrame frame, final Object receiver, final NativeObject selector, final Object object1, final Object object2, final Object object3, final Object object4,
                        final NotProvided object5) {
            final ClassObject rcvrClass = lookup(receiver);
            return dispatch(frame, selector, new Object[]{receiver, object1, object2, object3, object4}, rcvrClass);
        }

        @Specialization(guards = {"!isNotProvided(object1)", "!isNotProvided(object2)", "!isNotProvided(object3)", "!isNotProvided(object4)", "!isNotProvided(object5)"})
        protected Object perform(final VirtualFrame frame, final Object receiver, final NativeObject selector, final Object object1, final Object object2, final Object object3, final Object object4,
                        final Object object5) {
            final ClassObject rcvrClass = lookup(receiver);
            return dispatch(frame, selector, new Object[]{receiver, object1, object2, object3, object4, object5}, rcvrClass);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 84)
    protected abstract static class PrimPerformWithArgumentsNode extends AbstractPerformPrimitiveNode {
        protected PrimPerformWithArgumentsNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected Object perform(final VirtualFrame frame, final Object receiver, final NativeObject selector, final PointersObject arguments) {
            return dispatch(frame, selector, arguments.unwrappedWithFirst(receiver), lookup(receiver));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 85)
    protected abstract static class PrimSignalNode extends AbstractPrimitiveWithPushNode {
        @Child private SignalSemaphoreNode signalSemaphoreNode;

        protected PrimSignalNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
            signalSemaphoreNode = SignalSemaphoreNode.create(method);
        }

        @Specialization(guards = "isSemaphore(receiver)")
        protected AbstractSqueakObject doSignal(final VirtualFrame frame, final PointersObject receiver) {
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

        protected PrimWaitNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
            linkProcessToListNode = LinkProcessToListNode.create(method.image);
            wakeHighestPriorityNode = WakeHighestPriorityNode.create(method);
            getActiveProcessNode = GetActiveProcessNode.create(method.image);
        }

        @Specialization(guards = "isSemaphore(receiver)")
        protected AbstractSqueakObject doWait(final VirtualFrame frame, final PointersObject receiver) {
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

        protected PrimResumeNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
            resumeProcessNode = ResumeProcessNode.create(method);
        }

        @Specialization
        protected AbstractSqueakObject doResume(final VirtualFrame frame, final PointersObject receiver) {
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
        @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();

        protected PrimSuspendNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
            removeProcessNode = RemoveProcessFromListNode.create(method.image);
            wakeHighestPriorityNode = WakeHighestPriorityNode.create(method);
            getActiveProcessNode = GetActiveProcessNode.create(method.image);
        }

        @Specialization
        protected AbstractSqueakObject doSuspend(final VirtualFrame frame, final PointersObject receiver) {
            final PointersObject activeProcess = getActiveProcessNode.executeGet();
            if (receiver == activeProcess) {
                pushNode.executeWrite(frame, code.image.nil);
                wakeHighestPriorityNode.executeWake(frame);
            } else {
                final Object oldList = at0Node.execute(receiver, PROCESS.LIST);
                if (oldList == code.image.nil) {
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

        public PrimFlushCacheNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final AbstractSqueakObject doFlush(final AbstractSqueakObject receiver) {
            // TODO: actually flush caches once there are some
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 100)
    protected abstract static class PrimPerformWithArgumentsInSuperclassNode extends AbstractPerformPrimitiveNode {

        protected PrimPerformWithArgumentsInSuperclassNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final Object doPerform(final VirtualFrame frame, final Object receiver, final NativeObject selector, final PointersObject arguments, final ClassObject superClass,
                        @SuppressWarnings("unused") final NotProvided np) {
            // Object>>#perform:withArguments:inSuperclass:
            return dispatch(frame, selector, arguments.unwrappedWithFirst(receiver), superClass);
        }

        @Specialization
        protected final Object doPerform(final VirtualFrame frame, @SuppressWarnings("unused") final Object receiver, final Object object, final NativeObject selector, final PointersObject arguments,
                        final ClassObject superClass) {
            // Context>>#object:perform:withArguments:inClass:
            return dispatch(frame, selector, arguments.unwrappedWithFirst(object), superClass);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 110)
    protected abstract static class PrimIdenticalNode extends AbstractPrimitiveNode {
        protected PrimIdenticalNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
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
    @SqueakPrimitive(index = 111)
    protected abstract static class PrimClassNode extends AbstractPrimitiveNode {
        private @Child SqueakLookupClassNode node;

        protected PrimClassNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
            node = SqueakLookupClassNode.create(code.image);
        }

        @Specialization
        protected final ClassObject doClass(final Object receiver, @SuppressWarnings("unused") final NotProvided object) {
            return node.executeLookup(receiver);
        }

        @Specialization(guards = "!isNotProvided(object)")
        protected final ClassObject doClass(@SuppressWarnings("unused") final Object receiver, final Object object) {
            return node.executeLookup(object);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 112)
    protected abstract static class PrimBytesLeftNode extends AbstractPrimitiveNode {

        protected PrimBytesLeftNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final Object doBytesLeft(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return code.image.wrap(Runtime.getRuntime().freeMemory());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 113)
    protected abstract static class PrimQuitNode extends AbstractPrimitiveNode {
        protected PrimQuitNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected Object doQuit(final Object receiver, final NotProvided errorCode) {
            throw new SqueakQuit(1);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected Object doQuit(final Object receiver, final long errorCode) {
            throw new SqueakQuit((int) errorCode);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 114)
    protected abstract static class PrimExitToDebuggerNode extends AbstractPrimitiveNode {
        protected PrimExitToDebuggerNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected Object debugger(@SuppressWarnings("unused") final VirtualFrame frame) {
            throw new SqueakException("EXIT TO DEBUGGER");
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 115)
    protected abstract static class PrimChangeClassNode extends AbstractPrimitiveNode {
        @Child private NativeGetBytesNode getBytesNode = NativeGetBytesNode.create();

        protected PrimChangeClassNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isSmallInteger(receiver)", "isSmallInteger(argument)"})
        protected Object doSmallInteger(final long receiver, final long argument) {
            throw new PrimitiveFailed();
        }

        @Specialization(guards = "receiver.haveSameStorageType(argument)")
        protected Object doNative(final NativeObject receiver, final NativeObject argument) {
            receiver.setSqClass(argument.getSqClass());
            throw new PrimitiveWithoutResultException();
        }

        @Specialization(guards = {"!receiver.haveSameStorageType(argument)", "argument.isByteType()"})
        protected Object doNativeConvertToBytes(final NativeObject receiver, final NativeObject argument) {
            receiver.setSqClass(argument.getSqClass());
            receiver.convertToBytesStorage(getBytesNode.execute(receiver));
            throw new PrimitiveWithoutResultException();
        }

        @Specialization(guards = {"!receiver.haveSameStorageType(argument)", "argument.isShortType()"})
        protected Object doNativeConvertToShorts(final NativeObject receiver, final NativeObject argument) {
            receiver.setSqClass(argument.getSqClass());
            receiver.convertToBytesStorage(getBytesNode.execute(receiver));
            throw new PrimitiveWithoutResultException();
        }

        @Specialization(guards = {"!receiver.haveSameStorageType(argument)", "argument.isIntType()"})
        protected Object doNativeConvertToInts(final NativeObject receiver, final NativeObject argument) {
            receiver.setSqClass(argument.getSqClass());
            receiver.convertToBytesStorage(getBytesNode.execute(receiver));
            throw new PrimitiveWithoutResultException();
        }

        @Specialization(guards = {"!receiver.haveSameStorageType(argument)", "argument.isLongType()"})
        protected Object doNativeConvertToLongs(final NativeObject receiver, final NativeObject argument) {
            receiver.setSqClass(argument.getSqClass());
            receiver.convertToBytesStorage(getBytesNode.execute(receiver));
            throw new PrimitiveWithoutResultException();
        }

        @Specialization(guards = "receiver.isByteType()")
        protected Object doNativeLargeInteger(final NativeObject receiver, final LargeIntegerObject argument) {
            receiver.setSqClass(argument.getSqClass());
            throw new PrimitiveWithoutResultException();
        }

        @Specialization(guards = "!receiver.isByteType()")
        protected Object doNativeLargeIntegerConvert(final NativeObject receiver, final LargeIntegerObject argument) {
            receiver.setSqClass(argument.getSqClass());
            receiver.convertToBytesStorage(getBytesNode.execute(receiver));
            throw new PrimitiveWithoutResultException();
        }

        @Specialization(guards = "receiver.isByteType()")
        protected Object doNativeFloat(final NativeObject receiver, final FloatObject argument) {
            receiver.setSqClass(argument.getSqClass());
            throw new PrimitiveWithoutResultException();
        }

        @Specialization(guards = "!receiver.isByteType()")
        protected Object doNativeFloatConvert(final NativeObject receiver, final FloatObject argument) {
            receiver.setSqClass(argument.getSqClass());
            receiver.convertToBytesStorage(getBytesNode.execute(receiver));
            throw new PrimitiveWithoutResultException();
        }

        @Specialization
        protected Object doLargeInteger(final LargeIntegerObject receiver, final LargeIntegerObject argument) {
            receiver.setSqClass(argument.getSqClass());
            receiver.setBytes(argument.getBytes());
            throw new PrimitiveWithoutResultException();
        }

        @Specialization
        protected Object doLargeIntegerNative(final LargeIntegerObject receiver, final NativeObject argument) {
            receiver.setSqClass(argument.getSqClass());
            receiver.setBytes(getBytesNode.execute(argument));
            throw new PrimitiveWithoutResultException();
        }

        @Specialization
        protected Object doLargeIntegerFloat(final LargeIntegerObject receiver, final FloatObject argument) {
            receiver.setSqClass(argument.getSqClass());
            receiver.setBytes(argument.getBytes());
            throw new PrimitiveWithoutResultException();
        }

        @Specialization
        protected Object doFloat(final FloatObject receiver, final FloatObject argument) {
            receiver.setSqClass(argument.getSqClass());
            receiver.setBytes(argument.getBytes());
            throw new PrimitiveWithoutResultException();
        }

        @Specialization
        protected Object doFloatLargeInteger(final FloatObject receiver, final LargeIntegerObject argument) {
            receiver.setSqClass(argument.getSqClass());
            receiver.setBytes(argument.getBytes());
            throw new PrimitiveWithoutResultException();
        }

        @Specialization
        protected Object doFloatNative(final FloatObject receiver, final NativeObject argument) {
            receiver.setSqClass(argument.getSqClass());
            receiver.setBytes(getBytesNode.execute(argument));
            throw new PrimitiveWithoutResultException();
        }

        @Specialization(guards = {"!isNativeObject(receiver)", "!isLargeInteger(receiver)", "!isFloat(receiver)"})
        protected Object doSqueakObject(final AbstractSqueakObject receiver, final AbstractSqueakObject argument) {
            receiver.setSqClass(argument.getSqClass());
            throw new PrimitiveWithoutResultException();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 116)
    protected abstract static class PrimFlushCacheByMethodNode extends AbstractPrimitiveNode {

        public PrimFlushCacheByMethodNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final AbstractSqueakObject doFlush(final AbstractSqueakObject receiver) {
            // TODO: actually flush caches once there are some
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 117)
    protected abstract static class PrimExternalCallNode extends AbstractPrimitiveNode {
        @Child private NativeGetBytesNode getBytes = NativeGetBytesNode.create();
        @Child private SqueakObjectSizeNode sizeNode = SqueakObjectSizeNode.create();
        @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();

        protected PrimExternalCallNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Override
        public final Object executeWithArguments(final VirtualFrame frame, final Object... receiverAndArguments) {
            final AbstractSqueakObject descriptor = code.getLiteral(0) instanceof AbstractSqueakObject ? (AbstractSqueakObject) code.getLiteral(0) : null;
            if (descriptor != null && descriptor.getSqClass() != null && sizeNode.execute(descriptor) >= 2) {
                final Object descriptorAt0 = at0Node.execute(descriptor, 0);
                final Object descriptorAt1 = at0Node.execute(descriptor, 1);
                if (descriptorAt0 instanceof NativeObject && descriptorAt1 instanceof NativeObject) {
                    final String moduleName = getBytes.executeAsString((NativeObject) descriptorAt0);
                    final String functionName = getBytes.executeAsString((NativeObject) descriptorAt1);
                    final AbstractPrimitiveNode primitiveNode = PrimitiveNodeFactory.forName((CompiledMethodObject) code, moduleName, functionName);
                    return replace(primitiveNode).executeWithArguments(frame, ArrayUtils.fillWith(receiverAndArguments, primitiveNode.numArguments, NotProvided.INSTANCE));
                }
            }
            return replace(PrimitiveFailedNode.create((CompiledMethodObject) code)).executePrimitive(frame);
        }

        @Specialization // FIXME: split with guards
        protected Object doExternalCall(final VirtualFrame frame) {
            final AbstractSqueakObject descriptor = code.getLiteral(0) instanceof AbstractSqueakObject ? (AbstractSqueakObject) code.getLiteral(0) : null;
            if (descriptor != null && descriptor.getSqClass() != null && sizeNode.execute(descriptor) >= 2) {
                final Object descriptorAt0 = at0Node.execute(descriptor, 0);
                final Object descriptorAt1 = at0Node.execute(descriptor, 1);
                if (descriptorAt0 instanceof NativeObject && descriptorAt1 instanceof NativeObject) {
                    final String moduleName = getBytes.executeAsString((NativeObject) descriptorAt0);
                    final String functionName = getBytes.executeAsString((NativeObject) descriptorAt1);
                    return replace(PrimitiveNodeFactory.forName((CompiledMethodObject) code, moduleName, functionName)).executePrimitive(frame);
                }
            }
            return replace(PrimitiveFailedNode.create((CompiledMethodObject) code)).executePrimitive(frame);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 119)
    protected abstract static class PrimFlushCacheSelectiveNode extends AbstractPrimitiveNode {

        public PrimFlushCacheSelectiveNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final AbstractSqueakObject doFlush(final AbstractSqueakObject receiver) {
            // TODO: actually flush caches once there are some
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 130)
    protected abstract static class PrimFullGCNode extends AbstractPrimitiveNode {

        protected PrimFullGCNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected final Object doGC(final VirtualFrame frame, final AbstractSqueakObject receiver) {
            System.gc();
            if (hasPendingFinalizations()) {
                code.image.interrupt.setPendingFinalizations();
            }
            return code.image.wrap(Runtime.getRuntime().freeMemory());
        }

        @TruffleBoundary
        private boolean hasPendingFinalizations() {
            final ReferenceQueue<Object> queue = WeakPointersObject.weakPointersQueue;
            Reference<? extends Object> element = queue.poll();
            int count = 0;
            while (element != null) {
                count++;
                element = queue.poll();
            }
            code.image.traceVerbose(count, " WeakPointersObjects have been garbage collected.");
            return count > 0;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 131)
    protected abstract static class PrimIncrementalGCNode extends AbstractPrimitiveNode {

        protected PrimIncrementalGCNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected final Object doGC(final VirtualFrame frame, final AbstractSqueakObject receiver) {
            System.gc();
            return code.image.wrap(Runtime.getRuntime().freeMemory());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 167)
    protected abstract static class PrimYieldNode extends AbstractPrimitiveWithPushNode {
        @Child private YieldProcessNode yieldProcessNode;

        public PrimYieldNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
            yieldProcessNode = YieldProcessNode.create(method);
        }

        @Specialization
        protected final Object doYield(final VirtualFrame frame, final PointersObject scheduler) {
            pushNode.executeWrite(frame, scheduler); // keep receiver on stack
            yieldProcessNode.executeYield(frame, scheduler);
            throw new PrimitiveWithoutResultException();
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 169) // complements 110
    protected abstract static class PrimNotIdenticalNode extends AbstractPrimitiveNode {
        protected PrimNotIdenticalNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
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
        protected final boolean doDouble(final double a, final double b) {
            if (Double.isNaN(a) && Double.isNaN(b)) {
                return code.image.sqFalse;
            } else {
                return a != b;
            }
        }

        @Specialization
        protected final boolean doFloat(final FloatObject a, final FloatObject b) {
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

        public PrimExitCriticalSectionNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
            isEmptyListNode = IsEmptyListNode.create(method.image);
            removeFirstLinkOfListNode = RemoveFirstLinkOfListNode.create(method.image);
            resumeProcessNode = ResumeProcessNode.create(method);
        }

        @Specialization
        protected Object doExit(final VirtualFrame frame, final PointersObject mutex) {
            pushNode.executeWrite(frame, mutex); // keep receiver on stack
            if (isEmptyListNode.executeIsEmpty(mutex)) {
                mutex.atput0(MUTEX.OWNER, code.image.nil);
            } else {
                final Object owningProcess = removeFirstLinkOfListNode.executeRemove(mutex);
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

        public PrimEnterCriticalSectionNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
            getActiveProcessNode = GetActiveProcessNode.create(method.image);
            linkProcessToListNode = LinkProcessToListNode.create(method.image);
            wakeHighestPriorityNode = WakeHighestPriorityNode.create(method);
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

        public PrimTestAndSetOwnershipOfCriticalSectionNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
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
    @SqueakPrimitive(index = 188)
    protected abstract static class PrimExecuteMethodArgsArrayNode extends AbstractPerformPrimitiveNode {
        @Child private DispatchNode dispatchNode = DispatchNode.create();
        @Child private FrameSlotReadNode contextOrMarkerNode = FrameSlotReadNode.createForContextOrMarker();

        protected PrimExecuteMethodArgsArrayNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final Object doExecute(final VirtualFrame frame, final Object receiver, final PointersObject argArray, final CompiledCodeObject codeObject) {
            final int numArgs = argArray.size();
            final Object[] dispatchRcvrAndArgs = new Object[1 + numArgs];
            dispatchRcvrAndArgs[0] = receiver;
            for (int i = 0; i < numArgs; i++) {
                dispatchRcvrAndArgs[1 + i] = argArray.at0(i);
            }
            final Object thisContext = contextOrMarkerNode.executeRead(frame);
            return dispatchNode.executeDispatch(frame, codeObject, dispatchRcvrAndArgs, thisContext);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 230)
    protected abstract static class PrimRelinquishProcessorNode extends AbstractPrimitiveNode {

        public PrimRelinquishProcessorNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = {"!code.image.config.disableInterruptHandler()"})
        protected final AbstractSqueakObject doRelinquish(final VirtualFrame frame, final AbstractSqueakObject receiver, final long timeMicroseconds) {
            code.image.interrupt.executeCheck(frame.materialize());
            sleepFor(timeMicroseconds / 1000);
            return receiver;
        }

        @TruffleBoundary
        private static void sleepFor(final long millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                CompilerDirectives.transferToInterpreter();
                e.printStackTrace();
            }
        }

        @Specialization(guards = {"code.image.config.disableInterruptHandler()"})
        protected static final AbstractSqueakObject doNothing(final AbstractSqueakObject receiver, @SuppressWarnings("unused") final long timeMicroseconds) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 231)
    protected abstract static class PrimForceDisplayUpdateNode extends AbstractPrimitiveNode {
        protected PrimForceDisplayUpdateNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final AbstractSqueakObject doForceUpdate(final AbstractSqueakObject receiver) {
            code.image.display.forceUpdate();
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 233)
    protected abstract static class PrimSetFullScreenNode extends AbstractPrimitiveNode {
        protected PrimSetFullScreenNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final AbstractSqueakObject doFullScreen(final AbstractSqueakObject receiver, final boolean enable) {
            code.image.display.setFullscreen(enable);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 256)
    protected abstract static class PrimQuickReturnSelfNode extends AbstractPrimitiveNode {
        protected PrimQuickReturnSelfNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final Object returnValue(final Object receiver) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 257)
    protected abstract static class PrimQuickReturnTrueNode extends AbstractPrimitiveNode {
        protected PrimQuickReturnTrueNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final Object returnValue(@SuppressWarnings("unused") final Object receiver) {
            return code.image.sqTrue;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 258)
    protected abstract static class PrimQuickReturnFalseNode extends AbstractPrimitiveNode {
        protected PrimQuickReturnFalseNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final Object returnValue(@SuppressWarnings("unused") final Object receiver) {
            return code.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 259)
    protected abstract static class PrimQuickReturnNilNode extends AbstractPrimitiveNode {
        protected PrimQuickReturnNilNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final Object returnValue(@SuppressWarnings("unused") final Object receiver) {
            return code.image.nil;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 260)
    protected abstract static class PrimQuickReturnMinusOneNode extends AbstractPrimitiveNode {
        protected PrimQuickReturnMinusOneNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final Object returnValue(@SuppressWarnings("unused") final Object receiver) {
            return -1L;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 261)
    protected abstract static class PrimQuickReturnZeroNode extends AbstractPrimitiveNode {
        protected PrimQuickReturnZeroNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final Object returnValue(@SuppressWarnings("unused") final Object receiver) {
            return 0L;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 262)
    protected abstract static class PrimQuickReturnOneNode extends AbstractPrimitiveNode {
        protected PrimQuickReturnOneNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final Object returnValue(@SuppressWarnings("unused") final Object receiver) {
            return 1L;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 263)
    protected abstract static class PrimQuickReturnTwoNode extends AbstractPrimitiveNode {
        protected PrimQuickReturnTwoNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final Object returnValue(@SuppressWarnings("unused") final Object receiver) {
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
            super(method, 1);
            receiverVariableNode = ObjectAtNode.create(variableIndex, ReceiverNode.create(method));
        }

        @Specialization
        protected final Object receiverVariable(final VirtualFrame frame) {
            return receiverVariableNode.executeGeneric(frame);
        }
    }
}
