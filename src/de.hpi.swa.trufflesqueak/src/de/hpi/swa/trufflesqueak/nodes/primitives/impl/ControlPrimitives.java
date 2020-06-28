/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.lang.management.ManagementFactory;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleOptions;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakQuit;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObjectWithClassAndHash;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.MUTEX;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.SEMAPHORE;
import de.hpi.swa.trufflesqueak.nodes.DispatchEagerlyNode;
import de.hpi.swa.trufflesqueak.nodes.DispatchSendNode;
import de.hpi.swa.trufflesqueak.nodes.DispatchSendNode.DispatchSendSelectorNode;
import de.hpi.swa.trufflesqueak.nodes.InheritsFromNode;
import de.hpi.swa.trufflesqueak.nodes.LookupMethodNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectCopyIntoObjectArrayNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectSizeNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectToObjectArrayWithFirstNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectChangeClassOfToNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectIdentityNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackPointerIncrementNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackPushNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitiveWithoutFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.QuaternaryPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.QuinaryPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.SeptenaryPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitiveWithoutFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitiveWithoutFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.nodes.process.GetActiveProcessNode;
import de.hpi.swa.trufflesqueak.nodes.process.LinkProcessToListNode;
import de.hpi.swa.trufflesqueak.nodes.process.RemoveProcessFromListNode;
import de.hpi.swa.trufflesqueak.nodes.process.ResumeProcessNode;
import de.hpi.swa.trufflesqueak.nodes.process.SignalSemaphoreNode;
import de.hpi.swa.trufflesqueak.nodes.process.WakeHighestPriorityNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.InterruptHandlerNode;
import de.hpi.swa.trufflesqueak.util.LogUtils;
import de.hpi.swa.trufflesqueak.util.MiscUtils;
import de.hpi.swa.trufflesqueak.util.NotProvided;

public final class ControlPrimitives extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return ControlPrimitivesFactory.getFactories();
    }

    protected abstract static class AbstractPrimitiveStackIncrementNode extends AbstractPrimitiveNode {
        @Child protected FrameStackPointerIncrementNode frameStackPointerIncrementNode;

        protected final FrameStackPointerIncrementNode getFrameStackPointerIncrementNode() {
            if (frameStackPointerIncrementNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                frameStackPointerIncrementNode = insert(FrameStackPointerIncrementNode.create());
            }
            return frameStackPointerIncrementNode;
        }
    }

    protected abstract static class AbstractPrimitiveStackPushNode extends AbstractPrimitiveNode {
        @Child protected FrameStackPushNode frameStackPushNode;

        protected final FrameStackPushNode getFrameStackPushNode() {
            if (frameStackPushNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                frameStackPushNode = insert(FrameStackPushNode.create());
            }
            return frameStackPushNode;
        }
    }

    /* primitiveFail (#19) handled specially. */

    // primitiveBlockCopy / primitiveBlockValue: (#80, #81, #82) no longer needed.

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 83)
    protected abstract static class PrimPerformNode extends AbstractPrimitiveNode implements SeptenaryPrimitive {
        protected static final int CACHE_LIMIT = 2;

        @SuppressWarnings("unused")
        @Specialization(guards = "selector == cachedSelector", limit = "CACHE_LIMIT")
        protected static final Object perform0Cached(final VirtualFrame frame, final Object receiver, final NativeObject selector, final NotProvided object1, final NotProvided object2,
                        final NotProvided object3, final NotProvided object4, final NotProvided object5,
                        @Cached("selector") final NativeObject cachedSelector,
                        @Cached final SqueakObjectClassNode lookupClassNode,
                        @Cached final LookupMethodNode lookupMethodNode,
                        @Cached("create(cachedSelector, getBlockOrMethod(frame))") final DispatchSendNode dispatchNode) {
            return dispatchCached(frame, cachedSelector, new Object[]{receiver}, lookupClassNode, lookupMethodNode, dispatchNode);
        }

        @SuppressWarnings("unused")
        @Specialization(replaces = "perform0Cached")
        protected static final Object perform0(final VirtualFrame frame, final Object receiver, final NativeObject selector, final NotProvided object1, final NotProvided object2,
                        final NotProvided object3, final NotProvided object4, final NotProvided object5,
                        @Cached final SqueakObjectClassNode lookupClassNode,
                        @Cached final LookupMethodNode lookupMethodNode,
                        @Cached final DispatchSendSelectorNode dispatchNode) {
            return dispatchUncached(frame, selector, new Object[]{receiver}, lookupClassNode, lookupMethodNode, dispatchNode);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"selector == cachedSelector", "!isNotProvided(object1)"}, limit = "CACHE_LIMIT")
        protected static final Object perform1Cached(final VirtualFrame frame, final Object receiver, final NativeObject selector, final Object object1, final NotProvided object2,
                        final NotProvided object3, final NotProvided object4, final NotProvided object5,
                        @Cached("selector") final NativeObject cachedSelector,
                        @Cached final SqueakObjectClassNode lookupClassNode,
                        @Cached final LookupMethodNode lookupMethodNode,
                        @Cached("create(cachedSelector, getBlockOrMethod(frame))") final DispatchSendNode dispatchNode) {
            return dispatchCached(frame, cachedSelector, new Object[]{receiver, object1}, lookupClassNode, lookupMethodNode, dispatchNode);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isNotProvided(object1)"}, replaces = "perform1Cached")
        protected static final Object perform1(final VirtualFrame frame, final Object receiver, final NativeObject selector, final Object object1, final NotProvided object2,
                        final NotProvided object3, final NotProvided object4, final NotProvided object5,
                        @Cached final SqueakObjectClassNode lookupClassNode,
                        @Cached final LookupMethodNode lookupMethodNode,
                        @Cached final DispatchSendSelectorNode dispatchNode) {
            return dispatchUncached(frame, selector, new Object[]{receiver, object1}, lookupClassNode, lookupMethodNode, dispatchNode);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"selector == cachedSelector", "!isNotProvided(object1)", "!isNotProvided(object2)"}, limit = "CACHE_LIMIT")
        protected static final Object perform2Cached(final VirtualFrame frame, final Object receiver, final NativeObject selector, final Object object1, final Object object2,
                        final NotProvided object3, final NotProvided object4, final NotProvided object5,
                        @Cached("selector") final NativeObject cachedSelector,
                        @Cached final SqueakObjectClassNode lookupClassNode,
                        @Cached final LookupMethodNode lookupMethodNode,
                        @Cached("create(cachedSelector, getBlockOrMethod(frame))") final DispatchSendNode dispatchNode) {
            return dispatchCached(frame, cachedSelector, new Object[]{receiver, object1, object2}, lookupClassNode, lookupMethodNode, dispatchNode);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isNotProvided(object1)", "!isNotProvided(object2)"}, replaces = "perform2Cached")
        protected static final Object perform2(final VirtualFrame frame, final Object receiver, final NativeObject selector, final Object object1, final Object object2,
                        final NotProvided object3, final NotProvided object4, final NotProvided object5,
                        @Cached final SqueakObjectClassNode lookupClassNode,
                        @Cached final LookupMethodNode lookupMethodNode,
                        @Cached final DispatchSendSelectorNode dispatchNode) {
            return dispatchUncached(frame, selector, new Object[]{receiver, object1, object2}, lookupClassNode, lookupMethodNode, dispatchNode);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"selector == cachedSelector", "!isNotProvided(object1)", "!isNotProvided(object2)", "!isNotProvided(object3)"}, limit = "CACHE_LIMIT")
        protected static final Object perform3Cached(final VirtualFrame frame, final Object receiver, final NativeObject selector, final Object object1, final Object object2,
                        final Object object3, final NotProvided object4, final NotProvided object5,
                        @Cached("selector") final NativeObject cachedSelector,
                        @Cached final SqueakObjectClassNode lookupClassNode,
                        @Cached final LookupMethodNode lookupMethodNode,
                        @Cached("create(cachedSelector, getBlockOrMethod(frame))") final DispatchSendNode dispatchNode) {
            return dispatchCached(frame, cachedSelector, new Object[]{receiver, object1, object2, object3}, lookupClassNode, lookupMethodNode, dispatchNode);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isNotProvided(object1)", "!isNotProvided(object2)", "!isNotProvided(object3)"}, replaces = "perform3Cached")
        protected static final Object perform3(final VirtualFrame frame, final Object receiver, final NativeObject selector, final Object object1, final Object object2,
                        final Object object3, final NotProvided object4, final NotProvided object5,
                        @Cached final SqueakObjectClassNode lookupClassNode,
                        @Cached final LookupMethodNode lookupMethodNode,
                        @Cached final DispatchSendSelectorNode dispatchNode) {
            return dispatchUncached(frame, selector, new Object[]{receiver, object1, object2, object3}, lookupClassNode, lookupMethodNode, dispatchNode);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"selector == cachedSelector", "!isNotProvided(object1)", "!isNotProvided(object2)", "!isNotProvided(object3)", "!isNotProvided(object4)"}, limit = "CACHE_LIMIT")
        protected static final Object perform4Cached(final VirtualFrame frame, final Object receiver, final NativeObject selector, final Object object1, final Object object2,
                        final Object object3, final Object object4, final NotProvided object5,
                        @Cached("selector") final NativeObject cachedSelector,
                        @Cached final SqueakObjectClassNode lookupClassNode,
                        @Cached final LookupMethodNode lookupMethodNode,
                        @Cached("create(cachedSelector, getBlockOrMethod(frame))") final DispatchSendNode dispatchNode) {
            return dispatchCached(frame, cachedSelector, new Object[]{receiver, object1, object2, object3, object4}, lookupClassNode, lookupMethodNode, dispatchNode);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isNotProvided(object1)", "!isNotProvided(object2)", "!isNotProvided(object3)", "!isNotProvided(object4)"}, replaces = "perform4Cached")
        protected static final Object perform4(final VirtualFrame frame, final Object receiver, final NativeObject selector, final Object object1, final Object object2,
                        final Object object3, final Object object4, final NotProvided object5,
                        @Cached final SqueakObjectClassNode lookupClassNode,
                        @Cached final LookupMethodNode lookupMethodNode,
                        @Cached final DispatchSendSelectorNode dispatchNode) {
            return dispatchUncached(frame, selector, new Object[]{receiver, object1, object2, object3, object4}, lookupClassNode, lookupMethodNode, dispatchNode);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"selector == cachedSelector", "!isNotProvided(object1)", "!isNotProvided(object2)", "!isNotProvided(object3)", "!isNotProvided(object4)",
                        "!isNotProvided(object5)"}, limit = "CACHE_LIMIT")
        protected static final Object perform5Cached(final VirtualFrame frame, final Object receiver, final NativeObject selector, final Object object1, final Object object2,
                        final Object object3, final Object object4, final Object object5,
                        @Cached("selector") final NativeObject cachedSelector,
                        @Cached final SqueakObjectClassNode lookupClassNode,
                        @Cached final LookupMethodNode lookupMethodNode,
                        @Cached("create(cachedSelector, getBlockOrMethod(frame))") final DispatchSendNode dispatchNode) {
            return dispatchCached(frame, cachedSelector, new Object[]{receiver, object1, object2, object3, object4, object5}, lookupClassNode, lookupMethodNode, dispatchNode);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isNotProvided(object1)", "!isNotProvided(object2)", "!isNotProvided(object3)", "!isNotProvided(object4)", "!isNotProvided(object5)"}, replaces = "perform5Cached")
        protected static final Object perform5(final VirtualFrame frame, final Object receiver, final NativeObject selector, final Object object1, final Object object2,
                        final Object object3, final Object object4, final Object object5,
                        @Cached final SqueakObjectClassNode lookupClassNode,
                        @Cached final LookupMethodNode lookupMethodNode,
                        @Cached final DispatchSendSelectorNode dispatchNode) {
            return dispatchUncached(frame, selector, new Object[]{receiver, object1, object2, object3, object4, object5}, lookupClassNode, lookupMethodNode, dispatchNode);
        }

        private static Object dispatchCached(final VirtualFrame frame, final NativeObject selector, final Object[] receiverAndArguments, final SqueakObjectClassNode lookupClassNode,
                        final LookupMethodNode lookupMethodNode, final DispatchSendNode dispatchNode) {
            final ClassObject rcvrClass = lookupClassNode.executeLookup(receiverAndArguments[0]);
            final Object lookupResult = lookupMethodNode.executeLookup(rcvrClass, selector);
            return dispatchNode.executeSend(frame, selector, lookupResult, rcvrClass, receiverAndArguments);
        }

        private static Object dispatchUncached(final VirtualFrame frame, final NativeObject selector, final Object[] receiverAndArguments, final SqueakObjectClassNode lookupClassNode,
                        final LookupMethodNode lookupMethodNode, final DispatchSendSelectorNode dispatchNode) {
            final ClassObject rcvrClass = lookupClassNode.executeLookup(receiverAndArguments[0]);
            final Object lookupResult = lookupMethodNode.executeLookup(rcvrClass, selector);
            return dispatchNode.executeSend(frame, selector, lookupResult, rcvrClass, receiverAndArguments);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 84)
    protected abstract static class PrimPerformWithArgumentsNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        @Child private ArrayObjectToObjectArrayWithFirstNode getObjectArrayNode = ArrayObjectToObjectArrayWithFirstNode.create();

        @Specialization(guards = "selector == cachedSelector", limit = "2")
        protected final Object performCached(final VirtualFrame frame, final Object receiver, @SuppressWarnings("unused") final NativeObject selector, final ArrayObject arguments,
                        @Cached("selector") final NativeObject cachedSelector,
                        @Cached final SqueakObjectClassNode lookupClassNode,
                        @Cached final LookupMethodNode lookupMethodNode,
                        @Cached("create(cachedSelector, getBlockOrMethod(frame))") final DispatchSendNode dispatchNode) {
            final ClassObject rcvrClass = lookupClassNode.executeLookup(receiver);
            final Object lookupResult = lookupMethodNode.executeLookup(rcvrClass, cachedSelector);
            return dispatchNode.executeSend(frame, cachedSelector, lookupResult, rcvrClass, getObjectArrayNode.execute(receiver, arguments));
        }

        @Specialization(replaces = "performCached")
        protected final Object perform(final VirtualFrame frame, final Object receiver, final NativeObject selector, final ArrayObject arguments,
                        @Cached final SqueakObjectClassNode lookupClassNode,
                        @Cached final LookupMethodNode lookupMethodNode,
                        @Cached final DispatchSendSelectorNode dispatchNode) {
            final ClassObject rcvrClass = lookupClassNode.executeLookup(receiver);
            final Object lookupResult = lookupMethodNode.executeLookup(rcvrClass, selector);
            return dispatchNode.executeSend(frame, selector, lookupResult, rcvrClass, getObjectArrayNode.execute(receiver, arguments));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 85)
    protected abstract static class PrimSignalNode extends AbstractPrimitiveStackIncrementNode implements UnaryPrimitive {
        @Specialization(guards = "receiver.getSqueakClass().isSemaphoreClass()")
        protected final Object doSignal(final VirtualFrame frame, final PointersObject receiver,
                        @Cached final SignalSemaphoreNode signalSemaphoreNode) {
            try {
                signalSemaphoreNode.executeSignal(frame, receiver);
            } catch (final ProcessSwitch ps) {
                /*
                 * Leave receiver on stack. It has not been removed from the stack yet, so it is
                 * enough to increment the stack pointer.
                 */
                getFrameStackPointerIncrementNode().execute(frame);
                throw ps;
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 86)
    protected abstract static class PrimWaitNode extends AbstractPrimitiveStackIncrementNode implements UnaryPrimitive {
        @Child private AbstractPointersObjectReadNode pointersReadNode = AbstractPointersObjectReadNode.create();

        @Specialization(guards = {"receiver.getSqueakClass().isSemaphoreClass()", "hasExcessSignals(receiver)"})
        protected final Object doWaitExcessSignals(final PointersObject receiver,
                        @Cached final AbstractPointersObjectWriteNode writeNode) {
            final long excessSignals = pointersReadNode.executeLong(receiver, SEMAPHORE.EXCESS_SIGNALS);
            writeNode.execute(receiver, SEMAPHORE.EXCESS_SIGNALS, excessSignals - 1);
            return receiver;
        }

        @Specialization(guards = {"receiver.getSqueakClass().isSemaphoreClass()", "!hasExcessSignals(receiver)"})
        protected final Object doWait(final VirtualFrame frame, final PointersObject receiver,
                        @Cached final LinkProcessToListNode linkProcessToListNode,
                        @Cached final WakeHighestPriorityNode wakeHighestPriorityNode,
                        @Cached final GetActiveProcessNode getActiveProcessNode) {
            linkProcessToListNode.executeLink(getActiveProcessNode.execute(), receiver);
            try {
                wakeHighestPriorityNode.executeWake(frame);
            } catch (final ProcessSwitch ps) {
                /*
                 * Leave receiver on stack. It has not been removed from the stack yet, so it is
                 * enough to increment the stack pointer.
                 */
                getFrameStackPointerIncrementNode().execute(frame);
                throw ps;
            }
            return receiver;
        }

        protected final boolean hasExcessSignals(final PointersObject semaphore) {
            return pointersReadNode.executeLong(semaphore, SEMAPHORE.EXCESS_SIGNALS) > 0;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 87)
    protected abstract static class PrimResumeNode extends AbstractPrimitiveStackIncrementNode implements UnaryPrimitive {

        @Specialization
        protected final Object doResume(final VirtualFrame frame, final PointersObject receiver,
                        @Cached final AbstractPointersObjectReadNode readNode,
                        @Cached final ResumeProcessNode resumeProcessNode) {
            if (!(readNode.execute(receiver, PROCESS.SUSPENDED_CONTEXT) instanceof ContextObject)) {
                CompilerDirectives.transferToInterpreter();
                throw PrimitiveFailed.GENERIC_ERROR;
            }
            try {
                resumeProcessNode.executeResume(frame, receiver);
            } catch (final ProcessSwitch ps) {
                /*
                 * Leave receiver on stack. It has not been removed from the stack yet, so it is
                 * enough to increment the stack pointer.
                 */
                getFrameStackPointerIncrementNode().execute(frame);
                throw ps;
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 88)
    protected abstract static class PrimSuspendNode extends AbstractPrimitiveStackPushNode implements UnaryPrimitive {

        @Specialization(guards = "receiver == getActiveProcessNode.execute()", limit = "1")
        protected final Object doSuspendActiveProcess(final VirtualFrame frame, @SuppressWarnings("unused") final PointersObject receiver,
                        @SuppressWarnings("unused") @Shared("getActiveProcessNode") @Cached final GetActiveProcessNode getActiveProcessNode,
                        @Cached final WakeHighestPriorityNode wakeHighestPriorityNode) {
            try {
                wakeHighestPriorityNode.executeWake(frame);
            } catch (final ProcessSwitch ps) {
                /* Leave `nil` as result on stack. */
                getFrameStackPushNode().execute(frame, NilObject.SINGLETON);
                throw ps;
            }
            return NilObject.SINGLETON;
        }

        @Specialization(guards = {"receiver != getActiveProcessNode.execute()"}, limit = "1")
        protected static final PointersObject doSuspendOtherProcess(final PointersObject receiver,
                        @SuppressWarnings("unused") @Shared("getActiveProcessNode") @Cached final GetActiveProcessNode getActiveProcessNode,
                        @Cached final RemoveProcessFromListNode removeProcessNode,
                        @Cached final AbstractPointersObjectReadNode readNode,
                        @Cached final AbstractPointersObjectWriteNode writeNode) {
            if (readNode.execute(receiver, PROCESS.LIST) == NilObject.SINGLETON) {
                CompilerDirectives.transferToInterpreter();
                throw PrimitiveFailed.BAD_RECEIVER;
            } else {
                final PointersObject oldList = readNode.executePointers(receiver, PROCESS.LIST);
                removeProcessNode.executeRemove(receiver, oldList);
                writeNode.execute(receiver, PROCESS.LIST, NilObject.SINGLETON);
                return oldList;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 89)
    protected abstract static class PrimFlushCacheNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        @Specialization
        protected static final Object doFlush(final Object receiver,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            image.flushMethodCache();
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 100)
    protected abstract static class PrimPerformWithArgumentsInSuperclassNode extends AbstractPrimitiveNode implements QuinaryPrimitive {
        @Child private ArrayObjectToObjectArrayWithFirstNode getObjectArrayNode = ArrayObjectToObjectArrayWithFirstNode.create();
        @Child protected InheritsFromNode inheritsFromNode = InheritsFromNode.create();

        /*
         * Object>>#perform:withArguments:inSuperclass:
         */

        @Specialization(guards = "selector == cachedSelector", limit = "2")
        protected final Object performCached(final VirtualFrame frame, final Object receiver, @SuppressWarnings("unused") final NativeObject selector, final ArrayObject arguments,
                        final ClassObject superClass, @SuppressWarnings("unused") final NotProvided np,
                        @Cached("selector") final NativeObject cachedSelector,
                        @Cached final LookupMethodNode lookupMethodNode,
                        @Cached("create(cachedSelector, getBlockOrMethod(frame))") final DispatchSendNode dispatchNode) {
            if (inheritsFromNode.execute(receiver, superClass)) {
                final Object lookupResult = lookupMethodNode.executeLookup(superClass, cachedSelector);
                return dispatchNode.executeSend(frame, cachedSelector, lookupResult, superClass, getObjectArrayNode.execute(receiver, arguments));
            } else {
                CompilerDirectives.transferToInterpreter();
                throw PrimitiveFailed.BAD_RECEIVER;
            }
        }

        @Specialization(replaces = "performCached")
        protected final Object perform(final VirtualFrame frame, final Object receiver, final NativeObject selector, final ArrayObject arguments, final ClassObject superClass,
                        @SuppressWarnings("unused") final NotProvided np,
                        @Cached final LookupMethodNode lookupMethodNode,
                        @Cached final DispatchSendSelectorNode dispatchNode) {
            if (inheritsFromNode.execute(receiver, superClass)) {
                final Object lookupResult = lookupMethodNode.executeLookup(superClass, selector);
                return dispatchNode.executeSend(frame, selector, lookupResult, superClass, getObjectArrayNode.execute(receiver, arguments));
            } else {
                CompilerDirectives.transferToInterpreter();
                throw PrimitiveFailed.BAD_RECEIVER;
            }
        }

        /*
         * Context>>#object:perform:withArguments:inClass:
         */

        @Specialization(guards = "selector == cachedSelector", limit = "2")
        protected final Object performContextCached(final VirtualFrame frame, @SuppressWarnings("unused") final Object receiver, final Object target,
                        @SuppressWarnings("unused") final NativeObject selector, final ArrayObject arguments, final ClassObject superClass,
                        @Cached("selector") final NativeObject cachedSelector,
                        @Cached final LookupMethodNode lookupMethodNode,
                        @Cached("create(cachedSelector, getBlockOrMethod(frame))") final DispatchSendNode dispatchNode) {
            if (inheritsFromNode.execute(target, superClass)) {
                final Object lookupResult = lookupMethodNode.executeLookup(superClass, cachedSelector);
                return dispatchNode.executeSend(frame, cachedSelector, lookupResult, superClass, getObjectArrayNode.execute(target, arguments));
            } else {
                CompilerDirectives.transferToInterpreter();
                throw PrimitiveFailed.BAD_RECEIVER;
            }
        }

        @Specialization(replaces = "performContextCached")
        protected final Object performContext(final VirtualFrame frame, @SuppressWarnings("unused") final Object receiver, final Object target, final NativeObject selector,
                        final ArrayObject arguments, final ClassObject superClass,
                        @Cached final LookupMethodNode lookupMethodNode,
                        @Cached final DispatchSendSelectorNode dispatchNode) {
            if (inheritsFromNode.execute(target, superClass)) {
                final Object lookupResult = lookupMethodNode.executeLookup(superClass, selector);
                return dispatchNode.executeSend(frame, selector, lookupResult, superClass, getObjectArrayNode.execute(target, arguments));
            } else {
                CompilerDirectives.transferToInterpreter();
                throw PrimitiveFailed.BAD_RECEIVER;
            }
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 110)
    protected abstract static class PrimIdenticalNode extends AbstractPrimitiveNode implements TernaryPrimitiveWithoutFallback {
        @Specialization
        protected static final boolean doObject(final Object a, final Object b, @SuppressWarnings("unused") final NotProvided notProvided,
                        @Shared("identityNode") @Cached final SqueakObjectIdentityNode identityNode) {
            return identityNode.execute(a, b);
        }

        @Specialization(guards = "!isNotProvided(b)")
        public static final boolean doObject(@SuppressWarnings("unused") final Object context, final Object a, final Object b,
                        @Shared("identityNode") @Cached final SqueakObjectIdentityNode identityNode) {
            return identityNode.execute(a, b);
        }
    }

    /*
     * primitiveClass (see Object>>class and Context>>objectClass:).
     */
    @NodeInfo(cost = NodeCost.NONE)
    @GenerateNodeFactory
    @SqueakPrimitive(indices = 111)
    protected abstract static class PrimClassNode extends AbstractPrimitiveNode implements BinaryPrimitive {
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

        @Specialization
        protected static final long doBytesLeft(@SuppressWarnings("unused") final Object receiver) {
            return MiscUtils.runtimeFreeMemory();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 113)
    protected abstract static class PrimQuitNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        @SuppressWarnings("unused")
        @Specialization
        protected static final Object doQuit(final Object receiver, final NotProvided exitStatus) {
            throw new SqueakQuit(0);
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

        @Specialization
        protected static final Object doDebugger(@SuppressWarnings("unused") final Object receiver) {
            return receiver;
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    @GenerateNodeFactory
    @SqueakPrimitive(indices = 115)
    protected abstract static class PrimChangeClassNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        @Specialization
        protected static final AbstractSqueakObject doPrimChangeClass(final AbstractSqueakObjectWithClassAndHash receiver, final AbstractSqueakObjectWithClassAndHash argument,
                        @Cached final SqueakObjectChangeClassOfToNode changeClassOfToNode) {
            return changeClassOfToNode.execute(receiver, argument.getSqueakClass());
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 116)
    protected abstract static class PrimFlushCacheByMethodNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        @Child protected AbstractPointersObjectReadNode readNode = AbstractPointersObjectReadNode.create();

        @Specialization(guards = "receiver.hasMethodClass(readNode)")
        protected final CompiledMethodObject doFlush(final CompiledMethodObject receiver,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            image.flushMethodCacheForMethod(receiver);
            receiver.getMethodClass(readNode).invalidateMethodDictStableAssumption();
            return receiver;
        }
    }

    /** primitiveExternalCall (#117) handled specially in {@link PrimitiveNodeFactory}. */

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 118)
    protected abstract static class PrimDoPrimitiveWithArgsNode extends AbstractPrimitiveNode implements QuaternaryPrimitive {

        @Specialization(guards = {"primitiveIndex == cachedPrimitiveIndex", "primitiveNode != null"}, limit = "2")
        protected static final Object doPrimitiveWithArgsCached(final VirtualFrame frame, final Object receiver, @SuppressWarnings("unused") final long primitiveIndex, final ArrayObject argumentArray,
                        @SuppressWarnings("unused") final NotProvided notProvided,
                        @SuppressWarnings("unused") @Cached("primitiveIndex") final long cachedPrimitiveIndex,
                        @Cached("createPrimitiveNode(frame, cachedPrimitiveIndex)") final AbstractPrimitiveNode primitiveNode,
                        @Cached final ArrayObjectSizeNode arraySizeNode,
                        @Cached("create(1)") final ArrayObjectCopyIntoObjectArrayNode copyIntoNode) {
            final Object[] arguments = new Object[primitiveNode.getNumArguments()];
            arguments[0] = receiver;
            copyIntoNode.execute(arguments, argumentArray);
            Arrays.fill(arguments, 1 + arraySizeNode.execute(argumentArray), primitiveNode.getNumArguments(), NotProvided.SINGLETON);
            return primitiveNode.executeWithArguments(frame, arguments);
        }

        @Specialization(replaces = "doPrimitiveWithArgsCached")
        protected final Object doPrimitiveWithArgs(final VirtualFrame frame, final Object receiver, final long primitiveIndex, final ArrayObject argumentArray,
                        @SuppressWarnings("unused") final NotProvided notProvided) {
            /* Deopt might be acceptable because primitive is mostly used for debugging anyway. */
            CompilerDirectives.transferToInterpreter();
            final AbstractPrimitiveNode primitiveNode = insert(createPrimitiveNode(frame, primitiveIndex));
            if (primitiveNode == null) {
                throw PrimitiveFailed.GENERIC_ERROR;
            } else {
                final Object[] arguments = new Object[primitiveNode.getNumArguments()];
                arguments[0] = receiver;
                ArrayObjectCopyIntoObjectArrayNode.getUncached1().execute(arguments, argumentArray);
                Arrays.fill(arguments, 1 + ArrayObjectSizeNode.getUncached().execute(argumentArray), primitiveNode.getNumArguments(), NotProvided.SINGLETON);
                return primitiveNode.executeWithArguments(frame, arguments);
            }
        }

        @Specialization(guards = {"primitiveIndex == cachedPrimitiveIndex", "primitiveNode != null"}, limit = "2")
        protected static final Object doPrimitiveWithArgsContextCached(final VirtualFrame frame, @SuppressWarnings("unused") final Object context, final Object receiver,
                        final long primitiveIndex, final ArrayObject argumentArray,
                        @SuppressWarnings("unused") @Cached("primitiveIndex") final long cachedPrimitiveIndex,
                        @Cached("createPrimitiveNode(frame, cachedPrimitiveIndex)") final AbstractPrimitiveNode primitiveNode,
                        @Cached final ArrayObjectSizeNode arraySizeNode,
                        @Cached("create(1)") final ArrayObjectCopyIntoObjectArrayNode copyIntoNode) {
            return doPrimitiveWithArgsCached(frame, receiver, primitiveIndex, argumentArray, NotProvided.SINGLETON, cachedPrimitiveIndex, primitiveNode, arraySizeNode, copyIntoNode);
        }

        @Specialization(replaces = "doPrimitiveWithArgsContextCached")
        protected final Object doPrimitiveWithArgsContext(final VirtualFrame frame, @SuppressWarnings("unused") final Object context, final Object receiver,
                        final long primitiveIndex, final ArrayObject argumentArray) {
            return doPrimitiveWithArgs(frame, receiver, primitiveIndex, argumentArray, NotProvided.SINGLETON);
        }

        protected static final AbstractPrimitiveNode createPrimitiveNode(final VirtualFrame frame, final long primitiveIndex) {
            return PrimitiveNodeFactory.forIndex(FrameAccess.getMethod(frame), false, (int) primitiveIndex);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 119)
    protected abstract static class PrimFlushCacheSelectiveNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        @Specialization
        protected static final NativeObject doFlush(final NativeObject receiver,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            image.flushMethodCacheForSelector(receiver);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 130)
    protected abstract static class PrimFullGCNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        private static final MBeanServer SERVER = TruffleOptions.AOT ? null : ManagementFactory.getPlatformMBeanServer();
        private static final String OPERATION_NAME = "gcRun";
        private static final Object[] PARAMS = new Object[]{null};
        private static final String[] SIGNATURE = new String[]{String[].class.getName()};
        private static final ObjectName OBJECT_NAME;

        static {
            if (TruffleOptions.AOT) {
                OBJECT_NAME = null;
            } else {
                try {
                    OBJECT_NAME = new ObjectName("com.sun.management:type=DiagnosticCommand");
                } catch (final MalformedObjectNameException e) {
                    throw SqueakException.create(e);
                }
            }
        }

        @Specialization
        protected static final long doGC(@SuppressWarnings("unused") final Object receiver,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            if (TruffleOptions.AOT) {
                /* System.gc() triggers full GC by default in SVM (see https://git.io/JvY7g). */
                MiscUtils.systemGC();
            } else {
                forceFullGC();
            }
            final boolean hasPendingFinalizations = LogUtils.GC_IS_LOGGABLE_FINE ? hasPendingFinalizationsWithLogging(image) : hasPendingFinalizations(image);
            if (hasPendingFinalizations) {
                image.interrupt.setPendingFinalizations(true);
            }
            return MiscUtils.runtimeFreeMemory();
        }

        /**
         * {@link System#gc()} does not force a GC, but the DiagnosticCommand "gcRun" does.
         */
        @TruffleBoundary
        private static void forceFullGC() {
            try {
                SERVER.invoke(OBJECT_NAME, OPERATION_NAME, PARAMS, SIGNATURE);
            } catch (InstanceNotFoundException | ReflectionException | MBeanException e) {
                e.printStackTrace();
            }
        }

        @TruffleBoundary
        private static boolean hasPendingFinalizations(final SqueakImageContext image) {
            return image.weakPointersQueue.poll() != null;
        }

        @TruffleBoundary
        private static boolean hasPendingFinalizationsWithLogging(final SqueakImageContext image) {
            final ReferenceQueue<Object> queue = image.weakPointersQueue;
            Reference<? extends Object> element = queue.poll();
            int count = 0;
            while (element != null) {
                count++;
                element = queue.poll();
            }
            LogUtils.GC.log(Level.FINE, "Number of garbage collected WeakPointersObjects: {0}", count);
            return count > 0;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 131)
    protected abstract static class PrimIncrementalGCNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

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

        @Specialization
        protected static final ClassObject doPrimAdoptInstance(final ClassObject receiver, final AbstractSqueakObjectWithClassAndHash argument,
                        @Cached final SqueakObjectChangeClassOfToNode changeClassOfToNode) {
            changeClassOfToNode.execute(argument, receiver);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 167)
    protected abstract static class PrimYieldNode extends AbstractPrimitiveStackIncrementNode implements UnaryPrimitive {
        @Child private LinkProcessToListNode linkProcessToListNode;
        @Child private WakeHighestPriorityNode wakeHighestPriorityNode;

        @Specialization
        protected final Object doYield(final VirtualFrame frame, final PointersObject scheduler,
                        @Cached final ArrayObjectReadNode arrayReadNode,
                        @Cached final GetActiveProcessNode getActiveProcessNode,
                        @Cached final AbstractPointersObjectReadNode pointersReadNode) {
            final PointersObject activeProcess = getActiveProcessNode.execute();
            final long priority = pointersReadNode.executeLong(activeProcess, PROCESS.PRIORITY);
            final ArrayObject processLists = pointersReadNode.executeArray(scheduler, PROCESS_SCHEDULER.PROCESS_LISTS);
            final PointersObject processList = (PointersObject) arrayReadNode.execute(processLists, priority - 1);
            if (!processList.isEmptyList(pointersReadNode)) {
                getLinkProcessToListNode().executeLink(activeProcess, processList);
                try {
                    getWakeHighestPriorityNode().executeWake(frame);
                } catch (final ProcessSwitch ps) {
                    /*
                     * Leave receiver on stack. It has not been removed from the stack yet, so it is
                     * enough to increment the stack pointer.
                     */
                    getFrameStackPointerIncrementNode().execute(frame);
                    throw ps;
                }
            }
            return scheduler;
        }

        private LinkProcessToListNode getLinkProcessToListNode() {
            if (linkProcessToListNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                linkProcessToListNode = insert(LinkProcessToListNode.create());
                wakeHighestPriorityNode = insert(WakeHighestPriorityNode.create());
            }
            return linkProcessToListNode;
        }

        private WakeHighestPriorityNode getWakeHighestPriorityNode() {
            if (wakeHighestPriorityNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
            }
            return wakeHighestPriorityNode;
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 169)
    protected abstract static class PrimNotIdenticalNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {
        @Specialization
        public static final boolean doObject(final Object a, final Object b,
                        @Cached final SqueakObjectIdentityNode identityNode) {
            return !identityNode.execute(a, b);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 185)
    protected abstract static class PrimExitCriticalSectionNode extends AbstractPrimitiveStackIncrementNode implements UnaryPrimitive {
        @Child protected AbstractPointersObjectReadNode readNode = AbstractPointersObjectReadNode.create();
        @Child private AbstractPointersObjectWriteNode writeNode = AbstractPointersObjectWriteNode.create();

        @Specialization(guards = "mutex.isEmptyList(readNode)")
        protected final PointersObject doExitEmpty(final PointersObject mutex) {
            writeNode.executeNil(mutex, MUTEX.OWNER);
            return mutex;
        }

        @Specialization(guards = "!mutex.isEmptyList(readNode)")
        protected final Object doExitNonEmpty(final VirtualFrame frame, final PointersObject mutex,
                        @Cached("create()") final ResumeProcessNode resumeProcessNode) {
            final PointersObject owningProcess = mutex.removeFirstLinkOfList(readNode, writeNode);
            writeNode.execute(mutex, MUTEX.OWNER, owningProcess);
            try {
                resumeProcessNode.executeResume(frame, owningProcess);
            } catch (final ProcessSwitch ps) {
                /*
                 * Leave receiver on stack. It has not been removed from the stack yet, so it is
                 * enough to increment the stack pointer.
                 */
                getFrameStackPointerIncrementNode().execute(frame);
                throw ps;
            }
            return mutex;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 186)
    protected abstract static class PrimEnterCriticalSectionNode extends AbstractPrimitiveStackPushNode implements BinaryPrimitive {
        @Child private AbstractPointersObjectReadNode readNode = AbstractPointersObjectReadNode.create();

        @Specialization(guards = "ownerIsNil(mutex)")
        protected static final boolean doEnterNilOwner(final PointersObject mutex, @SuppressWarnings("unused") final NotProvided notProvided,
                        @Shared("writeNode") @Cached final AbstractPointersObjectWriteNode writeNode,
                        @Shared("getActiveProcessNode") @Cached final GetActiveProcessNode getActiveProcessNode) {
            writeNode.execute(mutex, MUTEX.OWNER, getActiveProcessNode.execute());
            return BooleanObject.FALSE;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "activeProcessMutexOwner(mutex, getActiveProcessNode)", limit = "1")
        protected static final boolean doEnterActiveProcessOwner(final PointersObject mutex, final NotProvided notProvided,
                        @Shared("getActiveProcessNode") @Cached final GetActiveProcessNode getActiveProcessNode) {
            return BooleanObject.TRUE;
        }

        @Specialization(guards = {"!ownerIsNil(mutex)", "!activeProcessMutexOwner(mutex, getActiveProcessNode)"}, limit = "1")
        protected final Object doEnter(final VirtualFrame frame, final PointersObject mutex, @SuppressWarnings("unused") final NotProvided notProvided,
                        @Shared("linkProcessToListNode") @Cached final LinkProcessToListNode linkProcessToListNode,
                        @Shared("wakeHighestPriorityNode") @Cached final WakeHighestPriorityNode wakeHighestPriorityNode,
                        @Shared("getActiveProcessNode") @Cached final GetActiveProcessNode getActiveProcessNode) {
            linkProcessToListNode.executeLink(getActiveProcessNode.execute(), mutex);
            try {
                wakeHighestPriorityNode.executeWake(frame);
            } catch (final ProcessSwitch ps) {
                /* Leave `false` as result on stack. */
                getFrameStackPushNode().execute(frame, BooleanObject.FALSE);
                throw ps;
            }
            return BooleanObject.FALSE;
        }

        @Specialization(guards = "ownerIsNil(mutex)")
        protected static final boolean doEnterNilOwner(final PointersObject mutex, @SuppressWarnings("unused") final PointersObject effectiveProcess,
                        @Shared("writeNode") @Cached final AbstractPointersObjectWriteNode writeNode) {
            writeNode.execute(mutex, MUTEX.OWNER, effectiveProcess);
            return BooleanObject.FALSE;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "isMutexOwner(mutex, effectiveProcess)")
        protected static final boolean doEnterActiveProcessOwner(final PointersObject mutex, final PointersObject effectiveProcess) {
            return BooleanObject.TRUE;
        }

        @Specialization(guards = {"!ownerIsNil(mutex)", "!isMutexOwner(mutex, effectiveProcess)"})
        protected final Object doEnter(final VirtualFrame frame, final PointersObject mutex, @SuppressWarnings("unused") final PointersObject effectiveProcess,
                        @Shared("linkProcessToListNode") @Cached final LinkProcessToListNode linkProcessToListNode,
                        @Shared("wakeHighestPriorityNode") @Cached final WakeHighestPriorityNode wakeHighestPriorityNode) {
            linkProcessToListNode.executeLink(effectiveProcess, mutex);
            try {
                wakeHighestPriorityNode.executeWake(frame);
            } catch (final ProcessSwitch ps) {
                /* Leave `false` as result on stack. */
                getFrameStackPushNode().execute(frame, BooleanObject.FALSE);
                throw ps;
            }
            return BooleanObject.FALSE;
        }

        protected final boolean ownerIsNil(final PointersObject mutex) {
            return readNode.execute(mutex, MUTEX.OWNER) == NilObject.SINGLETON;
        }

        protected final boolean activeProcessMutexOwner(final PointersObject mutex, final GetActiveProcessNode getActiveProcessNode) {
            return readNode.execute(mutex, MUTEX.OWNER) == getActiveProcessNode.execute();
        }

        protected final boolean isMutexOwner(final PointersObject mutex, final PointersObject effectiveProcess) {
            return readNode.execute(mutex, MUTEX.OWNER) == effectiveProcess;
        }
    }

    @ImportStatic(MUTEX.class)
    @GenerateNodeFactory
    @SqueakPrimitive(indices = 187)
    protected abstract static class PrimTestAndSetOwnershipOfCriticalSectionNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        @Child private GetActiveProcessNode getActiveProcessNode = GetActiveProcessNode.create();
        @Child private AbstractPointersObjectReadNode readNode = AbstractPointersObjectReadNode.create();

        @Specialization(guards = {"ownerIsNil(rcvrMutex)"})
        protected final boolean doNilOwner(final PointersObject rcvrMutex,
                        @Cached final AbstractPointersObjectWriteNode writeNode) {
            writeNode.execute(rcvrMutex, MUTEX.OWNER, getActiveProcessNode.execute());
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

        protected final boolean ownerIsNil(final PointersObject mutex) {
            return readNode.execute(mutex, MUTEX.OWNER) == NilObject.SINGLETON;
        }

        protected final boolean ownerIsActiveProcess(final PointersObject mutex) {
            return readNode.execute(mutex, MUTEX.OWNER) == getActiveProcessNode.execute();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 188)
    protected abstract static class PrimExecuteMethodArgsArrayNode extends AbstractPrimitiveNode implements QuaternaryPrimitive {
        @Child protected ArrayObjectSizeNode sizeNode = ArrayObjectSizeNode.create();
        @Child private ArrayObjectReadNode readNode = ArrayObjectReadNode.create();

        /** Deprecated since Kernel-eem.1204. Kept for backward compatibility. */
        @Specialization
        protected final Object doExecute(final VirtualFrame frame, final Object receiver, final ArrayObject argArray, final CompiledMethodObject methodObject,
                        @SuppressWarnings("unused") final NotProvided notProvided,
                        @Cached("create()") final DispatchEagerlyNode dispatchNode) {
            return doExecute(frame, null, receiver, argArray, methodObject, dispatchNode);
        }

        @ExplodeLoop
        @Specialization(guards = "sizeNode.execute(argArray) == cachedNumArgs", limit = "3")
        protected final Object doExecuteCached(final VirtualFrame frame, @SuppressWarnings("unused") final ClassObject compiledMethodClass, final Object receiver, final ArrayObject argArray,
                        final CompiledMethodObject methodObject,
                        @Cached("sizeNode.execute(argArray)") final int cachedNumArgs,
                        @Cached("create()") final DispatchEagerlyNode dispatchNode) {
            final Object[] dispatchRcvrAndArgs = new Object[1 + cachedNumArgs];
            dispatchRcvrAndArgs[0] = receiver;
            for (int i = 0; i < cachedNumArgs; i++) {
                dispatchRcvrAndArgs[1 + i] = readNode.execute(argArray, i);
            }
            return dispatchNode.executeDispatch(frame, methodObject, dispatchRcvrAndArgs);
        }

        @Specialization(replaces = "doExecuteCached")
        protected final Object doExecute(final VirtualFrame frame, @SuppressWarnings("unused") final ClassObject compiledMethodClass, final Object receiver, final ArrayObject argArray,
                        final CompiledMethodObject methodObject,
                        @Cached("create()") final DispatchEagerlyNode dispatchNode) {
            final int numArgs = sizeNode.execute(argArray);
            final Object[] dispatchRcvrAndArgs = new Object[1 + numArgs];
            dispatchRcvrAndArgs[0] = receiver;
            for (int i = 0; i < numArgs; i++) {
                dispatchRcvrAndArgs[1 + i] = readNode.execute(argArray, i);
            }
            return dispatchNode.executeDispatch(frame, methodObject, dispatchRcvrAndArgs);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 218)
    protected abstract static class PrimDoNamedPrimitiveWithArgsNode extends AbstractPrimitiveNode implements QuaternaryPrimitive {

        @Specialization(guards = {"methodObject == cachedMethodObject", "primitiveNode != null"}, limit = "2")
        protected static final Object doNamedPrimitiveWithArgsContextCached(final VirtualFrame frame, @SuppressWarnings("unused") final Object context,
                        @SuppressWarnings("unused") final CompiledMethodObject methodObject, final Object target, final ArrayObject argumentArray,
                        @SuppressWarnings("unused") @Cached("methodObject") final CompiledMethodObject cachedMethodObject,
                        @Cached("createPrimitiveNode(methodObject)") final AbstractPrimitiveNode primitiveNode,
                        @Cached final ArrayObjectSizeNode arraySizeNode,
                        @Cached("create(1)") final ArrayObjectCopyIntoObjectArrayNode copyIntoNode) {
            final Object[] arguments = new Object[primitiveNode.getNumArguments()];
            arguments[0] = target;
            copyIntoNode.execute(arguments, argumentArray);
            Arrays.fill(arguments, 1 + arraySizeNode.execute(argumentArray), primitiveNode.getNumArguments(), NotProvided.SINGLETON);
            return primitiveNode.executeWithArguments(frame, arguments);
        }

        @Specialization(replaces = "doNamedPrimitiveWithArgsContextCached")
        protected final Object doNamedPrimitiveWithArgsContext(final VirtualFrame frame, @SuppressWarnings("unused") final Object context, final CompiledMethodObject methodObject,
                        final Object target, final ArrayObject argumentArray) {
            /* Deopt might be acceptable because primitive is mostly used for debugging anyway. */
            CompilerDirectives.transferToInterpreter();
            final AbstractPrimitiveNode primitiveNode = insert(createPrimitiveNode(methodObject));
            if (primitiveNode == null) {
                throw PrimitiveFailed.GENERIC_ERROR;
            } else {
                final Object[] arguments = new Object[primitiveNode.getNumArguments()];
                arguments[0] = target;
                ArrayObjectCopyIntoObjectArrayNode.getUncached1().execute(arguments, argumentArray);
                Arrays.fill(arguments, 1 + ArrayObjectSizeNode.getUncached().execute(argumentArray), primitiveNode.getNumArguments(), NotProvided.SINGLETON);
                return primitiveNode.executeWithArguments(frame, arguments);
            }
        }

        protected static final AbstractPrimitiveNode createPrimitiveNode(final CompiledMethodObject methodObject) {
            return PrimitiveNodeFactory.namedFor(methodObject, false);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 230)
    protected abstract static class PrimRelinquishProcessorNode extends AbstractPrimitiveStackIncrementNode implements BinaryPrimitive {
        @Specialization
        protected final Object doRelinquish(final VirtualFrame frame, final Object receiver, final long timeMicroseconds,
                        @Cached("createOrNull(true)") final InterruptHandlerNode interruptNode) {
            MiscUtils.sleep(timeMicroseconds / 1000);
            /*
             * Perform interrupt check (even if interrupt handler is not active), otherwise
             * idleProcess gets stuck. Checking whether the interrupt handler `shouldTrigger()`
             * decreases performance for some reason, forcing interrupt check instead.
             */
            if (interruptNode != null) {
                try {
                    interruptNode.executeTrigger(frame);
                } catch (final ProcessSwitch ps) {
                    /*
                     * Leave receiver on stack. It has not been removed from the stack yet, so it is
                     * enough to increment the stack pointer.
                     */
                    getFrameStackPointerIncrementNode().execute(frame);
                    throw ps;
                }
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 231)
    protected abstract static class PrimForceDisplayUpdateNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        @Specialization
        protected static final Object doForceUpdate(final Object receiver) {
            return receiver; // Do nothing.
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 233)
    protected abstract static class PrimSetFullScreenNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        @Specialization
        protected static final Object doFullScreen(final Object receiver, final boolean enable,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            if (image.hasDisplay()) {
                image.getDisplay().setFullscreen(enable);
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 256)
    protected abstract static class PrimQuickReturnSelfNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        @Specialization
        protected static final Object returnValue(final Object receiver) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 257)
    protected abstract static class PrimQuickReturnTrueNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        @Specialization
        protected static final boolean returnValue(@SuppressWarnings("unused") final Object receiver) {
            return BooleanObject.TRUE;
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 258)
    protected abstract static class PrimQuickReturnFalseNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        @Specialization
        protected static final boolean returnValue(@SuppressWarnings("unused") final Object receiver) {
            return BooleanObject.FALSE;
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 259)
    protected abstract static class PrimQuickReturnNilNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        @Specialization
        protected static final NilObject returnValue(@SuppressWarnings("unused") final Object receiver) {
            return NilObject.SINGLETON;
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 260)
    protected abstract static class PrimQuickReturnMinusOneNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        @Specialization
        protected static final long returnValue(@SuppressWarnings("unused") final Object receiver) {
            return -1L;
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 261)
    protected abstract static class PrimQuickReturnZeroNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        @Specialization
        protected static final long returnValue(@SuppressWarnings("unused") final Object receiver) {
            return 0L;
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 262)
    protected abstract static class PrimQuickReturnOneNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        @Specialization
        protected static final long returnValue(@SuppressWarnings("unused") final Object receiver) {
            return 1L;
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 263)
    protected abstract static class PrimQuickReturnTwoNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        @Specialization
        protected static final long returnValue(@SuppressWarnings("unused") final Object receiver) {
            return 2L;
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    public abstract static class PrimLoadInstVarNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();
        private final long variableIndex;

        protected PrimLoadInstVarNode(final long variableIndex) {
            this.variableIndex = variableIndex;
        }

        @Specialization
        protected final Object doReceiverVariable(final Object receiver) {
            return at0Node.execute(receiver, variableIndex);
        }
    }
}
