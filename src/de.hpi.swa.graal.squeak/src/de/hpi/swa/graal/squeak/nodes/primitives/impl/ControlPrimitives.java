package de.hpi.swa.graal.squeak.nodes.primitives.impl;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveWithoutResultException;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakQuit;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.CharacterObject;
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
import de.hpi.swa.graal.squeak.nodes.LookupMethodNode;
import de.hpi.swa.graal.squeak.nodes.SqueakGuards;
import de.hpi.swa.graal.squeak.nodes.SqueakNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectSizeNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.GetObjectArrayNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ReadArrayObjectNode;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodes.NativeGetBytesNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectSizeNode;
import de.hpi.swa.graal.squeak.nodes.context.LookupClassNode;
import de.hpi.swa.graal.squeak.nodes.context.ReceiverNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.CreateEagerArgumentsNode;
import de.hpi.swa.graal.squeak.nodes.context.stack.StackPushForPrimitivesNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.ControlPrimitivesFactory.PrimExternalCallNodeFactory.GetAbstractPrimitiveNodeGen;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.ControlPrimitivesFactory.PrimExternalCallNodeFactory.GetNamedPrimitiveNodeGen;
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
import de.hpi.swa.graal.squeak.util.InterruptHandlerNode;
import de.hpi.swa.graal.squeak.util.MiscUtils;

public final class ControlPrimitives extends AbstractPrimitiveFactoryHolder {

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
        protected final Object fail(@SuppressWarnings("unused") final VirtualFrame frame) {
            code.image.printVerbose("Primitive not yet written: ", code);
            throw new PrimitiveFailed();
        }
    }

    // primitiveBlockCopy / primitiveBlockValue: (#80, #81, #82) no longer needed.

    private abstract static class AbstractPerformPrimitiveNode extends AbstractPrimitiveNode {
        @Child protected LookupMethodNode lookupMethodNode;
        @Child protected LookupClassNode lookupClassNode;
        @Child private DispatchSendNode dispatchSendNode;

        protected AbstractPerformPrimitiveNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
            lookupMethodNode = LookupMethodNode.create(code.image);
            lookupClassNode = LookupClassNode.create(code.image);
            dispatchSendNode = DispatchSendNode.create(code.image);
        }

        protected final ClassObject lookup(final Object receiver) {
            return lookupClassNode.executeLookup(receiver);
        }

        protected final Object dispatch(final VirtualFrame frame, final NativeObject selector, final Object[] rcvrAndArgs, final ClassObject rcvrClass) {
            final Object lookupResult = lookupMethodNode.executeLookup(rcvrClass, selector);
            final Object contextOrMarker = getContextOrMarker(frame);
            return dispatchSendNode.executeSend(frame, selector, lookupResult, rcvrClass, rcvrAndArgs, contextOrMarker);
        }
    }

    private abstract static class AbstractPrimitiveWithPushNode extends AbstractPrimitiveNode {
        @Child protected StackPushForPrimitivesNode pushNode;

        protected AbstractPrimitiveWithPushNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
            pushNode = StackPushForPrimitivesNode.create();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 83)
    protected abstract static class PrimPerformNode extends AbstractPerformPrimitiveNode {

        protected PrimPerformNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected final Object perform(final VirtualFrame frame, final Object receiver, final NativeObject selector, final NotProvided object1, final NotProvided object2, final NotProvided object3,
                        final NotProvided object4, final NotProvided object5) {
            final ClassObject rcvrClass = lookup(receiver);
            return dispatch(frame, selector, new Object[]{receiver}, rcvrClass);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isNotProvided(object1)"})
        protected final Object perform(final VirtualFrame frame, final Object receiver, final NativeObject selector, final Object object1, final NotProvided object2, final NotProvided object3,
                        final NotProvided object4, final NotProvided object5) {
            final ClassObject rcvrClass = lookup(receiver);
            return dispatch(frame, selector, new Object[]{receiver, object1}, rcvrClass);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isNotProvided(object1)", "!isNotProvided(object2)"})
        protected final Object perform(final VirtualFrame frame, final Object receiver, final NativeObject selector, final Object object1, final Object object2, final NotProvided object3,
                        final NotProvided object4, final NotProvided object5) {
            final ClassObject rcvrClass = lookup(receiver);
            return dispatch(frame, selector, new Object[]{receiver, object1, object2}, rcvrClass);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isNotProvided(object1)", "!isNotProvided(object2)", "!isNotProvided(object3)"})
        protected final Object perform(final VirtualFrame frame, final Object receiver, final NativeObject selector, final Object object1, final Object object2, final Object object3,
                        final NotProvided object4, final NotProvided object5) {
            final ClassObject rcvrClass = lookup(receiver);
            return dispatch(frame, selector, new Object[]{receiver, object1, object2, object3}, rcvrClass);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isNotProvided(object1)", "!isNotProvided(object2)", "!isNotProvided(object3)", "!isNotProvided(object4)"})
        protected final Object perform(final VirtualFrame frame, final Object receiver, final NativeObject selector, final Object object1, final Object object2, final Object object3,
                        final Object object4,
                        final NotProvided object5) {
            final ClassObject rcvrClass = lookup(receiver);
            return dispatch(frame, selector, new Object[]{receiver, object1, object2, object3, object4}, rcvrClass);
        }

        @Specialization(guards = {"!isNotProvided(object1)", "!isNotProvided(object2)", "!isNotProvided(object3)", "!isNotProvided(object4)", "!isNotProvided(object5)"})
        protected final Object perform(final VirtualFrame frame, final Object receiver, final NativeObject selector, final Object object1, final Object object2, final Object object3,
                        final Object object4,
                        final Object object5) {
            final ClassObject rcvrClass = lookup(receiver);
            return dispatch(frame, selector, new Object[]{receiver, object1, object2, object3, object4, object5}, rcvrClass);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 84)
    protected abstract static class PrimPerformWithArgumentsNode extends AbstractPerformPrimitiveNode {
        @Child private GetObjectArrayNode getObjectArrayNode = GetObjectArrayNode.create();

        protected PrimPerformWithArgumentsNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected Object perform(final VirtualFrame frame, final Object receiver, final NativeObject selector, final ArrayObject arguments) {
            return dispatch(frame, selector, ArrayUtils.copyWithFirst(getObjectArrayNode.execute(arguments), receiver), lookup(receiver));
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

        @Specialization(guards = "receiver.isSemaphore()")
        protected final AbstractSqueakObject doSignal(final VirtualFrame frame, final PointersObject receiver) {
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
            linkProcessToListNode = LinkProcessToListNode.create(method);
            wakeHighestPriorityNode = WakeHighestPriorityNode.create(method);
            getActiveProcessNode = GetActiveProcessNode.create(method.image);
        }

        @Specialization(guards = {"receiver.isSemaphore()", "hasExcessSignals(receiver)"})
        protected final AbstractSqueakObject doWaitExcessSignals(final VirtualFrame frame, final PointersObject receiver) {
            pushNode.executeWrite(frame, receiver); // keep receiver on stack
            final long excessSignals = (long) receiver.at0(SEMAPHORE.EXCESS_SIGNALS);
            receiver.atput0(SEMAPHORE.EXCESS_SIGNALS, excessSignals - 1);
            throw new PrimitiveWithoutResultException();
        }

        @Specialization(guards = {"receiver.isSemaphore()", "!hasExcessSignals(receiver)"})
        protected final AbstractSqueakObject doWait(final VirtualFrame frame, final PointersObject receiver) {
            pushNode.executeWrite(frame, receiver); // keep receiver on stack
            linkProcessToListNode.executeLink(getActiveProcessNode.executeGet(), receiver);
            wakeHighestPriorityNode.executeWake(frame);
            throw new PrimitiveWithoutResultException();
        }

        protected static final boolean hasExcessSignals(final PointersObject semaphore) {
            return (long) semaphore.at0(SEMAPHORE.EXCESS_SIGNALS) > 0;
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
        protected final AbstractSqueakObject doResume(final VirtualFrame frame, final PointersObject receiver) {
            // keep receiver on stack before resuming other process
            pushNode.executeWrite(frame, receiver);
            resumeProcessNode.executeResume(frame, receiver);
            throw new PrimitiveWithoutResultException();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 88)
    protected abstract static class PrimSuspendNode extends AbstractPrimitiveNode {
        @Child protected GetActiveProcessNode getActiveProcessNode;

        protected PrimSuspendNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
            getActiveProcessNode = GetActiveProcessNode.create(method.image);
        }

        @Specialization(guards = "receiver == getActiveProcessNode.executeGet()")
        protected final AbstractSqueakObject doSuspendActiveProcess(final VirtualFrame frame, @SuppressWarnings("unused") final PointersObject receiver,
                        @Cached("create()") final StackPushForPrimitivesNode pushNode,
                        @Cached("create(code)") final WakeHighestPriorityNode wakeHighestPriorityNode) {
            pushNode.executeWrite(frame, code.image.nil);
            wakeHighestPriorityNode.executeWake(frame);
            throw new PrimitiveWithoutResultException(); // result already pushed above
        }

        @Specialization(guards = "receiver != getActiveProcessNode.executeGet()")
        protected final Object doSuspendOtherProcess(final PointersObject receiver,
                        @Cached("create()") final SqueakObjectAt0Node at0Node,
                        @Cached("create(code.image)") final RemoveProcessFromListNode removeProcessNode) {
            final Object oldList = at0Node.execute(receiver, PROCESS.LIST);
            if (oldList == code.image.nil) {
                throw new PrimitiveFailed(ERROR_TABLE.BAD_RECEIVER);
            }
            removeProcessNode.executeRemove(receiver, oldList);
            receiver.atput0(PROCESS.LIST, code.image.nil);
            return oldList;
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
        @Child private GetObjectArrayNode getObjectArrayNode = GetObjectArrayNode.create();

        protected PrimPerformWithArgumentsInSuperclassNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final Object doPerform(final VirtualFrame frame, final Object receiver, final NativeObject selector, final ArrayObject arguments, final ClassObject superClass,
                        @SuppressWarnings("unused") final NotProvided np) {
            // Object>>#perform:withArguments:inSuperclass:
            return dispatch(frame, selector, ArrayUtils.copyWithFirst(getObjectArrayNode.execute(arguments), receiver), superClass);
        }

        @Specialization
        protected final Object doPerform(final VirtualFrame frame, @SuppressWarnings("unused") final Object receiver, final Object object, final NativeObject selector, final ArrayObject arguments,
                        final ClassObject superClass) {
            // Context>>#object:perform:withArguments:inClass:
            return dispatch(frame, selector, ArrayUtils.copyWithFirst(getObjectArrayNode.execute(arguments), object), superClass);
        }
    }

    @ImportStatic(Double.class)
    @GenerateNodeFactory
    @SqueakPrimitive(index = 110)
    protected abstract static class PrimIdenticalNode extends AbstractPrimitiveNode {
        protected PrimIdenticalNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final boolean doBoolean(final boolean a, final boolean b) {
            return a == b ? code.image.sqTrue : code.image.sqFalse;
        }

        @Specialization
        protected final boolean doChar(final char a, final char b) {
            return a == b ? code.image.sqTrue : code.image.sqFalse;
        }

        @SuppressWarnings("unused")
        @Specialization
        protected final boolean doChar(final CharacterObject a, final char b) {
            return code.image.sqFalse;
        }

        @SuppressWarnings("unused")
        @Specialization
        protected final boolean doChar(final char a, final CharacterObject b) {
            return code.image.sqFalse;
        }

        @Specialization
        protected final boolean doChar(final CharacterObject a, final CharacterObject b) {
            return a.getValue() == b.getValue() ? code.image.sqTrue : code.image.sqFalse;
        }

        @Specialization
        protected final boolean doLong(final long a, final long b) {
            return a == b ? code.image.sqTrue : code.image.sqFalse;
        }

        @Specialization(guards = {"!isNaN(a)", "!isNaN(b)"})
        protected final boolean doDouble(final double a, final double b) {
            return a == b ? code.image.sqTrue : code.image.sqFalse;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isNaN(a) || isNaN(b)"})
        protected final boolean doDoubleNaN(final double a, final double b) {
            return code.image.sqTrue;
        }

        @Specialization(guards = {"!isNaN(a.getValue())", "!isNaN(b.getValue())"})
        protected final boolean doFloat(final FloatObject a, final FloatObject b) {
            return a == b || doDouble(a.getValue(), b.getValue());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isNaN(a.getValue()) || isNaN(b.getValue())"})
        protected final boolean doFloatNaN(final FloatObject a, final FloatObject b) {
            return code.image.sqTrue;
        }

        @SuppressWarnings("unused")
        @Specialization
        protected final boolean doObject(final NilObject a, final NilObject b) {
            return code.image.sqTrue;
        }

        @Fallback
        protected final boolean doSqueakObject(final Object a, final Object b) {
            return a == b ? code.image.sqTrue : code.image.sqFalse;
        }
    }

    /*
     * primitiveClass (see Object>>class and Context>>objectClass:).
     */
    @NodeInfo(cost = NodeCost.NONE)
    @GenerateNodeFactory
    @SqueakPrimitive(index = 111)
    protected abstract static class PrimClassNode extends AbstractPrimitiveNode {
        private @Child LookupClassNode node;

        protected PrimClassNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
            node = LookupClassNode.create(code.image);
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
            return code.image.wrap(MiscUtils.runtimeFreeMemory());
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
        protected static final Object doQuit(final Object receiver, final NotProvided exitStatus) {
            throw new SqueakQuit(1);
        }

        @Specialization
        protected static final Object doQuit(@SuppressWarnings("unused") final Object receiver, final long exitStatus) {
            throw new SqueakQuit((int) exitStatus);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 114)
    protected abstract static class PrimExitToDebuggerNode extends AbstractPrimitiveNode {
        protected PrimExitToDebuggerNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final Object debugger(@SuppressWarnings("unused") final VirtualFrame frame) {
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
        protected static final Object doSmallInteger(final long receiver, final long argument) {
            throw new PrimitiveFailed();
        }

        @Specialization(guards = "receiver.haveSameStorageType(argument)")
        protected static final Object doNative(final NativeObject receiver, final NativeObject argument) {
            receiver.setSqueakClass(argument.getSqueakClass());
            throw new PrimitiveWithoutResultException();
        }

        @Specialization(guards = {"!receiver.haveSameStorageType(argument)", "argument.isByteType()"})
        protected final Object doNativeConvertToBytes(final NativeObject receiver, final NativeObject argument) {
            receiver.setSqueakClass(argument.getSqueakClass());
            receiver.convertToBytesStorage(getBytesNode.execute(receiver));
            throw new PrimitiveWithoutResultException();
        }

        @Specialization(guards = {"!receiver.haveSameStorageType(argument)", "argument.isShortType()"})
        protected final Object doNativeConvertToShorts(final NativeObject receiver, final NativeObject argument) {
            receiver.setSqueakClass(argument.getSqueakClass());
            receiver.convertToBytesStorage(getBytesNode.execute(receiver));
            throw new PrimitiveWithoutResultException();
        }

        @Specialization(guards = {"!receiver.haveSameStorageType(argument)", "argument.isIntType()"})
        protected final Object doNativeConvertToInts(final NativeObject receiver, final NativeObject argument) {
            receiver.setSqueakClass(argument.getSqueakClass());
            receiver.convertToBytesStorage(getBytesNode.execute(receiver));
            throw new PrimitiveWithoutResultException();
        }

        @Specialization(guards = {"!receiver.haveSameStorageType(argument)", "argument.isLongType()"})
        protected final Object doNativeConvertToLongs(final NativeObject receiver, final NativeObject argument) {
            receiver.setSqueakClass(argument.getSqueakClass());
            receiver.convertToBytesStorage(getBytesNode.execute(receiver));
            throw new PrimitiveWithoutResultException();
        }

        @Specialization(guards = "receiver.isByteType()")
        protected static final Object doNativeLargeInteger(final NativeObject receiver, final LargeIntegerObject argument) {
            receiver.setSqueakClass(argument.getSqueakClass());
            throw new PrimitiveWithoutResultException();
        }

        @Specialization(guards = "!receiver.isByteType()")
        protected final Object doNativeLargeIntegerConvert(final NativeObject receiver, final LargeIntegerObject argument) {
            receiver.setSqueakClass(argument.getSqueakClass());
            receiver.convertToBytesStorage(getBytesNode.execute(receiver));
            throw new PrimitiveWithoutResultException();
        }

        @Specialization(guards = "receiver.isByteType()")
        protected static final Object doNativeFloat(final NativeObject receiver, final FloatObject argument) {
            receiver.setSqueakClass(argument.getSqueakClass());
            throw new PrimitiveWithoutResultException();
        }

        @Specialization(guards = "!receiver.isByteType()")
        protected final Object doNativeFloatConvert(final NativeObject receiver, final FloatObject argument) {
            receiver.setSqueakClass(argument.getSqueakClass());
            receiver.convertToBytesStorage(getBytesNode.execute(receiver));
            throw new PrimitiveWithoutResultException();
        }

        @Specialization
        protected static final Object doLargeInteger(final LargeIntegerObject receiver, final LargeIntegerObject argument) {
            receiver.setSqueakClass(argument.getSqueakClass());
            receiver.setBytes(argument.getBytes());
            throw new PrimitiveWithoutResultException();
        }

        @Specialization
        protected final Object doLargeIntegerNative(final LargeIntegerObject receiver, final NativeObject argument) {
            receiver.setSqueakClass(argument.getSqueakClass());
            receiver.setBytes(getBytesNode.execute(argument));
            throw new PrimitiveWithoutResultException();
        }

        @Specialization
        protected static final Object doLargeIntegerFloat(final LargeIntegerObject receiver, final FloatObject argument) {
            receiver.setSqueakClass(argument.getSqueakClass());
            receiver.setBytes(argument.getBytes());
            throw new PrimitiveWithoutResultException();
        }

        @Specialization
        protected static final Object doFloat(final FloatObject receiver, final FloatObject argument) {
            receiver.setSqueakClass(argument.getSqueakClass());
            receiver.setBytes(argument.getBytes());
            throw new PrimitiveWithoutResultException();
        }

        @Specialization
        protected static final Object doFloatLargeInteger(final FloatObject receiver, final LargeIntegerObject argument) {
            receiver.setSqueakClass(argument.getSqueakClass());
            receiver.setBytes(argument.getBytes());
            throw new PrimitiveWithoutResultException();
        }

        @Specialization
        protected final Object doFloatNative(final FloatObject receiver, final NativeObject argument) {
            receiver.setSqueakClass(argument.getSqueakClass());
            receiver.setBytes(getBytesNode.execute(argument));
            throw new PrimitiveWithoutResultException();
        }

        @Specialization(guards = {"!isNativeObject(receiver)", "!isLargeIntegerObject(receiver)", "!isFloatObject(receiver)"})
        protected static final Object doSqueakObject(final AbstractSqueakObject receiver, final AbstractSqueakObject argument) {
            receiver.setSqueakClass(argument.getSqueakClass());
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
        @Child private GetAbstractPrimitiveNode getPrimitiveNode = GetAbstractPrimitiveNodeGen.create();
        @Child private CreateEagerArgumentsNode createEagerArgumentsNode;

        protected PrimExternalCallNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Override
        public final Object executeWithArguments(final VirtualFrame frame, final Object... receiverAndArguments) {
            final AbstractPrimitiveNode primitiveNode = getPrimitiveNode.execute(code, code.getLiteral(0));
            return replace(primitiveNode).executeWithArguments(frame, getCreateEagerArgumentsNode().executeCreate(primitiveNode.numArguments, receiverAndArguments));
        }

        @Specialization
        protected final Object doExternalCall(final VirtualFrame frame) {
            return replace(getPrimitiveNode.execute(code, code.getLiteral(0))).executePrimitive(frame);
        }

        private CreateEagerArgumentsNode getCreateEagerArgumentsNode() {
            if (createEagerArgumentsNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                createEagerArgumentsNode = insert(CreateEagerArgumentsNode.create());
            }
            return createEagerArgumentsNode;
        }

        @ImportStatic(SqueakGuards.class)
        protected abstract static class GetAbstractPrimitiveNode extends Node {

            protected abstract AbstractPrimitiveNode execute(CompiledCodeObject method, Object literal);

            @Specialization
            protected static final AbstractPrimitiveNode doGet(final CompiledMethodObject method, final ArrayObject array,
                            @Cached("createGetNamedPrimitiveNode()") final GetNamedPrimitiveNode getNode) {
                final Object[] objects = array.getObjectStorage();
                return getNode.execute(method, objects[0], objects[1]);
            }

            @Fallback
            protected static final AbstractPrimitiveNode doFail(final CompiledCodeObject method, final Object value) {
                throw new SqueakException("Should not happen:", method, value);
            }

            protected static final GetNamedPrimitiveNode createGetNamedPrimitiveNode() {
                return GetNamedPrimitiveNodeGen.create();
            }
        }

        protected abstract static class GetNamedPrimitiveNode extends Node {

            protected abstract AbstractPrimitiveNode execute(CompiledMethodObject method, Object moduleName, Object functionName);

            @Specialization
            protected static final AbstractPrimitiveNode dof(final CompiledMethodObject method, final NativeObject moduleName, final NativeObject functionName) {
                return PrimitiveNodeFactory.forName(method, moduleName.asString(), functionName.asString());
            }

            @SuppressWarnings("unused")
            @Fallback
            protected static final AbstractPrimitiveNode doFail(final CompiledMethodObject method, final Object moduleName, final Object functionName) {
                return PrimitiveFailedNode.create(method);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 118)
    protected abstract static class PrimDoPrimitiveWithArgsNode extends AbstractPrimitiveNode {
        @Child protected GetObjectArrayNode getObjectArrayNode = GetObjectArrayNode.create();

        public PrimDoPrimitiveWithArgsNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = "primitiveNode != null")
        protected final Object doPrimitive(final VirtualFrame frame, final Object receiver, @SuppressWarnings("unused") final long primitiveIndex, final ArrayObject argumentArray,
                        @SuppressWarnings("unused") final NotProvided notProvided,
                        @Cached("forIndex(primitiveIndex)") final AbstractPrimitiveNode primitiveNode) {
            final Object[] receiverAndArguments = ArrayUtils.copyWithFirst(getObjectArrayNode.execute(argumentArray), receiver);
            return replace(primitiveNode).executeWithArguments(frame, receiverAndArguments);
        }

        @Specialization(guards = "primitiveNode != null")
        protected final Object doPrimitive(final VirtualFrame frame, @SuppressWarnings("unused") final Object context, final Object receiver,
                        @SuppressWarnings("unused") final long primitiveIndex, final ArrayObject argumentArray,
                        @Cached("forIndex(primitiveIndex)") final AbstractPrimitiveNode primitiveNode) {
            final Object[] receiverAndArguments = ArrayUtils.copyWithFirst(getObjectArrayNode.execute(argumentArray), receiver);
            return replace(primitiveNode).executeWithArguments(frame, receiverAndArguments);
        }

        protected final AbstractPrimitiveNode forIndex(final long primitiveIndex) {
            return PrimitiveNodeFactory.forIndex((CompiledMethodObject) code, (int) primitiveIndex);
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
            MiscUtils.systemGC();
            if (hasPendingFinalizations()) {
                code.image.interrupt.setPendingFinalizations(true);
            }
            return code.image.wrap(MiscUtils.runtimeFreeMemory());
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
            code.image.printVerbose(count, " WeakPointersObjects have been garbage collected.");
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
        protected final Object doIncrementalGC(final VirtualFrame frame, final AbstractSqueakObject receiver) {
            // It is not possible to suggest incremental GCs, so do not do anything here
            return code.image.wrap(MiscUtils.runtimeFreeMemory());
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

    @ImportStatic(Double.class)
    @GenerateNodeFactory
    @SqueakPrimitive(index = 169) // complements 110
    protected abstract static class PrimNotIdenticalNode extends AbstractPrimitiveNode {
        protected PrimNotIdenticalNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final boolean doBoolean(final boolean a, final boolean b) {
            return a != b ? code.image.sqTrue : code.image.sqFalse;
        }

        @Specialization
        protected final boolean doChar(final char a, final char b) {
            return a != b ? code.image.sqTrue : code.image.sqFalse;
        }

        @Specialization
        protected final boolean doLong(final long a, final long b) {
            return a != b ? code.image.sqTrue : code.image.sqFalse;
        }

        @Specialization(guards = {"!isNaN(a)", "!isNaN(b)"})
        protected final boolean doDouble(final double a, final double b) {
            return a != b ? code.image.sqTrue : code.image.sqFalse;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isNaN(a) || isNaN(b)"})
        protected final boolean doDoubleNaN(final double a, final double b) {
            return code.image.sqFalse;
        }

        @Specialization(guards = {"!isNaN(a.getValue())", "!isNaN(b.getValue())"})
        protected final boolean doFloat(final FloatObject a, final FloatObject b) {
            return a != b && !doDouble(a.getValue(), b.getValue());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isNaN(a.getValue()) || isNaN(b.getValue())"})
        protected final boolean doFloatNaN(final FloatObject a, final FloatObject b) {
            return code.image.sqFalse;
        }

        @Specialization
        protected final boolean doObject(final Object a, final Object b) {
            return !a.equals(b) ? code.image.sqTrue : code.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 185)
    protected abstract static class PrimExitCriticalSectionNode extends AbstractPrimitiveNode {
        @Child protected IsEmptyListNode isEmptyListNode;

        public PrimExitCriticalSectionNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
            isEmptyListNode = IsEmptyListNode.create(method.image);
        }

        @Specialization(guards = "isEmptyListNode.executeIsEmpty(mutex)")
        protected final Object doExitEmpty(final PointersObject mutex) {
            mutex.atput0(MUTEX.OWNER, code.image.nil);
            return mutex;
        }

        @Specialization(guards = "!isEmptyListNode.executeIsEmpty(mutex)")
        protected static final Object doExitNonEmpty(final VirtualFrame frame, final PointersObject mutex,
                        @Cached("create()") final StackPushForPrimitivesNode pushNode,
                        @Cached("create(code.image)") final RemoveFirstLinkOfListNode removeFirstLinkOfListNode,
                        @Cached("create(code)") final ResumeProcessNode resumeProcessNode) {
            pushNode.executeWrite(frame, mutex); // keep receiver on stack
            final Object owningProcess = removeFirstLinkOfListNode.executeRemove(mutex);
            mutex.atput0(MUTEX.OWNER, owningProcess);
            resumeProcessNode.executeResume(frame, owningProcess);
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
        }

        @Specialization(guards = "ownerIsNil(mutex)")
        protected final Object doEnterNilOwner(final PointersObject mutex, @SuppressWarnings("unused") final NotProvided notProvided) {
            mutex.atput0(MUTEX.OWNER, getGetActiveProcessNode().executeGet());
            return code.image.sqFalse;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "activeProcessMutexOwner(mutex)")
        protected final Object doEnterActiveProcessOwner(final PointersObject mutex, final NotProvided notProvided) {
            return code.image.sqTrue;
        }

        @Specialization(guards = {"!ownerIsNil(mutex)", "!activeProcessMutexOwner(mutex)"})
        protected final Object doEnter(final VirtualFrame frame, final PointersObject mutex, @SuppressWarnings("unused") final NotProvided notProvided) {
            getPushNode().executeWrite(frame, code.image.sqFalse);
            getLinkProcessToListNode().executeLink(getGetActiveProcessNode().executeGet(), mutex);
            getWakeHighestPriorityNode().executeWake(frame);
            throw new PrimitiveWithoutResultException();
        }

        @Specialization(guards = "ownerIsNil(mutex)")
        protected final Object doEnterNilOwner(final PointersObject mutex, @SuppressWarnings("unused") final PointersObject effectiveProcess) {
            mutex.atput0(MUTEX.OWNER, effectiveProcess);
            return code.image.sqFalse;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "isMutexOwner(mutex, effectiveProcess)")
        protected final Object doEnterActiveProcessOwner(final PointersObject mutex, final PointersObject effectiveProcess) {
            return code.image.sqTrue;
        }

        @Specialization(guards = {"!ownerIsNil(mutex)", "!isMutexOwner(mutex, effectiveProcess)"})
        protected final Object doEnter(final VirtualFrame frame, final PointersObject mutex, @SuppressWarnings("unused") final PointersObject effectiveProcess) {
            getPushNode().executeWrite(frame, code.image.sqFalse);
            getLinkProcessToListNode().executeLink(effectiveProcess, mutex);
            getWakeHighestPriorityNode().executeWake(frame);
            throw new PrimitiveWithoutResultException();
        }

        protected final boolean ownerIsNil(final PointersObject mutex) {
            return mutex.at0(MUTEX.OWNER) == code.image.nil;
        }

        protected final boolean activeProcessMutexOwner(final PointersObject mutex) {
            return mutex.at0(MUTEX.OWNER) == getGetActiveProcessNode().executeGet();
        }

        protected static final boolean isMutexOwner(final PointersObject mutex, final PointersObject effectiveProcess) {
            return mutex.at0(MUTEX.OWNER) == effectiveProcess;
        }

        private StackPushForPrimitivesNode getPushNode() {
            if (pushNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                pushNode = insert(StackPushForPrimitivesNode.create());
            }
            return pushNode;
        }

        private GetActiveProcessNode getGetActiveProcessNode() {
            if (getActiveProcessNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getActiveProcessNode = insert(GetActiveProcessNode.create(code.image));
            }
            return getActiveProcessNode;
        }

        private LinkProcessToListNode getLinkProcessToListNode() {
            if (linkProcessToListNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                linkProcessToListNode = insert(LinkProcessToListNode.create(code));
            }
            return linkProcessToListNode;
        }

        private WakeHighestPriorityNode getWakeHighestPriorityNode() {
            if (wakeHighestPriorityNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                wakeHighestPriorityNode = insert(WakeHighestPriorityNode.create(code));
            }
            return wakeHighestPriorityNode;
        }
    }

    @ImportStatic(MUTEX.class)
    @GenerateNodeFactory
    @SqueakPrimitive(index = 187)
    protected abstract static class PrimTestAndSetOwnershipOfCriticalSectionNode extends AbstractPrimitiveNode {
        @Child private GetActiveProcessNode getActiveProcessNode;

        public PrimTestAndSetOwnershipOfCriticalSectionNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
            getActiveProcessNode = GetActiveProcessNode.create(method.image);
        }

        @Specialization(guards = {"ownerIsNil(rcvrMutex)"})
        protected final boolean doNilOwner(final PointersObject rcvrMutex) {
            rcvrMutex.atput0(MUTEX.OWNER, getActiveProcessNode.executeGet());
            return code.image.sqFalse;
        }

        @Specialization(guards = {"ownerIsActiveProcess(rcvrMutex)"})
        protected final boolean doOwnerIsActiveProcess(@SuppressWarnings("unused") final PointersObject rcvrMutex) {
            return code.image.sqTrue;
        }

        @Specialization(guards = {"!ownerIsNil(rcvrMutex)", "!ownerIsActiveProcess(rcvrMutex)"})
        protected final Object doFallback(@SuppressWarnings("unused") final PointersObject rcvrMutex) {
            return code.image.nil;
        }

        protected final boolean ownerIsNil(final PointersObject mutex) {
            return mutex.at0(MUTEX.OWNER) == code.image.nil;
        }

        protected final boolean ownerIsActiveProcess(final PointersObject mutex) {
            return mutex.at0(MUTEX.OWNER) == getActiveProcessNode.executeGet();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 188)
    protected abstract static class PrimExecuteMethodArgsArrayNode extends AbstractPerformPrimitiveNode {
        @Child private DispatchNode dispatchNode = DispatchNode.create();
        @Child private ArrayObjectSizeNode sizeNode = ArrayObjectSizeNode.create();
        @Child private ReadArrayObjectNode readNode = ReadArrayObjectNode.create();

        protected PrimExecuteMethodArgsArrayNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final Object doExecute(final VirtualFrame frame, final Object receiver, final ArrayObject argArray, final CompiledCodeObject codeObject) {
            final int numArgs = sizeNode.execute(argArray);
            final Object[] dispatchRcvrAndArgs = new Object[1 + numArgs];
            dispatchRcvrAndArgs[0] = receiver;
            for (int i = 0; i < numArgs; i++) {
                dispatchRcvrAndArgs[1 + i] = readNode.execute(argArray, i);
            }
            final Object thisContext = getContextOrMarker(frame);
            return dispatchNode.executeDispatch(frame, codeObject, dispatchRcvrAndArgs, thisContext);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 230)
    protected abstract static class PrimRelinquishProcessorNode extends AbstractPrimitiveNode {

        public PrimRelinquishProcessorNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = {"!code.image.interruptHandlerDisabled()"})
        protected static final AbstractSqueakObject doRelinquish(final VirtualFrame frame, final AbstractSqueakObject receiver, @SuppressWarnings("unused") final long timeMicroseconds,
                        @Cached("create()") final StackPushForPrimitivesNode pushNode,
                        @Cached("create(code)") final InterruptHandlerNode interruptNode) {
            // Keep receiver on stack, interrupt handler could trigger.
            pushNode.executeWrite(frame, receiver);
            /*
             * Perform forced interrupt check, otherwise control flow cannot continue when
             * idleProcess is running.
             */
            interruptNode.executeTrigger(frame);
            throw new PrimitiveWithoutResultException();
        }

        @Specialization(guards = {"code.image.interruptHandlerDisabled()"})
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
        protected static final AbstractSqueakObject doForceUpdate(final AbstractSqueakObject receiver) {
            return receiver; // Do nothing.
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 233)
    protected abstract static class PrimSetFullScreenNode extends AbstractPrimitiveNode {
        protected PrimSetFullScreenNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = "code.image.hasDisplay()")
        protected final AbstractSqueakObject doFullScreen(final AbstractSqueakObject receiver, final boolean enable) {
            code.image.getDisplay().setFullscreen(enable);
            return receiver;
        }

        @Specialization(guards = "!code.image.hasDisplay()")
        protected static final AbstractSqueakObject doFullScreenHeadless(final AbstractSqueakObject receiver, @SuppressWarnings("unused") final boolean enable) {
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

    @NodeInfo(cost = NodeCost.NONE)
    @GenerateNodeFactory
    public abstract static class PrimQuickReturnReceiverVariableNode extends AbstractPrimitiveNode {
        @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();
        @Child private ReceiverNode receiverNode;
        private final long variableIndex;

        public static PrimQuickReturnReceiverVariableNode create(final CompiledMethodObject method, final long variableIndex) {
            return PrimQuickReturnReceiverVariableNodeFactory.create(method, variableIndex, new SqueakNode[0]);
        }

        protected PrimQuickReturnReceiverVariableNode(final CompiledMethodObject method, final long variableIndex) {
            super(method, 1);
            this.variableIndex = variableIndex;
            receiverNode = ReceiverNode.create(method);
        }

        @Specialization
        protected final Object receiverVariable(final VirtualFrame frame) {
            return at0Node.execute(receiverNode.executeRead(frame), variableIndex);
        }
    }
}
