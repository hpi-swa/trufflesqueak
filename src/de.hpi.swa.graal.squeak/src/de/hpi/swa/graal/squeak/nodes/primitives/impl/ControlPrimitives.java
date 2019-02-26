package de.hpi.swa.graal.squeak.nodes.primitives.impl;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.List;
import java.util.logging.Level;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveWithoutResultException;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakQuit;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
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
import de.hpi.swa.graal.squeak.nodes.InheritsFromNode;
import de.hpi.swa.graal.squeak.nodes.LookupMethodNode;
import de.hpi.swa.graal.squeak.nodes.LookupClassNodes.LookupClassNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectReadNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectSizeNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectToObjectArrayTransformNode;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodes.NativeGetBytesNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.graal.squeak.nodes.context.frame.CreateEagerArgumentsNode;
import de.hpi.swa.graal.squeak.nodes.context.stack.StackPushForPrimitivesNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.QuaternaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.QuinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.SeptenaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitiveWithoutFallback;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.graal.squeak.nodes.process.LinkProcessToListNode;
import de.hpi.swa.graal.squeak.nodes.process.RemoveProcessFromListNode;
import de.hpi.swa.graal.squeak.nodes.process.ResumeProcessNode;
import de.hpi.swa.graal.squeak.nodes.process.SignalSemaphoreNode;
import de.hpi.swa.graal.squeak.nodes.process.WakeHighestPriorityNode;
import de.hpi.swa.graal.squeak.nodes.process.YieldProcessNode;
import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;
import de.hpi.swa.graal.squeak.util.InterruptHandlerNode;
import de.hpi.swa.graal.squeak.util.MiscUtils;

public final class ControlPrimitives extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return ControlPrimitivesFactory.getFactories();
    }

    /*
     * This node is not in primitive table, so that lookups fail just like when a primitive is not
     * implemented. This way, the node does not fill any caches during dispatch.
     *
     * @SqueakPrimitive(indices = 19)
     */
    public static final class PrimitiveFailedNode extends AbstractPrimitiveNode {

        protected PrimitiveFailedNode(final CompiledMethodObject method) {
            super(method);
        }

        public static PrimitiveFailedNode create(final CompiledMethodObject method) {
            return new PrimitiveFailedNode(method);
        }

        @Override
        public Object executeWithArguments(final VirtualFrame frame, final Object... arguments) {
            throw new PrimitiveFailed();
        }

        @Override
        public Object executePrimitive(final VirtualFrame frameValue) {
            throw new PrimitiveFailed();
        }

        @Override
        public int getNumArguments() {
            return 0;
        }
    }

    // primitiveBlockCopy / primitiveBlockValue: (#80, #81, #82) no longer needed.

    private abstract static class AbstractPerformPrimitiveNode extends AbstractPrimitiveNode {
        @Child protected LookupMethodNode lookupMethodNode = LookupMethodNode.create();
        @Child protected LookupClassNode lookupClassNode;
        @Child private DispatchSendNode dispatchSendNode;

        protected AbstractPerformPrimitiveNode(final CompiledMethodObject method) {
            super(method);
        }

        protected final Object dispatch(final VirtualFrame frame, final NativeObject selector, final Object[] rcvrAndArgs) {
            return dispatch(frame, selector, lookupClass(rcvrAndArgs[0]), rcvrAndArgs);
        }

        protected final Object dispatch(final VirtualFrame frame, final NativeObject selector, final ClassObject rcvrClass, final Object[] rcvrAndArgs) {
            final Object lookupResult = lookupMethodNode.executeLookup(rcvrClass, selector);
            final Object contextOrMarker = getContextOrMarker(frame);
            return getDispatchSendNode().executeSend(frame, selector, lookupResult, rcvrClass, rcvrAndArgs, contextOrMarker);
        }

        protected final ClassObject lookupClass(final Object object) {
            return getLookupClassNode().executeLookup(object);
        }

        private DispatchSendNode getDispatchSendNode() {
            if (dispatchSendNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                dispatchSendNode = insert(DispatchSendNode.create(method.image));
            }
            return dispatchSendNode;
        }

        private LookupClassNode getLookupClassNode() {
            if (lookupClassNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                lookupClassNode = insert(LookupClassNode.create(method.image));
            }
            return lookupClassNode;
        }
    }

    private abstract static class AbstractPrimitiveWithPushNode extends AbstractPrimitiveNode {
        @Child protected StackPushForPrimitivesNode pushNode;

        protected AbstractPrimitiveWithPushNode(final CompiledMethodObject method) {
            super(method);
            pushNode = StackPushForPrimitivesNode.create();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 83)
    protected abstract static class PrimPerformNode extends AbstractPerformPrimitiveNode implements SeptenaryPrimitive {

        protected PrimPerformNode(final CompiledMethodObject method) {
            super(method);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected final Object perform(final VirtualFrame frame, final Object receiver, final NativeObject selector, final NotProvided object1, final NotProvided object2, final NotProvided object3,
                        final NotProvided object4, final NotProvided object5) {
            return dispatch(frame, selector, new Object[]{receiver});
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isNotProvided(object1)"})
        protected final Object perform(final VirtualFrame frame, final Object receiver, final NativeObject selector, final Object object1, final NotProvided object2, final NotProvided object3,
                        final NotProvided object4, final NotProvided object5) {
            return dispatch(frame, selector, new Object[]{receiver, object1});
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isNotProvided(object1)", "!isNotProvided(object2)"})
        protected final Object perform(final VirtualFrame frame, final Object receiver, final NativeObject selector, final Object object1, final Object object2, final NotProvided object3,
                        final NotProvided object4, final NotProvided object5) {
            return dispatch(frame, selector, new Object[]{receiver, object1, object2});
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isNotProvided(object1)", "!isNotProvided(object2)", "!isNotProvided(object3)"})
        protected final Object perform(final VirtualFrame frame, final Object receiver, final NativeObject selector, final Object object1, final Object object2, final Object object3,
                        final NotProvided object4, final NotProvided object5) {
            return dispatch(frame, selector, new Object[]{receiver, object1, object2, object3});
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isNotProvided(object1)", "!isNotProvided(object2)", "!isNotProvided(object3)", "!isNotProvided(object4)"})
        protected final Object perform(final VirtualFrame frame, final Object receiver, final NativeObject selector, final Object object1, final Object object2, final Object object3,
                        final Object object4, final NotProvided object5) {
            return dispatch(frame, selector, new Object[]{receiver, object1, object2, object3, object4});
        }

        @Specialization(guards = {"!isNotProvided(object1)", "!isNotProvided(object2)", "!isNotProvided(object3)", "!isNotProvided(object4)", "!isNotProvided(object5)"})
        protected final Object perform(final VirtualFrame frame, final Object receiver, final NativeObject selector, final Object object1, final Object object2, final Object object3,
                        final Object object4, final Object object5) {
            return dispatch(frame, selector, new Object[]{receiver, object1, object2, object3, object4, object5});
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 84)
    protected abstract static class PrimPerformWithArgumentsNode extends AbstractPerformPrimitiveNode implements TernaryPrimitive {
        @Child private ArrayObjectToObjectArrayTransformNode getObjectArrayNode = ArrayObjectToObjectArrayTransformNode.create();

        protected PrimPerformWithArgumentsNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object perform(final VirtualFrame frame, final Object receiver, final NativeObject selector, final ArrayObject arguments) {
            return dispatch(frame, selector, getObjectArrayNode.executeWithFirst(arguments, receiver));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 85)
    protected abstract static class PrimSignalNode extends AbstractPrimitiveWithPushNode implements UnaryPrimitive {
        @Child private SignalSemaphoreNode signalSemaphoreNode;

        protected PrimSignalNode(final CompiledMethodObject method) {
            super(method);
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
    @SqueakPrimitive(indices = 86)
    protected abstract static class PrimWaitNode extends AbstractPrimitiveWithPushNode implements UnaryPrimitive {
        @Child private LinkProcessToListNode linkProcessToListNode;

        protected PrimWaitNode(final CompiledMethodObject method) {
            super(method);
            linkProcessToListNode = LinkProcessToListNode.create(method.image);
        }

        @Specialization(guards = {"receiver.isSemaphore()", "hasExcessSignals(receiver)"})
        protected final AbstractSqueakObject doWaitExcessSignals(final VirtualFrame frame, final PointersObject receiver) {
            pushNode.executeWrite(frame, receiver); // keep receiver on stack
            final long excessSignals = (long) receiver.at0(SEMAPHORE.EXCESS_SIGNALS);
            receiver.atput0(SEMAPHORE.EXCESS_SIGNALS, excessSignals - 1);
            throw new PrimitiveWithoutResultException();
        }

        @Specialization(guards = {"receiver.isSemaphore()", "!hasExcessSignals(receiver)"})
        protected final AbstractSqueakObject doWait(final VirtualFrame frame, final PointersObject receiver,
                        @Cached("create(method)") final WakeHighestPriorityNode wakeHighestPriorityNode) {
            pushNode.executeWrite(frame, receiver); // keep receiver on stack
            linkProcessToListNode.executeLink(method.image.getActiveProcess(), receiver);
            wakeHighestPriorityNode.executeWake(frame);
            throw new PrimitiveWithoutResultException();
        }

        protected static final boolean hasExcessSignals(final PointersObject semaphore) {
            return (long) semaphore.at0(SEMAPHORE.EXCESS_SIGNALS) > 0;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 87)
    protected abstract static class PrimResumeNode extends AbstractPrimitiveWithPushNode implements UnaryPrimitive {
        @Child private ResumeProcessNode resumeProcessNode;

        protected PrimResumeNode(final CompiledMethodObject method) {
            super(method);
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
    @SqueakPrimitive(indices = 88)
    protected abstract static class PrimSuspendNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimSuspendNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "receiver.isActiveProcess()")
        protected final AbstractSqueakObject doSuspendActiveProcess(final VirtualFrame frame, @SuppressWarnings("unused") final PointersObject receiver,
                        @Cached final StackPushForPrimitivesNode pushNode,
                        @Cached("create(method)") final WakeHighestPriorityNode wakeHighestPriorityNode) {
            pushNode.executeWrite(frame, method.image.nil);
            wakeHighestPriorityNode.executeWake(frame);
            throw new PrimitiveWithoutResultException(); // result already pushed above
        }

        @Specialization(guards = {"!receiver.isActiveProcess()", "!hasNilList(receiver)"})
        protected final PointersObject doSuspendOtherProcess(final PointersObject receiver,
                        @Cached("create(method.image)") final RemoveProcessFromListNode removeProcessNode) {
            final PointersObject oldList = (PointersObject) receiver.at0(PROCESS.LIST);
            removeProcessNode.executeRemove(receiver, oldList);
            receiver.atput0(PROCESS.LIST, method.image.nil);
            return oldList;
        }

        @Specialization(guards = {"!receiver.isActiveProcess()", "hasNilList(receiver)"})
        protected static final Object doBadReceiver(@SuppressWarnings("unused") final PointersObject receiver) {
            throw new PrimitiveFailed(ERROR_TABLE.BAD_RECEIVER);
        }

        protected final boolean hasNilList(final PointersObject process) {
            return process.at0(PROCESS.LIST) == method.image.nil;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 89)
    protected abstract static class PrimFlushCacheNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        public PrimFlushCacheNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final AbstractSqueakObject doFlush(final AbstractSqueakObject receiver) {
            // TODO: actually flush caches once there are some
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 100)
    protected abstract static class PrimPerformWithArgumentsInSuperclassNode extends AbstractPerformPrimitiveNode implements QuinaryPrimitive {
        @Child private ArrayObjectToObjectArrayTransformNode getObjectArrayNode = ArrayObjectToObjectArrayTransformNode.create();
        @Child protected InheritsFromNode inheritsFromNode;

        protected PrimPerformWithArgumentsInSuperclassNode(final CompiledMethodObject method) {
            super(method);
            inheritsFromNode = InheritsFromNode.create(method.image);
        }

        /*
         * Object>>#perform:withArguments:inSuperclass:
         */

        @Specialization(guards = "inheritsFromNode.execute(receiver, superClass)")
        protected final Object doPerform(final VirtualFrame frame, final Object receiver, final NativeObject selector, final ArrayObject arguments, final ClassObject superClass,
                        @SuppressWarnings("unused") final NotProvided np) {
            return dispatch(frame, selector, superClass, getObjectArrayNode.executeWithFirst(arguments, receiver));
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!inheritsFromNode.execute(receiver, superClass)")
        protected static final Object doFail(final Object receiver, final NativeObject selector, final ArrayObject arguments, final ClassObject superClass, final NotProvided np) {
            throw new PrimitiveFailed(ERROR_TABLE.BAD_RECEIVER);
        }

        /*
         * Context>>#object:perform:withArguments:inClass:
         */

        @Specialization(guards = "inheritsFromNode.execute(target, superClass)")
        protected final Object doPerform(final VirtualFrame frame, @SuppressWarnings("unused") final ContextObject receiver, final Object target, final NativeObject selector,
                        final ArrayObject arguments, final ClassObject superClass) {
            return dispatch(frame, selector, superClass, getObjectArrayNode.executeWithFirst(arguments, target));
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!inheritsFromNode.execute(target, superClass)")
        protected static final Object doFail(final ContextObject receiver, final Object target, final NativeObject selector, final ArrayObject arguments, final ClassObject superClass) {
            throw new PrimitiveFailed(ERROR_TABLE.BAD_RECEIVER);
        }
    }

    /** Complements {@link PrimNotIdenticalNode}. */
    @GenerateNodeFactory
    @SqueakPrimitive(indices = 110)
    protected abstract static class PrimIdenticalNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        protected PrimIdenticalNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final boolean doBoolean(final boolean a, final boolean b, @SuppressWarnings("unused") final NotProvided notProvided) {
            return a == b ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization
        protected final boolean doChar(final char a, final char b, @SuppressWarnings("unused") final NotProvided notProvided) {
            return a == b ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization
        protected final boolean doLong(final long a, final long b, @SuppressWarnings("unused") final NotProvided notProvided) {
            return a == b ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization
        protected final boolean doDouble(final double a, final double b, @SuppressWarnings("unused") final NotProvided notProvided) {
            return Double.doubleToRawLongBits(a) == Double.doubleToRawLongBits(b) ? method.image.sqTrue : method.image.sqFalse;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"a.getClass() != b.getClass()"})
        public final boolean doIncompatiblePrimitiveTypes(final Object a, final Object b, final NotProvided notProvided) {
            return method.image.sqFalse;
        }

        @Specialization(guards = {"a.getClass() == b.getClass()", "!isPrimitive(a) || !isPrimitive(b)"})
        public final boolean doSameClass(final Object a, final Object b, @SuppressWarnings("unused") final NotProvided notProvided) {
            return a == b ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization
        protected final boolean doBoolean(@SuppressWarnings("unused") final ContextObject context, final boolean a, final boolean b) {
            return a == b ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization
        protected final boolean doChar(@SuppressWarnings("unused") final ContextObject context, final char a, final char b) {
            return a == b ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization
        protected final boolean doLong(@SuppressWarnings("unused") final ContextObject context, final long a, final long b) {
            return a == b ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization
        protected final boolean doDouble(@SuppressWarnings("unused") final ContextObject context, final double a, final double b) {
            return Double.doubleToRawLongBits(a) == Double.doubleToRawLongBits(b) ? method.image.sqTrue : method.image.sqFalse;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"a.getClass() != b.getClass()", "!isPrimitive(a) || !isPrimitive(b)"})
        public final boolean doIncompatiblePrimitiveTypes(final ContextObject context, final Object a, final Object b) {
            return method.image.sqFalse;
        }

        @Specialization(guards = {"a.getClass() == b.getClass()", "!isPrimitive(a) || !isPrimitive(b)"})
        public final boolean doSameClass(@SuppressWarnings("unused") final ContextObject context, final Object a, final Object b) {
            return a == b ? method.image.sqTrue : method.image.sqFalse;
        }
    }

    /*
     * primitiveClass (see Object>>class and Context>>objectClass:).
     */
    @NodeInfo(cost = NodeCost.NONE)
    @GenerateNodeFactory
    @SqueakPrimitive(indices = 111)
    protected abstract static class PrimClassNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        private @Child LookupClassNode node;

        protected PrimClassNode(final CompiledMethodObject method) {
            super(method);
            node = LookupClassNode.create(method.image);
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
    @SqueakPrimitive(indices = 112)
    protected abstract static class PrimBytesLeftNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimBytesLeftNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final long doBytesLeft(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return method.image.wrap(MiscUtils.runtimeFreeMemory());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 113)
    protected abstract static class PrimQuitNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimQuitNode(final CompiledMethodObject method) {
            super(method);
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
    @SqueakPrimitive(indices = 114)
    protected abstract static class PrimExitToDebuggerNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        protected PrimExitToDebuggerNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object debugger(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            throw SqueakException.create("EXIT TO DEBUGGER");
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 115)
    protected abstract static class PrimChangeClassNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        @Child private NativeGetBytesNode getBytesNode;

        protected PrimChangeClassNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "receiver.haveSameStorageType(argument)")
        protected static final NativeObject doNative(final NativeObject receiver, final NativeObject argument) {
            receiver.setSqueakClass(argument.getSqueakClass());
            return receiver;
        }

        @Specialization(guards = {"!receiver.haveSameStorageType(argument)", "argument.isByteType()"})
        protected static final NativeObject doNativeConvertToBytes(final NativeObject receiver, final NativeObject argument,
                        @Shared("getBytesNode") @Cached final NativeGetBytesNode getBytesNode) {
            receiver.setSqueakClass(argument.getSqueakClass());
            receiver.convertToBytesStorage(getBytesNode.execute(receiver));
            return receiver;
        }

        @Specialization(guards = {"!receiver.haveSameStorageType(argument)", "argument.isShortType()"})
        protected static final NativeObject doNativeConvertToShorts(final NativeObject receiver, final NativeObject argument,
                        @Shared("getBytesNode") @Cached final NativeGetBytesNode getBytesNode) {
            receiver.setSqueakClass(argument.getSqueakClass());
            receiver.convertToBytesStorage(getBytesNode.execute(receiver));
            return receiver;
        }

        @Specialization(guards = {"!receiver.haveSameStorageType(argument)", "argument.isIntType()"})
        protected static final NativeObject doNativeConvertToInts(final NativeObject receiver, final NativeObject argument,
                        @Shared("getBytesNode") @Cached final NativeGetBytesNode getBytesNode) {
            receiver.setSqueakClass(argument.getSqueakClass());
            receiver.convertToBytesStorage(getBytesNode.execute(receiver));
            return receiver;
        }

        @Specialization(guards = {"!receiver.haveSameStorageType(argument)", "argument.isLongType()"})
        protected static final NativeObject doNativeConvertToLongs(final NativeObject receiver, final NativeObject argument,
                        @Shared("getBytesNode") @Cached final NativeGetBytesNode getBytesNode) {
            receiver.setSqueakClass(argument.getSqueakClass());
            receiver.convertToBytesStorage(getBytesNode.execute(receiver));
            return receiver;
        }

        @Specialization(guards = "receiver.isByteType()")
        protected static final NativeObject doNativeLargeInteger(final NativeObject receiver, final LargeIntegerObject argument) {
            receiver.setSqueakClass(argument.getSqueakClass());
            return receiver;
        }

        @Specialization(guards = "!receiver.isByteType()")
        protected static final NativeObject doNativeLargeIntegerConvert(final NativeObject receiver, final LargeIntegerObject argument,
                        @Shared("getBytesNode") @Cached final NativeGetBytesNode getBytesNode) {
            receiver.setSqueakClass(argument.getSqueakClass());
            receiver.convertToBytesStorage(getBytesNode.execute(receiver));
            return receiver;
        }

        @Specialization(guards = "receiver.isByteType()")
        protected static final NativeObject doNativeFloat(final NativeObject receiver, final FloatObject argument) {
            receiver.setSqueakClass(argument.getSqueakClass());
            return receiver;
        }

        @Specialization(guards = "!receiver.isByteType()")
        protected static final NativeObject doNativeFloatConvert(final NativeObject receiver, final FloatObject argument,
                        @Shared("getBytesNode") @Cached final NativeGetBytesNode getBytesNode) {
            receiver.setSqueakClass(argument.getSqueakClass());
            receiver.convertToBytesStorage(getBytesNode.execute(receiver));
            return receiver;
        }

        @Specialization
        protected static final LargeIntegerObject doLargeInteger(final LargeIntegerObject receiver, final LargeIntegerObject argument) {
            receiver.setSqueakClass(argument.getSqueakClass());
            receiver.setInteger(argument);
            return receiver;
        }

        @Specialization
        protected static final LargeIntegerObject doLargeIntegerNative(final LargeIntegerObject receiver, final NativeObject argument,
                        @Shared("getBytesNode") @Cached final NativeGetBytesNode getBytesNode) {
            receiver.setSqueakClass(argument.getSqueakClass());
            receiver.setBytes(getBytesNode.execute(argument));
            return receiver;
        }

        @Specialization
        protected static final LargeIntegerObject doLargeIntegerFloat(final LargeIntegerObject receiver, final FloatObject argument) {
            receiver.setSqueakClass(argument.getSqueakClass());
            receiver.setBytes(argument.getBytes());
            return receiver;
        }

        @Specialization
        protected static final FloatObject doFloat(final FloatObject receiver, final FloatObject argument) {
            receiver.setSqueakClass(argument.getSqueakClass());
            receiver.setBytes(argument.getBytes());
            return receiver;
        }

        @Specialization
        protected static final FloatObject doFloatLargeInteger(final FloatObject receiver, final LargeIntegerObject argument) {
            receiver.setSqueakClass(argument.getSqueakClass());
            receiver.setBytes(argument.getBytes());
            return receiver;
        }

        @Specialization
        protected static final FloatObject doFloatNative(final FloatObject receiver, final NativeObject argument,
                        @Shared("getBytesNode") @Cached final NativeGetBytesNode getBytesNode) {
            receiver.setSqueakClass(argument.getSqueakClass());
            receiver.setBytes(getBytesNode.execute(argument));
            return receiver;
        }

        @Specialization(guards = {"!isNativeObject(receiver)", "!isLargeIntegerObject(receiver)", "!isFloatObject(receiver)"})
        protected static final AbstractSqueakObject doSqueakObject(final AbstractSqueakObject receiver, final AbstractSqueakObject argument) {
            receiver.setSqueakClass(argument.getSqueakClass());
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 116)
    protected abstract static class PrimFlushCacheByMethodNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        public PrimFlushCacheByMethodNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final AbstractSqueakObject doFlush(final AbstractSqueakObject receiver) {
            // TODO: actually flush caches once there are some
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 117)
    protected abstract static class PrimExternalCallNode extends AbstractPrimitiveNode {
        @Child private CreateEagerArgumentsNode createEagerArgumentsNode;

        protected PrimExternalCallNode(final CompiledMethodObject method) {
            super(method);
        }

        @Override
        public final Object executeWithArguments(final VirtualFrame frame, final Object... receiverAndArguments) {
            final AbstractPrimitiveNode primitiveNode = method.image.primitiveNodeFactory.namedFor(method);
            return replace(primitiveNode).executeWithArguments(frame, getCreateEagerArgumentsNode().executeCreate(primitiveNode.getNumArguments(), receiverAndArguments));
        }

        @Specialization
        protected final Object doExternalCall(final VirtualFrame frame) {
            return replace(method.image.primitiveNodeFactory.namedFor(method)).executePrimitive(frame);
        }

        @Override
        public int getNumArguments() {
            return 0;
        }

        private CreateEagerArgumentsNode getCreateEagerArgumentsNode() {
            if (createEagerArgumentsNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                createEagerArgumentsNode = insert(CreateEagerArgumentsNode.create());
            }
            return createEagerArgumentsNode;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 118)
    protected abstract static class PrimDoPrimitiveWithArgsNode extends AbstractPrimitiveNode implements QuaternaryPrimitive {
        @Child protected ArrayObjectToObjectArrayTransformNode getObjectArrayNode = ArrayObjectToObjectArrayTransformNode.create();
        @Child protected CreateEagerArgumentsNode createEagerArgumentsNode = CreateEagerArgumentsNode.create();

        public PrimDoPrimitiveWithArgsNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final Object doPrimitiveWithArgs(final VirtualFrame frame, final Object receiver, final long primitiveIndex, final ArrayObject argumentArray,
                        @SuppressWarnings("unused") final NotProvided notProvided) {
            return doPrimitiveWithArgs(frame, receiver, primitiveIndex, argumentArray);
        }

        @Specialization
        protected final Object doPrimitiveWithArgs(final VirtualFrame frame, @SuppressWarnings("unused") final ContextObject context, final Object receiver,
                        final long primitiveIndex, final ArrayObject argumentArray) {
            return doPrimitiveWithArgs(frame, receiver, primitiveIndex, argumentArray);
        }

        private Object doPrimitiveWithArgs(final VirtualFrame frame, final Object receiver, final long primitiveIndex, final ArrayObject argumentArray) {
            /*
             * It is non-trivial to avoid the creation of a primitive node here. Deopt might be
             * acceptable because primitive is mostly used for debugging anyway.
             */
            final Object[] receiverAndArguments = getObjectArrayNode.executeWithFirst(argumentArray, receiver);
            final AbstractPrimitiveNode primitiveNode = method.image.primitiveNodeFactory.forIndex(method, (int) primitiveIndex);
            if (primitiveNode == null) {
                throw new PrimitiveFailed();
            }
            return replace(primitiveNode).executeWithArguments(frame, createEagerArgumentsNode.executeCreate(primitiveNode.getNumArguments(), receiverAndArguments));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 119)
    protected abstract static class PrimFlushCacheSelectiveNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        public PrimFlushCacheSelectiveNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final AbstractSqueakObject doFlush(final AbstractSqueakObject receiver) {
            // TODO: actually flush caches once there are some
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 130)
    protected abstract static class PrimFullGCNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        private static final TruffleLogger LOG = TruffleLogger.getLogger(SqueakLanguageConfig.ID, PrimFullGCNode.class);

        protected PrimFullGCNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final long doGC(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            MiscUtils.systemGC();
            if (hasPendingFinalizations()) {
                method.image.interrupt.setPendingFinalizations(true);
            }
            return method.image.wrap(MiscUtils.runtimeFreeMemory());
        }

        @TruffleBoundary
        private static boolean hasPendingFinalizations() {
            final ReferenceQueue<Object> queue = WeakPointersObject.weakPointersQueue;
            Reference<? extends Object> element = queue.poll();
            int count = 0;
            while (element != null) {
                count++;
                element = queue.poll();
            }
            LOG.log(Level.FINE, "Number of garbage collected WeakPointersObjects", count);
            return count > 0;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 131)
    protected abstract static class PrimIncrementalGCNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimIncrementalGCNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final long doIncrementalGC(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            // It is not possible to suggest incremental GCs, so do not do anything here
            return method.image.wrap(MiscUtils.runtimeFreeMemory());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 167)
    protected abstract static class PrimYieldNode extends AbstractPrimitiveWithPushNode implements UnaryPrimitive {
        @Child private YieldProcessNode yieldProcessNode;

        public PrimYieldNode(final CompiledMethodObject method) {
            super(method);
            yieldProcessNode = YieldProcessNode.create(method);
        }

        @Specialization
        protected final Object doYield(final VirtualFrame frame, final PointersObject scheduler) {
            pushNode.executeWrite(frame, scheduler); // keep receiver on stack
            yieldProcessNode.executeYield(frame, scheduler);
            throw new PrimitiveWithoutResultException();
        }

    }

    /** Complements {@link PrimIdenticalNode}. */
    @GenerateNodeFactory
    @SqueakPrimitive(indices = 169)
    protected abstract static class PrimNotIdenticalNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimNotIdenticalNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final boolean doBoolean(final boolean a, final boolean b) {
            return a != b ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization
        protected final boolean doChar(final char a, final char b) {
            return a != b ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization
        protected final boolean doLong(final long a, final long b) {
            return a != b ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization
        protected final boolean doDouble(final double a, final double b) {
            return Double.doubleToRawLongBits(a) != Double.doubleToRawLongBits(b) ? method.image.sqTrue : method.image.sqFalse;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"a.getClass() != b.getClass()"})
        public final boolean doIncompatiblePrimitiveTypes(final Object a, final Object b) {
            return method.image.sqTrue;
        }

        @Specialization(guards = {"a.getClass() == b.getClass()", "!isPrimitive(a) || !isPrimitive(b)"})
        public final boolean doSameClass(final Object a, final Object b) {
            return a != b ? method.image.sqTrue : method.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 185)
    protected abstract static class PrimExitCriticalSectionNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        public PrimExitCriticalSectionNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "mutex.isEmptyList()")
        protected final PointersObject doExitEmpty(final PointersObject mutex) {
            mutex.atput0(MUTEX.OWNER, method.image.nil);
            return mutex;
        }

        @Specialization(guards = "!mutex.isEmptyList()")
        protected static final Object doExitNonEmpty(final VirtualFrame frame, final PointersObject mutex,
                        @Cached final StackPushForPrimitivesNode pushNode,
                        @Cached("create(method)") final ResumeProcessNode resumeProcessNode) {
            pushNode.executeWrite(frame, mutex); // keep receiver on stack
            final PointersObject owningProcess = mutex.removeFirstLinkOfList();
            mutex.atput0(MUTEX.OWNER, owningProcess);
            resumeProcessNode.executeResume(frame, owningProcess);
            throw new PrimitiveWithoutResultException();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 186)
    protected abstract static class PrimEnterCriticalSectionNode extends AbstractPrimitiveWithPushNode implements BinaryPrimitive {
        @Child private LinkProcessToListNode linkProcessToListNode;
        @Child private WakeHighestPriorityNode wakeHighestPriorityNode;

        public PrimEnterCriticalSectionNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "ownerIsNil(mutex)")
        protected final boolean doEnterNilOwner(final PointersObject mutex, @SuppressWarnings("unused") final NotProvided notProvided) {
            mutex.atput0(MUTEX.OWNER, method.image.getActiveProcess());
            return method.image.sqFalse;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "activeProcessMutexOwner(mutex)")
        protected final boolean doEnterActiveProcessOwner(final PointersObject mutex, final NotProvided notProvided) {
            return method.image.sqTrue;
        }

        @Specialization(guards = {"!ownerIsNil(mutex)", "!activeProcessMutexOwner(mutex)"})
        protected final boolean doEnter(final VirtualFrame frame, final PointersObject mutex, @SuppressWarnings("unused") final NotProvided notProvided) {
            getPushNode().executeWrite(frame, method.image.sqFalse);
            getLinkProcessToListNode().executeLink(method.image.getActiveProcess(), mutex);
            getWakeHighestPriorityNode().executeWake(frame);
            throw new PrimitiveWithoutResultException();
        }

        @Specialization(guards = "ownerIsNil(mutex)")
        protected final boolean doEnterNilOwner(final PointersObject mutex, @SuppressWarnings("unused") final PointersObject effectiveProcess) {
            mutex.atput0(MUTEX.OWNER, effectiveProcess);
            return method.image.sqFalse;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "isMutexOwner(mutex, effectiveProcess)")
        protected final boolean doEnterActiveProcessOwner(final PointersObject mutex, final PointersObject effectiveProcess) {
            return method.image.sqTrue;
        }

        @Specialization(guards = {"!ownerIsNil(mutex)", "!isMutexOwner(mutex, effectiveProcess)"})
        protected final boolean doEnter(final VirtualFrame frame, final PointersObject mutex, @SuppressWarnings("unused") final PointersObject effectiveProcess) {
            getPushNode().executeWrite(frame, method.image.sqFalse);
            getLinkProcessToListNode().executeLink(effectiveProcess, mutex);
            getWakeHighestPriorityNode().executeWake(frame);
            throw new PrimitiveWithoutResultException();
        }

        protected final boolean ownerIsNil(final PointersObject mutex) {
            return mutex.at0(MUTEX.OWNER) == method.image.nil;
        }

        protected final boolean activeProcessMutexOwner(final PointersObject mutex) {
            return mutex.at0(MUTEX.OWNER) == method.image.getActiveProcess();
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

        private LinkProcessToListNode getLinkProcessToListNode() {
            if (linkProcessToListNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                linkProcessToListNode = insert(LinkProcessToListNode.create(method.image));
            }
            return linkProcessToListNode;
        }

        private WakeHighestPriorityNode getWakeHighestPriorityNode() {
            if (wakeHighestPriorityNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                wakeHighestPriorityNode = insert(WakeHighestPriorityNode.create(method));
            }
            return wakeHighestPriorityNode;
        }
    }

    @ImportStatic(MUTEX.class)
    @GenerateNodeFactory
    @SqueakPrimitive(indices = 187)
    protected abstract static class PrimTestAndSetOwnershipOfCriticalSectionNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        public PrimTestAndSetOwnershipOfCriticalSectionNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"ownerIsNil(rcvrMutex)"})
        protected final boolean doNilOwner(final PointersObject rcvrMutex) {
            rcvrMutex.atput0(MUTEX.OWNER, method.image.getActiveProcess());
            return method.image.sqFalse;
        }

        @Specialization(guards = {"ownerIsActiveProcess(rcvrMutex)"})
        protected final boolean doOwnerIsActiveProcess(@SuppressWarnings("unused") final PointersObject rcvrMutex) {
            return method.image.sqTrue;
        }

        @Specialization(guards = {"!ownerIsNil(rcvrMutex)", "!ownerIsActiveProcess(rcvrMutex)"})
        protected final Object doFallback(@SuppressWarnings("unused") final PointersObject rcvrMutex) {
            return method.image.nil;
        }

        protected final boolean ownerIsNil(final PointersObject mutex) {
            return mutex.at0(MUTEX.OWNER) == method.image.nil;
        }

        protected final boolean ownerIsActiveProcess(final PointersObject mutex) {
            return mutex.at0(MUTEX.OWNER) == method.image.getActiveProcess();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 188)
    protected abstract static class PrimExecuteMethodArgsArrayNode extends AbstractPerformPrimitiveNode implements TernaryPrimitive {
        @Child private DispatchNode dispatchNode = DispatchNode.create();
        @Child private ArrayObjectSizeNode sizeNode = ArrayObjectSizeNode.create();
        @Child private ArrayObjectReadNode readNode = ArrayObjectReadNode.create();

        protected PrimExecuteMethodArgsArrayNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final Object doExecute(final VirtualFrame frame, final Object receiver, final ArrayObject argArray, final CompiledMethodObject methodObject) {
            final int numArgs = sizeNode.execute(argArray);
            final Object[] dispatchRcvrAndArgs = new Object[1 + numArgs];
            dispatchRcvrAndArgs[0] = receiver;
            for (int i = 0; i < numArgs; i++) {
                dispatchRcvrAndArgs[1 + i] = readNode.execute(argArray, i);
            }
            final Object thisContext = getContextOrMarker(frame);
            return dispatchNode.executeDispatch(frame, methodObject, dispatchRcvrAndArgs, thisContext);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 218)
    protected abstract static class PrimDoNamedPrimitiveWithArgsNode extends AbstractPrimitiveNode implements QuaternaryPrimitive {
        @Child protected ArrayObjectToObjectArrayTransformNode getObjectArrayNode = ArrayObjectToObjectArrayTransformNode.create();

        public PrimDoNamedPrimitiveWithArgsNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final Object doNamedPrimitiveWithArgs(final VirtualFrame frame, @SuppressWarnings("unused") final ContextObject receiver, final CompiledMethodObject methodObject,
                        final Object target, final ArrayObject argumentArray) {
            /*
             * It is non-trivial to avoid the creation of a primitive node here. Deopt might be
             * acceptable because primitive is mostly used for debugging anyway.
             */
            final AbstractPrimitiveNode primitiveNode = method.image.primitiveNodeFactory.namedFor(methodObject);
            final Object[] receiverAndArguments = getObjectArrayNode.executeWithFirst(argumentArray, target);
            return replace(primitiveNode).executeWithArguments(frame, receiverAndArguments);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 230)
    protected abstract static class PrimRelinquishProcessorNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        public PrimRelinquishProcessorNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"!method.image.interruptHandlerDisabled()"})
        protected static final AbstractSqueakObject doRelinquish(final VirtualFrame frame, final AbstractSqueakObject receiver, @SuppressWarnings("unused") final long timeMicroseconds,
                        @Cached final StackPushForPrimitivesNode pushNode,
                        @Cached("create(method)") final InterruptHandlerNode interruptNode) {
            // Keep receiver on stack, interrupt handler could trigger.
            pushNode.executeWrite(frame, receiver);
            /*
             * Perform forced interrupt check, otherwise control flow cannot continue when
             * idleProcess is running.
             */
            interruptNode.executeTrigger(frame);
            throw new PrimitiveWithoutResultException();
        }

        @Specialization(guards = {"method.image.interruptHandlerDisabled()"})
        protected static final AbstractSqueakObject doNothing(final AbstractSqueakObject receiver, @SuppressWarnings("unused") final long timeMicroseconds) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 231)
    protected abstract static class PrimForceDisplayUpdateNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        protected PrimForceDisplayUpdateNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final AbstractSqueakObject doForceUpdate(final AbstractSqueakObject receiver) {
            return receiver; // Do nothing.
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 233)
    protected abstract static class PrimSetFullScreenNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimSetFullScreenNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "method.image.hasDisplay()")
        protected final AbstractSqueakObject doFullScreen(final AbstractSqueakObject receiver, final boolean enable) {
            method.image.getDisplay().setFullscreen(enable);
            return receiver;
        }

        @Specialization(guards = "!method.image.hasDisplay()")
        protected static final AbstractSqueakObject doFullScreenHeadless(final AbstractSqueakObject receiver, @SuppressWarnings("unused") final boolean enable) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 256)
    protected abstract static class PrimQuickReturnSelfNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        protected PrimQuickReturnSelfNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object returnValue(final Object receiver) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 257)
    protected abstract static class PrimQuickReturnTrueNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        protected PrimQuickReturnTrueNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final boolean returnValue(@SuppressWarnings("unused") final Object receiver) {
            return method.image.sqTrue;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 258)
    protected abstract static class PrimQuickReturnFalseNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        protected PrimQuickReturnFalseNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final boolean returnValue(@SuppressWarnings("unused") final Object receiver) {
            return method.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 259)
    protected abstract static class PrimQuickReturnNilNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        protected PrimQuickReturnNilNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final NilObject returnValue(@SuppressWarnings("unused") final Object receiver) {
            return method.image.nil;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 260)
    protected abstract static class PrimQuickReturnMinusOneNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        protected PrimQuickReturnMinusOneNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long returnValue(@SuppressWarnings("unused") final Object receiver) {
            return -1L;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 261)
    protected abstract static class PrimQuickReturnZeroNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        protected PrimQuickReturnZeroNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long returnValue(@SuppressWarnings("unused") final Object receiver) {
            return 0L;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 262)
    protected abstract static class PrimQuickReturnOneNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        protected PrimQuickReturnOneNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long returnValue(@SuppressWarnings("unused") final Object receiver) {
            return 1L;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 263)
    protected abstract static class PrimQuickReturnTwoNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        protected PrimQuickReturnTwoNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long returnValue(@SuppressWarnings("unused") final Object receiver) {
            return 2L;
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    @GenerateNodeFactory
    public abstract static class PrimQuickReturnReceiverVariableNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();
        private final long variableIndex;

        protected PrimQuickReturnReceiverVariableNode(final CompiledMethodObject method, final long variableIndex) {
            super(method);
            this.variableIndex = variableIndex;
        }

        @Specialization
        protected final Object doReceiverVariable(final Object receiver) {
            return at0Node.execute(receiver, variableIndex);
        }
    }
}
