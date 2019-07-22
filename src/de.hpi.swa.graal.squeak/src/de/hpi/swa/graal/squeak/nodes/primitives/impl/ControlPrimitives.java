package de.hpi.swa.graal.squeak.nodes.primitives.impl;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.logging.Level;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakQuit;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObjectWithClassAndHash;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BooleanObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.NotProvided;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.ERROR_TABLE;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.MUTEX;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.SEMAPHORE;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.DispatchEagerlyNode;
import de.hpi.swa.graal.squeak.nodes.DispatchSendNode;
import de.hpi.swa.graal.squeak.nodes.InheritsFromNode;
import de.hpi.swa.graal.squeak.nodes.LookupMethodNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectReadNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectSizeNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectToObjectArrayCopyNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectChangeClassOfToNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.SendBytecodes.AbstractSendNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.CreateEagerArgumentsNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitiveWithoutFallback;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.QuaternaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.QuinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.SeptenaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitiveWithoutFallback;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitiveWithoutFallback;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.StackPushForPrimitivesNode;
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
        @Child protected SqueakObjectClassNode classNode;
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
            if (classNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                classNode = insert(SqueakObjectClassNode.create());
            }
            return classNode.executeLookup(object);
        }

        private DispatchSendNode getDispatchSendNode() {
            if (dispatchSendNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                dispatchSendNode = insert(DispatchSendNode.create(method));
            }
            return dispatchSendNode;
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
        @Child private ArrayObjectToObjectArrayCopyNode getObjectArrayNode = ArrayObjectToObjectArrayCopyNode.create();

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
    protected abstract static class PrimSignalNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        @Child private SignalSemaphoreNode signalSemaphoreNode;

        protected PrimSignalNode(final CompiledMethodObject method) {
            super(method);
            signalSemaphoreNode = SignalSemaphoreNode.create(method);
        }

        @Specialization(guards = "receiver.getSqueakClass().isSemaphoreClass()")
        protected final Object doSignal(final VirtualFrame frame, final PointersObject receiver,
                        @Cached final StackPushForPrimitivesNode pushNode) {
            pushNode.executeWrite(frame, receiver); // keep receiver on stack
            signalSemaphoreNode.executeSignal(frame, receiver);
            return AbstractSendNode.NO_RESULT;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 86)
    protected abstract static class PrimWaitNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimWaitNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"receiver.getSqueakClass().isSemaphoreClass()", "hasExcessSignals(receiver)"})
        protected static final Object doWaitExcessSignals(final VirtualFrame frame, final PointersObject receiver,
                        @Shared("pushNode") @Cached final StackPushForPrimitivesNode pushNode) {
            pushNode.executeWrite(frame, receiver); // keep receiver on stack
            final long excessSignals = (long) receiver.at0(SEMAPHORE.EXCESS_SIGNALS);
            receiver.atput0(SEMAPHORE.EXCESS_SIGNALS, excessSignals - 1);
            return AbstractSendNode.NO_RESULT;
        }

        @Specialization(guards = {"receiver.getSqueakClass().isSemaphoreClass()", "!hasExcessSignals(receiver)"})
        protected final Object doWait(final VirtualFrame frame, final PointersObject receiver,
                        @Shared("pushNode") @Cached final StackPushForPrimitivesNode pushNode,
                        @Cached final LinkProcessToListNode linkProcessToListNode,
                        @Cached("create(method)") final WakeHighestPriorityNode wakeHighestPriorityNode) {
            pushNode.executeWrite(frame, receiver); // keep receiver on stack
            linkProcessToListNode.executeLink(method.image.getActiveProcess(), receiver);
            wakeHighestPriorityNode.executeWake(frame);
            return AbstractSendNode.NO_RESULT;
        }

        protected static final boolean hasExcessSignals(final PointersObject semaphore) {
            return (long) semaphore.at0(SEMAPHORE.EXCESS_SIGNALS) > 0;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 87)
    protected abstract static class PrimResumeNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        @Child private ResumeProcessNode resumeProcessNode;

        protected PrimResumeNode(final CompiledMethodObject method) {
            super(method);
            resumeProcessNode = ResumeProcessNode.create(method);
        }

        @Specialization
        protected final Object doResume(final VirtualFrame frame, final PointersObject receiver,
                        @Cached final StackPushForPrimitivesNode pushNode) {
            // keep receiver on stack before resuming other process
            pushNode.executeWrite(frame, receiver);
            resumeProcessNode.executeResume(frame, receiver);
            return AbstractSendNode.NO_RESULT;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 88)
    protected abstract static class PrimSuspendNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimSuspendNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "receiver.isActiveProcess()")
        protected static final Object doSuspendActiveProcess(final VirtualFrame frame, @SuppressWarnings("unused") final PointersObject receiver,
                        @Cached final StackPushForPrimitivesNode pushNode,
                        @Cached("create(method)") final WakeHighestPriorityNode wakeHighestPriorityNode) {
            pushNode.executeWrite(frame, NilObject.SINGLETON);
            wakeHighestPriorityNode.executeWake(frame);
            return AbstractSendNode.NO_RESULT; // result already pushed above
        }

        @Specialization(guards = {"!receiver.isActiveProcess()", "!hasNilList(receiver)"})
        protected static final PointersObject doSuspendOtherProcess(final PointersObject receiver,
                        @Cached final RemoveProcessFromListNode removeProcessNode) {
            final PointersObject oldList = (PointersObject) receiver.at0(PROCESS.LIST);
            removeProcessNode.executeRemove(receiver, oldList);
            receiver.atput0(PROCESS.LIST, NilObject.SINGLETON);
            return oldList;
        }

        @Specialization(guards = {"!receiver.isActiveProcess()", "hasNilList(receiver)"})
        protected static final Object doBadReceiver(@SuppressWarnings("unused") final PointersObject receiver) {
            throw new PrimitiveFailed(ERROR_TABLE.BAD_RECEIVER);
        }

        protected static final boolean hasNilList(final PointersObject process) {
            return process.at0(PROCESS.LIST) == NilObject.SINGLETON;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 89)
    protected abstract static class PrimFlushCacheNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        public PrimFlushCacheNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object doFlush(final Object receiver) {
            // TODO: actually flush caches once there are some
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 100)
    protected abstract static class PrimPerformWithArgumentsInSuperclassNode extends AbstractPerformPrimitiveNode implements QuinaryPrimitive {
        @Child private ArrayObjectToObjectArrayCopyNode getObjectArrayNode = ArrayObjectToObjectArrayCopyNode.create();
        @Child protected InheritsFromNode inheritsFromNode = InheritsFromNode.create();

        protected PrimPerformWithArgumentsInSuperclassNode(final CompiledMethodObject method) {
            super(method);
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
        protected final Object doPerform(final VirtualFrame frame, @SuppressWarnings("unused") final Object receiver, final Object target, final NativeObject selector,
                        final ArrayObject arguments, final ClassObject superClass) {
            return dispatch(frame, selector, superClass, getObjectArrayNode.executeWithFirst(arguments, target));
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!inheritsFromNode.execute(target, superClass)")
        protected static final Object doFail(final Object receiver, final Object target, final NativeObject selector, final ArrayObject arguments, final ClassObject superClass) {
            throw new PrimitiveFailed(ERROR_TABLE.BAD_RECEIVER);
        }
    }

    /** Complements {@link PrimNotIdenticalNode}. */
    @GenerateNodeFactory
    @SqueakPrimitive(indices = 110)
    protected abstract static class PrimIdenticalNode extends AbstractPrimitiveNode implements TernaryPrimitiveWithoutFallback {
        protected PrimIdenticalNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final boolean doBoolean(final boolean a, final boolean b, @SuppressWarnings("unused") final NotProvided notProvided) {
            return BooleanObject.wrap(a == b);
        }

        @Specialization
        protected static final boolean doChar(final char a, final char b, @SuppressWarnings("unused") final NotProvided notProvided) {
            return BooleanObject.wrap(a == b);
        }

        @Specialization
        protected static final boolean doLong(final long a, final long b, @SuppressWarnings("unused") final NotProvided notProvided) {
            return BooleanObject.wrap(a == b);
        }

        @Specialization
        protected static final boolean doDouble(final double a, final double b, @SuppressWarnings("unused") final NotProvided notProvided) {
            return BooleanObject.wrap(Double.doubleToRawLongBits(a) == Double.doubleToRawLongBits(b));
        }

        @Specialization
        protected static final boolean doObject(final Object a, final Object b, @SuppressWarnings("unused") final NotProvided notProvided) {
            return BooleanObject.wrap(a == b);
        }

        @Specialization
        protected static final boolean doBoolean(@SuppressWarnings("unused") final Object context, final boolean a, final boolean b) {
            return BooleanObject.wrap(a == b);
        }

        @Specialization
        protected static final boolean doChar(@SuppressWarnings("unused") final Object context, final char a, final char b) {
            return BooleanObject.wrap(a == b);
        }

        @Specialization
        protected static final boolean doLong(@SuppressWarnings("unused") final Object context, final long a, final long b) {
            return BooleanObject.wrap(a == b);
        }

        @Specialization
        protected static final boolean doDouble(@SuppressWarnings("unused") final Object context, final double a, final double b) {
            return BooleanObject.wrap(Double.doubleToRawLongBits(a) == Double.doubleToRawLongBits(b));
        }

        @Specialization(guards = "!isNotProvided(b)")
        public static final boolean doObject(@SuppressWarnings("unused") final Object context, final Object a, final Object b) {
            return BooleanObject.wrap(a == b);
        }
    }

    /*
     * primitiveClass (see Object>>class and Context>>objectClass:).
     */
    @NodeInfo(cost = NodeCost.NONE)
    @GenerateNodeFactory
    @SqueakPrimitive(indices = 111)
    protected abstract static class PrimClassNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimClassNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final ClassObject doClass(final Object receiver, @SuppressWarnings("unused") final NotProvided object,
                        @Shared("lookupNode") @Cached final SqueakObjectClassNode classNode) {
            return classNode.executeLookup(receiver);
        }

        @Specialization(guards = "!isNotProvided(object)")
        protected static final ClassObject doClass(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @Shared("lookupNode") @Cached final SqueakObjectClassNode classNode) {
            return classNode.executeLookup(object);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 112)
    protected abstract static class PrimBytesLeftNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        protected PrimBytesLeftNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long doBytesLeft(@SuppressWarnings("unused") final Object receiver) {
            return MiscUtils.runtimeFreeMemory();
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
    public abstract static class PrimExitToDebuggerNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        public static final String SELECTOR_NAME = "exitToDebugger";

        protected PrimExitToDebuggerNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object doDebugger(@SuppressWarnings("unused") final Object receiver) {
            return receiver;
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    @GenerateNodeFactory
    @SqueakPrimitive(indices = 115)
    protected abstract static class PrimChangeClassNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimChangeClassNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final AbstractSqueakObject doPrimChangeClass(final AbstractSqueakObjectWithClassAndHash receiver, final AbstractSqueakObjectWithClassAndHash argument,
                        @Cached final SqueakObjectChangeClassOfToNode changeClassOfToNode) {
            return changeClassOfToNode.execute(receiver, argument.getSqueakClass());
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 116)
    protected abstract static class PrimFlushCacheByMethodNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        public PrimFlushCacheByMethodNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "receiver.hasMethodClass()")
        protected static final CompiledMethodObject doFlush(final CompiledMethodObject receiver) {
            receiver.getMethodClass().invalidateMethodDictStableAssumption();
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
        @Child private ArrayObjectToObjectArrayCopyNode getObjectArrayNode = ArrayObjectToObjectArrayCopyNode.create();
        @Child private CreateEagerArgumentsNode createEagerArgumentsNode = CreateEagerArgumentsNode.create();

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
            } else {
                return primitiveNode.executeWithArguments(frame, createEagerArgumentsNode.executeCreate(primitiveNode.getNumArguments(), receiverAndArguments));
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 119)
    protected abstract static class PrimFlushCacheSelectiveNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        public PrimFlushCacheSelectiveNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object doFlush(final Object receiver) {
            // TODO: actually flush caches once there are some
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 130)
    protected abstract static class PrimFullGCNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        private static final TruffleLogger LOG = TruffleLogger.getLogger(SqueakLanguageConfig.ID, PrimFullGCNode.class);

        protected PrimFullGCNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final long doGC(@SuppressWarnings("unused") final Object receiver) {
            forceFullGC();
            if (hasPendingFinalizations()) {
                method.image.interrupt.setPendingFinalizations(true);
            }
            return MiscUtils.runtimeFreeMemory();
        }

        /**
         * {@link System#gc()} does not force a garbage collect, but it can be called until a new
         * object has been GC'ed (Source: https://git.io/fjED4).
         */
        @TruffleBoundary
        public static void forceFullGC() {
            Object obj = new Object();
            final WeakReference<?> ref = new WeakReference<>(obj);
            obj = null;
            while (ref.get() != null) {
                System.gc();
            }
        }

        @TruffleBoundary
        private boolean hasPendingFinalizations() {
            final ReferenceQueue<Object> queue = method.image.weakPointersQueue;
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
    protected abstract static class PrimIncrementalGCNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        protected PrimIncrementalGCNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long doIncrementalGC(@SuppressWarnings("unused") final Object receiver) {
            /* Cannot force incremental GC in Java, suggesting a normal GC instead. */
            MiscUtils.systemGC();
            return MiscUtils.runtimeFreeMemory();
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    @GenerateNodeFactory
    @SqueakPrimitive(indices = 160)
    protected abstract static class PrimAdoptInstanceNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimAdoptInstanceNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final ClassObject doPrimAdoptInstance(final ClassObject receiver, final AbstractSqueakObjectWithClassAndHash argument,
                        @Cached final SqueakObjectChangeClassOfToNode changeClassOfToNode) {
            changeClassOfToNode.execute(argument, receiver);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 167)
    protected abstract static class PrimYieldNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        @Child private YieldProcessNode yieldProcessNode;

        public PrimYieldNode(final CompiledMethodObject method) {
            super(method);
            yieldProcessNode = YieldProcessNode.create(method);
        }

        @Specialization
        protected final Object doYield(final VirtualFrame frame, final PointersObject scheduler,
                        @Cached final StackPushForPrimitivesNode pushNode) {
            pushNode.executeWrite(frame, scheduler); // keep receiver on stack
            yieldProcessNode.executeYield(frame, scheduler);
            return AbstractSendNode.NO_RESULT;
        }
    }

    /** Complements {@link PrimIdenticalNode}. */
    @GenerateNodeFactory
    @SqueakPrimitive(indices = 169)
    protected abstract static class PrimNotIdenticalNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {
        protected PrimNotIdenticalNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final boolean doBoolean(final boolean a, final boolean b) {
            return BooleanObject.wrap(a != b);
        }

        @Specialization
        protected static final boolean doChar(final char a, final char b) {
            return BooleanObject.wrap(a != b);
        }

        @Specialization
        protected static final boolean doLong(final long a, final long b) {
            return BooleanObject.wrap(a != b);
        }

        @Specialization
        protected static final boolean doDouble(final double a, final double b) {
            return BooleanObject.wrap(Double.doubleToRawLongBits(a) != Double.doubleToRawLongBits(b));
        }

        @Fallback
        public static final boolean doObject(final Object a, final Object b) {
            return BooleanObject.wrap(a != b);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 185)
    protected abstract static class PrimExitCriticalSectionNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        public PrimExitCriticalSectionNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "mutex.isEmptyList()")
        protected static final PointersObject doExitEmpty(final PointersObject mutex) {
            mutex.atputNil0(MUTEX.OWNER);
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
            return AbstractSendNode.NO_RESULT;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 186)
    protected abstract static class PrimEnterCriticalSectionNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        public PrimEnterCriticalSectionNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "ownerIsNil(mutex)")
        protected final boolean doEnterNilOwner(final PointersObject mutex, @SuppressWarnings("unused") final NotProvided notProvided) {
            mutex.atput0(MUTEX.OWNER, method.image.getActiveProcess());
            return BooleanObject.FALSE;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "activeProcessMutexOwner(mutex)")
        protected static final boolean doEnterActiveProcessOwner(final PointersObject mutex, final NotProvided notProvided) {
            return BooleanObject.TRUE;
        }

        @Specialization(guards = {"!ownerIsNil(mutex)", "!activeProcessMutexOwner(mutex)"})
        protected final Object doEnter(final VirtualFrame frame, final PointersObject mutex, @SuppressWarnings("unused") final NotProvided notProvided,
                        @Shared("pushNode") @Cached final StackPushForPrimitivesNode pushNode,
                        @Shared("linkProcessToListNode") @Cached final LinkProcessToListNode linkProcessToListNode,
                        @Shared("wakeHighestPriorityNode") @Cached("create(method)") final WakeHighestPriorityNode wakeHighestPriorityNode) {
            pushNode.executeWrite(frame, BooleanObject.FALSE);
            linkProcessToListNode.executeLink(method.image.getActiveProcess(), mutex);
            wakeHighestPriorityNode.executeWake(frame);
            return AbstractSendNode.NO_RESULT;
        }

        @Specialization(guards = "ownerIsNil(mutex)")
        protected static final boolean doEnterNilOwner(final PointersObject mutex, @SuppressWarnings("unused") final PointersObject effectiveProcess) {
            mutex.atput0(MUTEX.OWNER, effectiveProcess);
            return BooleanObject.FALSE;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "isMutexOwner(mutex, effectiveProcess)")
        protected static final boolean doEnterActiveProcessOwner(final PointersObject mutex, final PointersObject effectiveProcess) {
            return BooleanObject.TRUE;
        }

        @Specialization(guards = {"!ownerIsNil(mutex)", "!isMutexOwner(mutex, effectiveProcess)"})
        protected static final Object doEnter(final VirtualFrame frame, final PointersObject mutex, @SuppressWarnings("unused") final PointersObject effectiveProcess,
                        @Shared("pushNode") @Cached final StackPushForPrimitivesNode pushNode,
                        @Shared("linkProcessToListNode") @Cached final LinkProcessToListNode linkProcessToListNode,
                        @Shared("wakeHighestPriorityNode") @Cached("create(method)") final WakeHighestPriorityNode wakeHighestPriorityNode) {
            pushNode.executeWrite(frame, BooleanObject.FALSE);
            linkProcessToListNode.executeLink(effectiveProcess, mutex);
            wakeHighestPriorityNode.executeWake(frame);
            return AbstractSendNode.NO_RESULT;
        }

        protected static final boolean ownerIsNil(final PointersObject mutex) {
            return mutex.at0(MUTEX.OWNER) == NilObject.SINGLETON;
        }

        protected final boolean activeProcessMutexOwner(final PointersObject mutex) {
            return mutex.at0(MUTEX.OWNER) == method.image.getActiveProcess();
        }

        protected static final boolean isMutexOwner(final PointersObject mutex, final PointersObject effectiveProcess) {
            return mutex.at0(MUTEX.OWNER) == effectiveProcess;
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
            return BooleanObject.FALSE;
        }

        @Specialization(guards = {"ownerIsActiveProcess(rcvrMutex)"})
        protected static final boolean doOwnerIsActiveProcess(@SuppressWarnings("unused") final PointersObject rcvrMutex) {
            return BooleanObject.TRUE;
        }

        @Specialization(guards = {"!ownerIsNil(rcvrMutex)", "!ownerIsActiveProcess(rcvrMutex)"})
        protected static final Object doFallback(@SuppressWarnings("unused") final PointersObject rcvrMutex) {
            return NilObject.SINGLETON;
        }

        protected static final boolean ownerIsNil(final PointersObject mutex) {
            return mutex.at0(MUTEX.OWNER) == NilObject.SINGLETON;
        }

        protected final boolean ownerIsActiveProcess(final PointersObject mutex) {
            return mutex.at0(MUTEX.OWNER) == method.image.getActiveProcess();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 188)
    protected abstract static class PrimExecuteMethodArgsArrayNode extends AbstractPerformPrimitiveNode implements QuaternaryPrimitive {
        @Child private DispatchEagerlyNode dispatchNode = DispatchEagerlyNode.create();
        @Child private ArrayObjectSizeNode sizeNode = ArrayObjectSizeNode.create();
        @Child private ArrayObjectReadNode readNode = ArrayObjectReadNode.create();

        protected PrimExecuteMethodArgsArrayNode(final CompiledMethodObject method) {
            super(method);
        }

        /** Deprecated since Kernel-eem.1204. Kept for backward compatibility. */
        @Specialization
        protected final Object doExecute(final VirtualFrame frame, final Object receiver, final ArrayObject argArray, final CompiledMethodObject methodObject,
                        @SuppressWarnings("unused") final NotProvided notProvided) {
            return doExecute(frame, null, receiver, argArray, methodObject);
        }

        @Specialization
        protected final Object doExecute(final VirtualFrame frame, @SuppressWarnings("unused") final ClassObject compiledMethodClass, final Object receiver, final ArrayObject argArray,
                        final CompiledMethodObject methodObject) {
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
        @Child private ArrayObjectToObjectArrayCopyNode getObjectArrayNode = ArrayObjectToObjectArrayCopyNode.create();

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

        @Specialization
        protected static final Object doRelinquish(final VirtualFrame frame, final Object receiver, @SuppressWarnings("unused") final long timeMicroseconds,
                        @Cached final StackPushForPrimitivesNode pushNode,
                        @Cached("create(method)") final InterruptHandlerNode interruptNode) {
            /* Keep receiver on stack, interrupt handler could trigger. */
            pushNode.executeWrite(frame, receiver);
            /*
             * Perform interrupt check (even if interrupt handler is not active), otherwise
             * idleProcess gets stuck. Checking whether the interrupt handler `shouldTrigger()`
             * decreases performance for some reason, forcing interrupt check instead.
             */
            interruptNode.executeTrigger(frame);
            return AbstractSendNode.NO_RESULT;
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 231)
    protected abstract static class PrimForceDisplayUpdateNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        protected PrimForceDisplayUpdateNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object doForceUpdate(final Object receiver) {
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
        protected final Object doFullScreen(final Object receiver, final boolean enable) {
            method.image.getDisplay().setFullscreen(enable);
            return receiver;
        }

        @Specialization(guards = "!method.image.hasDisplay()")
        protected static final Object doFullScreenHeadless(final Object receiver, @SuppressWarnings("unused") final boolean enable) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
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
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 257)
    protected abstract static class PrimQuickReturnTrueNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        protected PrimQuickReturnTrueNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final boolean returnValue(@SuppressWarnings("unused") final Object receiver) {
            return BooleanObject.TRUE;
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 258)
    protected abstract static class PrimQuickReturnFalseNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        protected PrimQuickReturnFalseNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final boolean returnValue(@SuppressWarnings("unused") final Object receiver) {
            return BooleanObject.FALSE;
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 259)
    protected abstract static class PrimQuickReturnNilNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        protected PrimQuickReturnNilNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final NilObject returnValue(@SuppressWarnings("unused") final Object receiver) {
            return NilObject.SINGLETON;
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
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
    @NodeInfo(cost = NodeCost.NONE)
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
    @NodeInfo(cost = NodeCost.NONE)
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
    @NodeInfo(cost = NodeCost.NONE)
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

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
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
