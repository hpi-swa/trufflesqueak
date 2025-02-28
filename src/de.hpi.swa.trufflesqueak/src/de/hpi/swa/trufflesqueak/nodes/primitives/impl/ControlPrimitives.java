/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.lang.management.ManagementFactory;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
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
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DenyReplace;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakQuit;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObjectWithClassAndHash;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.LINKED_LIST;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.MUTEX;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.SEMAPHORE;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.InheritsFromNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectSizeNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectToObjectArrayCopyNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectChangeClassOfToNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectIdentityNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackPushNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector0Node.Dispatch0Node;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector0Node.DispatchDirect0Node;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector0Node.DispatchIndirect0Node;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector1Node.Dispatch1Node;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector1Node.DispatchDirect1Node;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector1Node.DispatchIndirect1Node;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector2Node.Dispatch2Node;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector2Node.DispatchDirect2Node;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector2Node.DispatchIndirect2Node;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector3Node.Dispatch3Node;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector3Node.DispatchDirect3Node;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector3Node.DispatchIndirect3Node;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector4Node.Dispatch4Node;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector4Node.DispatchDirect4Node;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector4Node.DispatchIndirect4Node;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector5Node.Dispatch5Node;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector5Node.DispatchIndirect5Node;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNode.DispatchDirectNaryNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNode.DispatchDirectedSuperNaryNode.DirectedSuperDispatchNaryNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNode.DispatchIndirectNaryNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNode.DispatchIndirectNaryNode.CreateFrameArgumentsForIndirectCallNaryNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNode.DispatchIndirectNaryNode.TryPrimitiveNaryNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNode.DispatchNaryNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.GetOrCreateContextOrMarkerNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.LookupClassGuard;
import de.hpi.swa.trufflesqueak.nodes.dispatch.ResolveMethodNode;
import de.hpi.swa.trufflesqueak.nodes.interrupts.CheckForInterruptsFullNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode.AbstractPrimitiveWithFrameNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractSingletonPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive2;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive2WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive3WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive4WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive5WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive6WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ControlPrimitivesFactory.PrimLoadInstVarNodeGen;
import de.hpi.swa.trufflesqueak.nodes.process.AddLastLinkToListNode;
import de.hpi.swa.trufflesqueak.nodes.process.GetActiveProcessNode;
import de.hpi.swa.trufflesqueak.nodes.process.RemoveProcessFromListNode;
import de.hpi.swa.trufflesqueak.nodes.process.ResumeProcessNode;
import de.hpi.swa.trufflesqueak.nodes.process.SignalSemaphoreNode;
import de.hpi.swa.trufflesqueak.nodes.process.WakeHighestPriorityNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.LogUtils;
import de.hpi.swa.trufflesqueak.util.MiscUtils;

public final class ControlPrimitives extends AbstractPrimitiveFactoryHolder {
    /* primitiveFail (#19) handled specially. */

    // primitiveBlockCopy / primitiveBlockValue: (#80, #81, #82) no longer needed.

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 83)
    protected abstract static class PrimPerform0Node extends AbstractPrimitiveWithFrameNode implements Primitive1WithFallback {
        @SuppressWarnings("unused")
        @Specialization(guards = "selector == cachedSelector", limit = "PERFORM_SELECTOR_CACHE_LIMIT")
        protected static final Object perform0Cached(final VirtualFrame frame, final Object receiver, final NativeObject selector,
                        @Bind("this") final Node node,
                        @Cached("selector") final NativeObject cachedSelector,
                        @Cached("create(cachedSelector)") final Dispatch0Node dispatchNode) {
            return dispatchNode.execute(frame, receiver);
        }

        @SuppressWarnings("unused")
        @ReportPolymorphism.Megamorphic
        @Specialization(replaces = "perform0Cached")
        protected static final Object perform0(final VirtualFrame frame, final Object receiver, final NativeObject selector,
                        @Cached final DispatchIndirect0Node dispatchNode) {
            return dispatchNode.execute(frame, selector, receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 83)
    protected abstract static class PrimPerform1Node extends AbstractPrimitiveWithFrameNode implements Primitive2WithFallback {
        @SuppressWarnings("unused")
        @Specialization(guards = {"selector == cachedSelector"}, limit = "PERFORM_SELECTOR_CACHE_LIMIT")
        protected static final Object perform1Cached(final VirtualFrame frame, final Object receiver, final NativeObject selector, final Object arg1,
                        @Bind("this") final Node node,
                        @Cached("selector") final NativeObject cachedSelector,
                        @Cached("create(cachedSelector)") final Dispatch1Node dispatchNode) {
            return dispatchNode.execute(frame, receiver, arg1);
        }

        @SuppressWarnings("unused")
        @ReportPolymorphism.Megamorphic
        @Specialization(replaces = "perform1Cached")
        protected static final Object perform1(final VirtualFrame frame, final Object receiver, final NativeObject selector, final Object arg1,
                        @Cached final DispatchIndirect1Node dispatchNode) {
            return dispatchNode.execute(frame, selector, receiver, arg1);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 83)
    protected abstract static class PrimPerform2Node extends AbstractPrimitiveWithFrameNode implements Primitive3WithFallback {
        @SuppressWarnings("unused")
        @Specialization(guards = {"selector == cachedSelector"}, limit = "PERFORM_SELECTOR_CACHE_LIMIT")
        protected static final Object perform2Cached(final VirtualFrame frame, final Object receiver, final NativeObject selector, final Object arg1, final Object arg2,
                        @Bind("this") final Node node,
                        @Cached("selector") final NativeObject cachedSelector,
                        @Cached("create(cachedSelector)") final Dispatch2Node dispatchNode) {
            return dispatchNode.execute(frame, receiver, arg1, arg2);
        }

        @SuppressWarnings("unused")
        @ReportPolymorphism.Megamorphic
        @Specialization(replaces = "perform2Cached")
        protected static final Object perform2(final VirtualFrame frame, final Object receiver, final NativeObject selector, final Object arg1, final Object arg2,
                        @Cached final DispatchIndirect2Node dispatchNode) {
            return dispatchNode.execute(frame, selector, receiver, arg1, arg2);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 83)
    protected abstract static class PrimPerform3Node extends AbstractPrimitiveWithFrameNode implements Primitive4WithFallback {
        @SuppressWarnings("unused")
        @Specialization(guards = {"selector == cachedSelector"}, limit = "PERFORM_SELECTOR_CACHE_LIMIT")
        protected static final Object perform3Cached(final VirtualFrame frame, final Object receiver, final NativeObject selector, final Object arg1, final Object arg2, final Object arg3,
                        @Bind("this") final Node node,
                        @Cached("selector") final NativeObject cachedSelector,
                        @Cached("create(cachedSelector)") final Dispatch3Node dispatchNode) {
            return dispatchNode.execute(frame, receiver, arg1, arg2, arg3);
        }

        @SuppressWarnings("unused")
        @ReportPolymorphism.Megamorphic
        @Specialization(replaces = "perform3Cached")
        protected static final Object perform3(final VirtualFrame frame, final Object receiver, final NativeObject selector, final Object arg1, final Object arg2, final Object arg3,
                        @Cached final DispatchIndirect3Node dispatchNode) {
            return dispatchNode.execute(frame, selector, receiver, arg1, arg2, arg3);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 83)
    protected abstract static class PrimPerform4Node extends AbstractPrimitiveWithFrameNode implements Primitive5WithFallback {
        @SuppressWarnings("unused")
        @Specialization(guards = {"selector == cachedSelector"}, limit = "PERFORM_SELECTOR_CACHE_LIMIT")
        protected static final Object perform4Cached(final VirtualFrame frame, final Object receiver, final NativeObject selector, final Object arg1, final Object arg2, final Object arg3,
                        final Object arg4,
                        @Bind("this") final Node node,
                        @Cached("selector") final NativeObject cachedSelector,
                        @Cached("create(cachedSelector)") final Dispatch4Node dispatchNode) {
            return dispatchNode.execute(frame, receiver, arg1, arg2, arg3, arg4);
        }

        @SuppressWarnings("unused")
        @ReportPolymorphism.Megamorphic
        @Specialization(replaces = "perform4Cached")
        protected static final Object perform4(final VirtualFrame frame, final Object receiver, final NativeObject selector, final Object arg1, final Object arg2, final Object arg3,
                        final Object arg4,
                        @Cached final DispatchIndirect4Node dispatchNode) {
            return dispatchNode.execute(frame, selector, receiver, arg1, arg2, arg3, arg4);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 83)
    protected abstract static class PrimPerform5Node extends AbstractPrimitiveWithFrameNode implements Primitive6WithFallback {
        @SuppressWarnings("unused")
        @Specialization(guards = {"selector == cachedSelector"}, limit = "PERFORM_SELECTOR_CACHE_LIMIT")
        protected static final Object perform5Cached(final VirtualFrame frame, final Object receiver, final NativeObject selector, final Object arg1, final Object arg2, final Object arg3,
                        final Object arg4, final Object arg5,
                        @Bind("this") final Node node,
                        @Cached("selector") final NativeObject cachedSelector,
                        @Cached("create(cachedSelector)") final Dispatch5Node dispatchNode) {
            return dispatchNode.execute(frame, receiver, arg1, arg2, arg3, arg4, arg5);
        }

        @SuppressWarnings("unused")
        @ReportPolymorphism.Megamorphic
        @Specialization(replaces = "perform5Cached")
        protected static final Object perform5(final VirtualFrame frame, final Object receiver, final NativeObject selector, final Object arg1, final Object arg2, final Object arg3,
                        final Object arg4, final Object arg5,
                        @Cached final DispatchIndirect5Node dispatchNode) {
            return dispatchNode.execute(frame, selector, receiver, arg1, arg2, arg3, arg4, arg5);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 84)
    protected abstract static class PrimPerformWithArgumentsNode extends AbstractPrimitiveWithFrameNode implements Primitive2WithFallback {
        @Specialization(guards = "selector == cachedSelector", limit = "PERFORM_SELECTOR_CACHE_LIMIT")
        protected static final Object performCached(final VirtualFrame frame, final Object receiver, @SuppressWarnings("unused") final NativeObject selector, final ArrayObject arguments,
                        @Bind("this") final Node node,
                        @SuppressWarnings("unused") @Cached("selector") final NativeObject cachedSelector,
                        @Exclusive @Cached final ArrayObjectToObjectArrayCopyNode getObjectArrayNode,
                        @Cached("create(cachedSelector)") final DispatchNaryNode dispatchNode) {
            return dispatchNode.execute(frame, receiver, getObjectArrayNode.execute(node, arguments));
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(replaces = "performCached")
        protected static final Object perform(final VirtualFrame frame, final Object receiver, final NativeObject selector, final ArrayObject argumentsArray,
                        @Bind("this") final Node node,
                        @Exclusive @Cached final ArrayObjectToObjectArrayCopyNode getObjectArrayNode,
                        @Cached final DispatchIndirectNaryNode dispatchNode) {
            final Object[] arguments = getObjectArrayNode.execute(node, argumentsArray);
            return dispatchNode.execute(frame, selector, receiver, arguments);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 85)
    protected abstract static class PrimSignalNode extends AbstractPrimitiveWithFrameNode implements Primitive0WithFallback {
        @Specialization(guards = "isSemaphore(receiver)")
        protected static final Object doSignal(final VirtualFrame frame, final PointersObject receiver,
                        @Bind("this") final Node node,
                        @Cached(inline = true) final SignalSemaphoreNode signalSemaphoreNode,
                        @Cached final FrameStackPushNode pushReceiverNode) {
            try {
                signalSemaphoreNode.executeSignal(frame, node, receiver);
            } catch (final ProcessSwitch ps) {
                pushReceiverNode.execute(frame, receiver);
                throw ps;
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 86)
    protected abstract static class PrimWaitNode extends AbstractPrimitiveWithFrameNode implements Primitive0WithFallback {
        @Specialization
        @SuppressWarnings("truffle-static-method")
        protected final PointersObject doWaitExcessSignals(final VirtualFrame frame, final PointersObject receiver,
                        @Bind("this") final Node node,
                        @Cached final AbstractPointersObjectReadNode pointersReadNode,
                        @Cached final AbstractPointersObjectWriteNode writeNode,
                        @Cached final AddLastLinkToListNode addLastLinkToListNode,
                        @Cached final WakeHighestPriorityNode wakeHighestPriorityNode,
                        @Cached final GetActiveProcessNode getActiveProcessNode,
                        @Cached final FrameStackPushNode pushReceiverNode) {
            assert isSemaphore(receiver);
            final long excessSignals = pointersReadNode.executeLong(node, receiver, SEMAPHORE.EXCESS_SIGNALS);
            if (excessSignals > 0) {
                writeNode.execute(node, receiver, SEMAPHORE.EXCESS_SIGNALS, excessSignals - 1);
                return receiver;
            } else {
                addLastLinkToListNode.execute(node, getActiveProcessNode.execute(node), receiver);
                try {
                    throw wakeHighestPriorityNode.executeWake(frame, node);
                } catch (final ProcessSwitch ps) {
                    pushReceiverNode.execute(frame, receiver);
                    throw ps;
                }
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 87)
    protected abstract static class PrimResumeNode extends AbstractPrimitiveWithFrameNode implements Primitive0WithFallback {
        @Specialization
        protected static final Object doResume(final VirtualFrame frame, final PointersObject receiver,
                        @Bind("this") final Node node,
                        @Cached final AbstractPointersObjectReadNode readNode,
                        @Cached final ResumeProcessNode resumeProcessNode,
                        @Cached final FrameStackPushNode pushReceiverNode) {
            if (!(readNode.execute(node, receiver, PROCESS.SUSPENDED_CONTEXT) instanceof ContextObject)) {
                CompilerDirectives.transferToInterpreter();
                throw PrimitiveFailed.GENERIC_ERROR;
            }
            try {
                resumeProcessNode.executeResume(frame, node, receiver);
            } catch (final ProcessSwitch ps) {
                pushReceiverNode.execute(frame, receiver);
                throw ps;
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 88)
    protected abstract static class PrimSuspendNode extends AbstractPrimitiveWithFrameNode implements Primitive0WithFallback {

        @Specialization(guards = "receiver == getActiveProcessNode.execute(node)", limit = "1")
        protected static final Object doSuspendActiveProcess(final VirtualFrame frame, @SuppressWarnings("unused") final PointersObject receiver,
                        @Bind("this") final Node node,
                        @SuppressWarnings("unused") @Exclusive @Cached final GetActiveProcessNode getActiveProcessNode,
                        @Cached final WakeHighestPriorityNode wakeHighestPriorityNode,
                        @Cached final FrameStackPushNode pushNode) {
            try {
                throw wakeHighestPriorityNode.executeWake(frame, node);
            } catch (final ProcessSwitch ps) {
                /* Leave `nil` as result on stack. */
                pushNode.execute(frame, NilObject.SINGLETON);
                throw ps;
            }
        }

        @Specialization(guards = {"receiver != getActiveProcessNode.execute(node)"}, limit = "1")
        protected static final PointersObject doSuspendOtherProcess(final PointersObject receiver,
                        @SuppressWarnings("unused") @Bind("this") final Node node,
                        @SuppressWarnings("unused") @Exclusive @Cached final GetActiveProcessNode getActiveProcessNode,
                        @Cached final RemoveProcessFromListNode removeProcessNode,
                        @Cached final AbstractPointersObjectReadNode readNode,
                        @Cached final AbstractPointersObjectWriteNode writeNode) {
            final Object myListOrNil = readNode.execute(node, receiver, PROCESS.LIST);
            if (!(myListOrNil instanceof final PointersObject myList)) {
                CompilerDirectives.transferToInterpreter();
                assert myListOrNil == NilObject.SINGLETON;
                throw PrimitiveFailed.BAD_RECEIVER;
            }
            removeProcessNode.executeRemove(receiver, myList, readNode, writeNode, node);
            writeNode.execute(node, receiver, PROCESS.LIST, NilObject.SINGLETON);
            return myList;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 578)
    protected abstract static class PrimSuspendBackingUpV2Node extends AbstractPrimitiveWithFrameNode implements Primitive0WithFallback {

        @Specialization(guards = "receiver == getActiveProcessNode.execute(node)", limit = "1")
        protected static final Object doSuspendActiveProcess(final VirtualFrame frame, @SuppressWarnings("unused") final PointersObject receiver,
                        @Bind("this") final Node node,
                        @SuppressWarnings("unused") @Exclusive @Cached final GetActiveProcessNode getActiveProcessNode,
                        @Cached final WakeHighestPriorityNode wakeHighestPriorityNode,
                        @Cached final FrameStackPushNode pushNode) {
            try {
                throw wakeHighestPriorityNode.executeWake(frame, node);
            } catch (final ProcessSwitch ps) {
                /* Leave `nil` as result on stack. */
                pushNode.execute(frame, NilObject.SINGLETON);
                throw ps;
            }
        }

        @Specialization(guards = {"receiver != getActiveProcessNode.execute(node)"}, limit = "1")
        protected static final Object doSuspendOtherProcess(final PointersObject receiver,
                        @SuppressWarnings("unused") @Bind("this") final Node node,
                        @SuppressWarnings("unused") @Exclusive @Cached final GetActiveProcessNode getActiveProcessNode,
                        @Cached final RemoveProcessFromListNode removeProcessNode,
                        @Cached final AbstractPointersObjectReadNode readNode,
                        @Cached final AbstractPointersObjectWriteNode writeNode) {
            final Object myListOrNil = readNode.execute(node, receiver, PROCESS.LIST);
            final Object myContext = readNode.execute(node, receiver, PROCESS.SUSPENDED_CONTEXT);
            if (myListOrNil instanceof final PointersObject myList) {
                removeProcessNode.executeRemove(receiver, myList, readNode, writeNode, node);
                writeNode.execute(node, receiver, PROCESS.LIST, NilObject.SINGLETON);
                if (myList.getSqueakClass() != getContext(node).getLinkedListClass()) {
                    backupContextToBlockingSendTo((ContextObject) myContext, myList);
                    return NilObject.SINGLETON;
                } else {
                    return myList;
                }
            } else {
                CompilerDirectives.transferToInterpreter();
                assert myListOrNil == NilObject.SINGLETON : "Unexpected object for myList";
                throw PrimitiveFailed.BAD_RECEIVER;
            }
        }

        private static void backupContextToBlockingSendTo(final ContextObject myContext, final PointersObject conditionVariable) {
            final int pc = myContext.getInstructionPointerForBytecodeLoop();
            final int sp = myContext.getStackPointer();
            assert pc > 0 && sp > 0;
            final int theNewPC = myContext.getCodeObject().pcPreviousTo(pc);
            assert theNewPC < pc && pc - theNewPC <= 3;
            myContext.setInstructionPointer(theNewPC);
            final int conditionTempIndex = sp + CONTEXT.RECEIVER - CONTEXT.TEMP_FRAME_START;
            assert myContext.atTemp(conditionTempIndex) == Boolean.valueOf(BooleanObject.FALSE) || myContext.atTemp(conditionTempIndex) == conditionVariable;
            myContext.atTempPut(conditionTempIndex, conditionVariable);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 89)
    protected abstract static class PrimFlushCacheNode extends AbstractPrimitiveNode implements Primitive0 {
        @Specialization
        protected final Object doFlush(final Object receiver) {
            getContext().flushMethodCache();
            return receiver;
        }
    }

    protected abstract static class AbstractPrimPerformWithArgumentsInSuperclassNode extends AbstractPrimitiveWithFrameNode {
        protected static final Object performCached(final VirtualFrame frame, final Object receiver, final ArrayObject arguments, final ClassObject lookupClass,
                        final InheritsFromNode inheritsFromNode, final DirectedSuperDispatchNaryNode dispatchNode, final ArrayObjectToObjectArrayCopyNode getObjectArrayNode, final Node node) {
            if (inheritsFromNode.execute(node, receiver, lookupClass)) {
                return dispatchNode.execute(frame, lookupClass, receiver, getObjectArrayNode.execute(node, arguments));
            } else {
                CompilerDirectives.transferToInterpreter();
                throw PrimitiveFailed.BAD_RECEIVER;
            }
        }

        protected static final Object performGeneric(final VirtualFrame frame, final Object receiver, final NativeObject selector, final ArrayObject arguments, final ClassObject lookupClass,
                        final InheritsFromNode inheritsFromNode, final ResolveMethodNode methodNode, final ArrayObjectToObjectArrayCopyNode getObjectArrayNode,
                        final CreateFrameArgumentsForIndirectCallNaryNode argumentsNode, final IndirectCallNode callNode, final Node node) {
            if (inheritsFromNode.execute(node, receiver, lookupClass)) {
                final Object lookupResult = getContext(node).lookup(lookupClass, selector);
                final CompiledCodeObject method = methodNode.execute(node, getContext(node), lookupClass, lookupResult);
                return callNode.call(method.getCallTarget(), argumentsNode.execute(frame, node, receiver, getObjectArrayNode.execute(node, arguments), lookupClass, lookupResult, method, selector));
            } else {
                CompilerDirectives.transferToInterpreter();
                throw PrimitiveFailed.BAD_RECEIVER;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 100)
    /* Object>>#perform:withArguments:inSuperclass: */
    protected abstract static class PrimPerformWithArgumentsInSuperclass4Node extends AbstractPrimPerformWithArgumentsInSuperclassNode implements Primitive3WithFallback {
        @Specialization(guards = "selector == cachedSelector", limit = "PERFORM_SELECTOR_CACHE_LIMIT")
        protected static final Object performCached(final VirtualFrame frame, final Object receiver, @SuppressWarnings("unused") final NativeObject selector, final ArrayObject arguments,
                        final ClassObject lookupClass,
                        @Bind("this") final Node node,
                        @SuppressWarnings("unused") @Cached("selector") final NativeObject cachedSelector,
                        @Exclusive @Cached final InheritsFromNode inheritsFromNode,
                        @Exclusive @Cached final ArrayObjectToObjectArrayCopyNode getObjectArrayNode,
                        @Cached("create(cachedSelector)") final DirectedSuperDispatchNaryNode dispatchNode) {
            return performCached(frame, receiver, arguments, lookupClass, inheritsFromNode, dispatchNode, getObjectArrayNode, node);
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(replaces = "performCached")
        protected static final Object perform(final VirtualFrame frame, final Object receiver, final NativeObject selector, final ArrayObject arguments, final ClassObject lookupClass,
                        @Bind("this") final Node node,
                        @Exclusive @Cached final InheritsFromNode inheritsFromNode,
                        @Cached final ResolveMethodNode methodNode,
                        @Exclusive @Cached final ArrayObjectToObjectArrayCopyNode getObjectArrayNode,
                        @Cached final CreateFrameArgumentsForIndirectCallNaryNode argumentsNode,
                        @Cached final IndirectCallNode callNode) {
            return performGeneric(frame, receiver, selector, arguments, lookupClass, inheritsFromNode, methodNode, getObjectArrayNode, argumentsNode, callNode, node);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 100)
    /* Context>>#object:perform:withArguments:inClass: */
    protected abstract static class PrimPerformWithArgumentsInSuperclass5Node extends AbstractPrimPerformWithArgumentsInSuperclassNode implements Primitive4WithFallback {
        @Specialization(guards = "selector == cachedSelector", limit = "PERFORM_SELECTOR_CACHE_LIMIT")
        protected static final Object performContextCached(final VirtualFrame frame, @SuppressWarnings("unused") final Object receiver, final Object target,
                        @SuppressWarnings("unused") final NativeObject selector, final ArrayObject arguments,
                        final ClassObject lookupClass,
                        @Bind("this") final Node node,
                        @SuppressWarnings("unused") @Cached("selector") final NativeObject cachedSelector,
                        @Exclusive @Cached final InheritsFromNode inheritsFromNode,
                        @Exclusive @Cached final ArrayObjectToObjectArrayCopyNode getObjectArrayNode,
                        @Cached("create(cachedSelector)") final DirectedSuperDispatchNaryNode dispatchNode) {
            return performCached(frame, target, arguments, lookupClass, inheritsFromNode, dispatchNode, getObjectArrayNode, node);
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(replaces = "performContextCached")
        protected static final Object performContext(final VirtualFrame frame, @SuppressWarnings("unused") final Object receiver, final Object target, final NativeObject selector,
                        final ArrayObject arguments, final ClassObject lookupClass,
                        @Bind("this") final Node node,
                        @Exclusive @Cached final InheritsFromNode inheritsFromNode,
                        @Cached final ResolveMethodNode methodNode,
                        @Exclusive @Cached final ArrayObjectToObjectArrayCopyNode getObjectArrayNode,
                        @Cached final CreateFrameArgumentsForIndirectCallNaryNode argumentsNode,
                        @Cached final IndirectCallNode callNode) {
            return performGeneric(frame, target, selector, arguments, lookupClass, inheritsFromNode, methodNode, getObjectArrayNode, argumentsNode, callNode, node);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 110)
    protected abstract static class PrimIdentical2Node extends AbstractPrimitiveNode implements Primitive1 {
        @Specialization
        protected static final boolean doObject(final Object a, final Object b,
                        @Bind("this") final Node node,
                        @Cached final SqueakObjectIdentityNode identityNode) {
            return identityNode.execute(node, a, b);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 110)
    protected abstract static class PrimIdentical3Node extends AbstractPrimitiveNode implements Primitive2 {
        @Specialization
        public static final boolean doObject(@SuppressWarnings("unused") final Object context, final Object a, final Object b,
                        @Bind("this") final Node node,
                        @Cached final SqueakObjectIdentityNode identityNode) {
            return identityNode.execute(node, a, b);
        }
    }

    /*
     * primitiveClass (see Object>>class and Context>>objectClass:).
     */
    @GenerateNodeFactory
    @SqueakPrimitive(indices = 111)
    protected abstract static class PrimClass1Node extends AbstractPrimitiveNode implements Primitive0 {
        @Specialization
        protected static final ClassObject doClass(final Object receiver,
                        @Bind("this") final Node node,
                        @Cached final SqueakObjectClassNode classNode) {
            return classNode.executeLookup(node, receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 111)
    protected abstract static class PrimClass2Node extends AbstractPrimitiveNode implements Primitive1 {
        @Specialization
        protected static final ClassObject doClass(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @Bind("this") final Node node,
                        @Cached final SqueakObjectClassNode classNode) {
            return classNode.executeLookup(node, object);
        }
    }

    @DenyReplace
    @SqueakPrimitive(indices = 112)
    public static final class PrimBytesLeftNode extends AbstractSingletonPrimitiveNode implements Primitive0 {
        @Override
        public Object execute(final VirtualFrame frame, final Object receiver) {
            return MiscUtils.runtimeFreeMemory();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 113)
    protected abstract static class PrimQuit1Node extends AbstractPrimitiveNode implements Primitive0 {
        @SuppressWarnings("unused")
        @Specialization
        protected final Object doQuit(final Object receiver) {
            throw new SqueakQuit(this, 0);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 113)
    protected abstract static class PrimQuit2Node extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected final Object doQuit(@SuppressWarnings("unused") final Object receiver, final long exitStatus) {
            throw new SqueakQuit(this, (int) exitStatus);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 114)
    public abstract static class PrimExitToDebuggerNode extends AbstractPrimitiveNode implements Primitive0 {
        public static final String SELECTOR_NAME = "exitToDebugger";

        @Specialization
        protected static final Object doDebugger(final Object receiver) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 115)
    protected abstract static class PrimChangeClassNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization
        protected static final AbstractSqueakObject doPrimChangeClass(final AbstractSqueakObjectWithClassAndHash receiver, final AbstractSqueakObjectWithClassAndHash argument,
                        @Bind("this") final Node node,
                        @Cached final SqueakObjectChangeClassOfToNode changeClassOfToNode) {
            changeClassOfToNode.execute(node, receiver, argument.getSqueakClass());
            return receiver;
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 116)
    protected abstract static class PrimFlushCacheByMethodNode extends AbstractPrimitiveNode implements Primitive0WithFallback {
        @Specialization(guards = "receiver.hasMethodClass(readNode, node)", limit = "1")
        protected final CompiledCodeObject doFlush(final CompiledCodeObject receiver,
                        @Bind("this") final Node node,
                        @Cached final AbstractPointersObjectReadNode readNode) {
            receiver.flushCache();
            getContext().flushMethodCacheForMethod(receiver);
            /*
             * TODO: maybe the method's callTarget could be invalidated to remove it from any PIC
             * and to avoid invalidating the entire methodDict assumption.
             */
            receiver.getMethodClass(readNode, node).invalidateClassHierarchyAndMethodDictStableAssumption();
            return receiver;
        }
    }

    /** primitiveExternalCall (#117) handled specially in {@link PrimitiveNodeFactory}. */

    protected abstract static class AbstractPrimDoPrimitiveWithArgsNode extends AbstractPrimitiveWithFrameNode {
        protected static final AbstractPrimitiveNode createPrimitiveNode(final long primitiveIndex, final int arraySize) {
            return PrimitiveNodeFactory.getOrCreateIndexed((int) primitiveIndex, 1 + arraySize);
        }

        protected static final Object primitiveWithArgs(final VirtualFrame frame, final Object receiver, final ArrayObject argumentArray,
                        final AbstractPrimitiveNode primitiveNode, final ArrayObjectToObjectArrayCopyNode toObjectArrayNode, final Node inlineTarget) {
            return primitiveNode.executeWithArguments(frame, receiver, toObjectArrayNode.execute(inlineTarget, argumentArray));
        }

        protected final Object primitiveWithArgsSlow(final VirtualFrame frame, final Object receiver, final long primitiveIndex, final ArrayObject argumentArray) {
            /* Deopt might be acceptable because primitive is mostly used for debugging anyway. */
            CompilerDirectives.transferToInterpreter();
            final int arraySize = ArrayObjectSizeNode.executeUncached(argumentArray);
            final AbstractPrimitiveNode primitiveNode = insert(createPrimitiveNode(primitiveIndex, arraySize));
            if (primitiveNode == null) {
                throw PrimitiveFailed.GENERIC_ERROR;
            } else {
                final Object[] arguments = ArrayObjectToObjectArrayCopyNode.executeUncached(argumentArray);
                return primitiveNode.executeWithArguments(frame, receiver, arguments);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 118)
    protected abstract static class PrimDoPrimitiveWithArgs3Node extends AbstractPrimDoPrimitiveWithArgsNode implements Primitive2WithFallback {
        @Specialization(guards = {"primitiveIndex == cachedPrimitiveIndex", "primitiveNode != null", "sizeNode.execute(node, argumentArray) == cachedArraySize"}, limit = "EXECUTE_METHOD_CACHE_LIMIT")
        protected static final Object doPrimitiveWithArgsCached(final VirtualFrame frame, final Object receiver, @SuppressWarnings("unused") final long primitiveIndex, final ArrayObject argumentArray,
                        @Bind("this") final Node node,
                        @SuppressWarnings("unused") @Cached("primitiveIndex") final long cachedPrimitiveIndex,
                        @SuppressWarnings("unused") @Cached final ArrayObjectSizeNode sizeNode,
                        @SuppressWarnings("unused") @Cached("sizeNode.execute(node, argumentArray)") final int cachedArraySize,
                        @Cached("createPrimitiveNode(cachedPrimitiveIndex, cachedArraySize)") final AbstractPrimitiveNode primitiveNode,
                        @Cached final ArrayObjectToObjectArrayCopyNode toObjectArrayNode) {
            return primitiveWithArgs(frame, receiver, argumentArray, primitiveNode, toObjectArrayNode, node);
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(replaces = "doPrimitiveWithArgsCached")
        protected final Object doPrimitiveWithArgs(final VirtualFrame frame, final Object receiver, final long primitiveIndex, final ArrayObject argumentArray) {
            return primitiveWithArgsSlow(frame, receiver, primitiveIndex, argumentArray);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 118)
    protected abstract static class PrimDoPrimitiveWithArgs4Node extends AbstractPrimDoPrimitiveWithArgsNode implements Primitive3WithFallback {
        @Specialization(guards = {"primitiveIndex == cachedPrimitiveIndex", "primitiveNode != null", "sizeNode.execute(node, argumentArray) == cachedArraySize"}, limit = "EXECUTE_METHOD_CACHE_LIMIT")
        protected static final Object doPrimitiveWithArgsContextCached(final VirtualFrame frame, @SuppressWarnings("unused") final Object context, final Object receiver,
                        @SuppressWarnings("unused") final long primitiveIndex, final ArrayObject argumentArray,
                        @Bind("this") final Node node,
                        @SuppressWarnings("unused") @Cached("primitiveIndex") final long cachedPrimitiveIndex,
                        @SuppressWarnings("unused") @Cached final ArrayObjectSizeNode sizeNode,
                        @SuppressWarnings("unused") @Cached("sizeNode.execute(node, argumentArray)") final int cachedArraySize,
                        @Cached("createPrimitiveNode(cachedPrimitiveIndex, cachedArraySize)") final AbstractPrimitiveNode primitiveNode,
                        @Cached final ArrayObjectToObjectArrayCopyNode toObjectArrayNode) {
            return primitiveWithArgs(frame, receiver, argumentArray, primitiveNode, toObjectArrayNode, node);
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(replaces = "doPrimitiveWithArgsContextCached")
        protected final Object doPrimitiveWithArgsContext(final VirtualFrame frame, @SuppressWarnings("unused") final Object context, final Object receiver,
                        final long primitiveIndex, final ArrayObject argumentArray) {
            return primitiveWithArgsSlow(frame, receiver, primitiveIndex, argumentArray);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 119)
    protected abstract static class PrimFlushCacheSelectiveNode extends AbstractPrimitiveNode implements Primitive0WithFallback {
        @Specialization
        protected final NativeObject doFlush(final NativeObject receiver) {
            getContext().flushMethodCacheForSelector(receiver);
            return receiver;
        }
    }

    @DenyReplace
    @SqueakPrimitive(indices = 130)
    public static final class PrimFullGCNode extends AbstractSingletonPrimitiveNode implements Primitive0 {
        private static final MBeanServer SERVER = TruffleOptions.AOT ? null : ManagementFactory.getPlatformMBeanServer();
        private static final String OPERATION_NAME = "gcRun";
        private static final Object[] PARAMS = {null};
        private static final String[] SIGNATURE = {String[].class.getName()};
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

        @Override
        public Object execute(final VirtualFrame frame, final Object receiver) {
            if (TruffleOptions.AOT) {
                /* System.gc() triggers full GC by default in SVM (see https://git.io/JvY7g). */
                MiscUtils.systemGC();
            } else {
                forceFullGC();
            }
            final SqueakImageContext image = getContext();
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
            final ReferenceQueue<AbstractSqueakObject> queue = image.weakPointersQueue;
            Reference<?> element = queue.poll();
            int count = 0;
            while (element != null) {
                count++;
                element = queue.poll();
            }
            LogUtils.GC.log(Level.FINE, "Number of garbage collected WeakPointersObjects: {0}", count);
            return count > 0;
        }
    }

    @DenyReplace
    @SqueakPrimitive(indices = 131)
    public static final class PrimIncrementalGCNode extends AbstractSingletonPrimitiveNode implements Primitive0 {
        @Override
        public Object execute(final VirtualFrame frame, final Object receiver) {
            /* Cannot force incremental GC in Java, suggesting a normal GC instead. */
            MiscUtils.systemGC();
            return MiscUtils.runtimeFreeMemory();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 160)
    protected abstract static class PrimAdoptInstanceNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization
        protected static final ClassObject doPrimAdoptInstance(final ClassObject receiver, final AbstractSqueakObjectWithClassAndHash argument,
                        @Bind("this") final Node node,
                        @Cached final SqueakObjectChangeClassOfToNode changeClassOfToNode) {
            changeClassOfToNode.execute(node, argument, receiver);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 167)
    protected abstract static class PrimYieldNode extends AbstractPrimitiveWithFrameNode implements Primitive0WithFallback {
        @Specialization
        protected static final Object doYield(final VirtualFrame frame, final PointersObject scheduler,
                        @Bind("this") final Node node,
                        @Cached final ArrayObjectReadNode arrayReadNode,
                        @Cached final GetActiveProcessNode getActiveProcessNode,
                        @Cached final AbstractPointersObjectReadNode pointersReadNode,
                        @Cached final AddLastLinkToListNode addLastLinkToListNode,
                        @Cached final WakeHighestPriorityNode wakeHighestPriorityNode,
                        @Cached final FrameStackPushNode pushReceiverNode) {
            final PointersObject activeProcess = getActiveProcessNode.execute(node);
            final long priority = pointersReadNode.executeLong(node, activeProcess, PROCESS.PRIORITY);
            final ArrayObject processLists = pointersReadNode.executeArray(node, scheduler, PROCESS_SCHEDULER.PROCESS_LISTS);
            final PointersObject processList = (PointersObject) arrayReadNode.execute(node, processLists, priority - 1);
            if (processList.isEmptyList(pointersReadNode, node)) {
                return NilObject.SINGLETON;
            }
            addLastLinkToListNode.execute(node, activeProcess, processList);
            try {
                throw wakeHighestPriorityNode.executeWake(frame, node);
            } catch (final ProcessSwitch ps) {
                pushReceiverNode.execute(frame, scheduler);
                throw ps;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 169)
    protected abstract static class PrimNotIdenticalNode extends AbstractPrimitiveNode implements Primitive1 {
        @Specialization
        public static final boolean doObject(final Object a, final Object b,
                        @Bind("this") final Node node,
                        @Cached final SqueakObjectIdentityNode identityNode) {
            return !identityNode.execute(node, a, b);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 185)
    protected abstract static class PrimExitCriticalSectionNode extends AbstractPrimitiveWithFrameNode implements Primitive0WithFallback {
        @Specialization
        protected static final PointersObject doExit(final VirtualFrame frame, final PointersObject mutex,
                        @Bind("this") final Node node,
                        @Cached final AbstractPointersObjectReadNode readNode,
                        @Cached final ExitCriticalSectionNode exitCriticalSectionNode) {
            return exitCriticalSectionNode.execute(frame, node, mutex, readNode.execute(node, mutex, LINKED_LIST.FIRST_LINK));
        }

        @GenerateInline
        @GenerateCached(false)
        protected abstract static class ExitCriticalSectionNode extends AbstractNode {
            protected abstract PointersObject execute(VirtualFrame frame, Node node, PointersObject mutex, Object firstLink);

            @Specialization
            protected static final PointersObject doExitEmpty(final Node node, final PointersObject mutex, @SuppressWarnings("unused") final NilObject firstLink,
                            @Exclusive @Cached final AbstractPointersObjectWriteNode writeNode) {
                writeNode.executeNil(node, mutex, MUTEX.OWNER);
                return mutex;
            }

            @Fallback
            protected static final PointersObject doExitNonEmpty(final VirtualFrame frame, final Node node, final PointersObject mutex, @SuppressWarnings("unused") final Object firstLink,
                            @Cached final AbstractPointersObjectReadNode readNode,
                            @Exclusive @Cached final AbstractPointersObjectWriteNode writeNode,
                            @Cached final ResumeProcessNode resumeProcessNode,
                            @Cached final FrameStackPushNode pushReceiverNode,
                            @Cached final FrameStackPushNode pushFirstLinkNode) {
                final PointersObject owningProcess = mutex.removeFirstLinkOfList(readNode, writeNode, node);
                writeNode.execute(node, mutex, MUTEX.OWNER, owningProcess);
                try {
                    resumeProcessNode.executeResume(frame, node, owningProcess);
                } catch (final ProcessSwitch ps) {
                    pushReceiverNode.execute(frame, mutex);
                    pushFirstLinkNode.execute(frame, firstLink);
                    throw ps;
                }
                return mutex;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 186)
    protected abstract static class PrimEnterCriticalSection1Node extends AbstractPrimitiveWithFrameNode implements Primitive0WithFallback {
        @Specialization
        protected static final Object doEnter(final VirtualFrame frame, final PointersObject mutex,
                        @Bind("this") final Node node,
                        @Cached final AbstractPointersObjectReadNode readNode,
                        @Cached final EnterCriticalSectionNode enterCriticalSectionNode,
                        @Cached final GetActiveProcessNode getActiveProcessNode) {
            return enterCriticalSectionNode.execute(frame, node, mutex, readNode.execute(node, mutex, MUTEX.OWNER), getActiveProcessNode.execute(node));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 186)
    protected abstract static class PrimEnterCriticalSection2Node extends AbstractPrimitiveWithFrameNode implements Primitive1WithFallback {
        @Specialization
        protected static final Object doEnter(final VirtualFrame frame, final PointersObject mutex, final PointersObject effectiveProcess,
                        @Bind("this") final Node node,
                        @Cached final AbstractPointersObjectReadNode readNode,
                        @Cached final EnterCriticalSectionNode enterCriticalSectionNode) {
            return enterCriticalSectionNode.execute(frame, node, mutex, readNode.execute(node, mutex, MUTEX.OWNER), effectiveProcess);
        }
    }

    @GenerateInline
    @GenerateCached(false)
    protected abstract static class EnterCriticalSectionNode extends AbstractNode {
        protected abstract Object execute(VirtualFrame frame, Node node, PointersObject mutex, Object mutexOwner, PointersObject effectiveProcess);

        @Specialization
        protected static final boolean doEnterNilOwner(final Node node, final PointersObject mutex, @SuppressWarnings("unused") final NilObject mutexOwner, final PointersObject effectiveProcess,
                        @Cached final AbstractPointersObjectWriteNode writeNode) {
            writeNode.execute(node, mutex, MUTEX.OWNER, effectiveProcess);
            return BooleanObject.FALSE;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "mutexOwner == effectiveProcess")
        protected static final boolean doEnterActiveProcessOwner(final PointersObject mutex, final PointersObject mutexOwner, final PointersObject effectiveProcess) {
            return BooleanObject.TRUE;
        }

        @Fallback
        protected static final Object doEnter(final VirtualFrame frame, final Node node, final PointersObject mutex, @SuppressWarnings("unused") final Object mutexOwner,
                        final PointersObject effectiveProcess,
                        @Cached final AddLastLinkToListNode addLastLinkToListNode,
                        @Cached final WakeHighestPriorityNode wakeHighestPriorityNode,
                        @Cached(inline = false) final FrameStackPushNode pushNode) {
            addLastLinkToListNode.execute(node, effectiveProcess, mutex);
            try {
                throw wakeHighestPriorityNode.executeWake(frame, node);
            } catch (final ProcessSwitch ps) {
                /* Leave `false` as result on stack. */
                pushNode.execute(frame, BooleanObject.FALSE);
                throw ps;
            }
        }
    }

    @ImportStatic(MUTEX.class)
    @GenerateNodeFactory
    @SqueakPrimitive(indices = 187)
    protected abstract static class PrimTestAndSetOwnershipOfCriticalSectionNode extends AbstractPrimitiveNode implements Primitive0WithFallback {
        @Specialization
        protected static final Object doTestAndSet(final PointersObject rcvrMutex,
                        @Bind("this") final Node node,
                        @Cached final GetActiveProcessNode getActiveProcessNode,
                        @Cached final AbstractPointersObjectReadNode readNode,
                        @Cached final TestAndSetOwnershipOfCriticalSectionNode testAndSetOwnershipOfCriticalSectionNode) {
            return testAndSetOwnershipOfCriticalSectionNode.execute(node, rcvrMutex, readNode.execute(node, rcvrMutex, MUTEX.OWNER), getActiveProcessNode.execute(node));
        }

        @GenerateInline
        @GenerateCached(false)
        protected abstract static class TestAndSetOwnershipOfCriticalSectionNode extends AbstractNode {
            protected abstract Object execute(Node node, PointersObject mutex, Object mutexOwner, PointersObject activeProcess);

            @Specialization
            protected static final boolean doNilOwner(final Node node, final PointersObject mutex, final NilObject owner, final PointersObject activeProcess,
                            @Cached final AbstractPointersObjectWriteNode writeNode) {
                return EnterCriticalSectionNode.doEnterNilOwner(node, mutex, owner, activeProcess, writeNode);
            }

            @SuppressWarnings("unused")
            @Specialization(guards = {"owner == activeProcess"})
            protected static final boolean doOwnerIsActiveProcess(final PointersObject mutex, final PointersObject owner, final PointersObject activeProcess) {
                return BooleanObject.TRUE;
            }

            @SuppressWarnings("unused")
            @Fallback
            protected static final Object doFallback(final PointersObject rcvrMutex, final Object owner, final PointersObject activeProcess) {
                return NilObject.SINGLETON;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 188)
    protected abstract static class PrimExecuteMethodArgsArray3Node extends AbstractPrimitiveWithFrameNode implements Primitive2WithFallback {
        /** Deprecated since Kernel-eem.1204. Kept for backward compatibility. */
        @Specialization(guards = {"method == cachedMethod", "guard.check(receiver)"}, assumptions = "dispatchNode.getAssumptions()", limit = "EXECUTE_METHOD_CACHE_LIMIT")
        protected static final Object doCached(final VirtualFrame frame, final Object receiver, final ArrayObject argArray,
                        @SuppressWarnings("unused") final CompiledCodeObject method,
                        @SuppressWarnings("unused") @Cached("method") final CompiledCodeObject cachedMethod,
                        @SuppressWarnings("unused") @Cached("create(receiver)") final LookupClassGuard guard,
                        @Bind("this") final Node node,
                        @Exclusive @Cached final ArrayObjectToObjectArrayCopyNode arrayNode,
                        @Cached("create(cachedMethod, guard)") final DispatchDirectNaryNode dispatchNode) {
            return dispatchNode.execute(frame, receiver, arrayNode.execute(node, argArray));
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(replaces = "doCached")
        protected static final Object doExecute(final VirtualFrame frame, final Object receiver, final ArrayObject argArray, final CompiledCodeObject method,
                        @Bind("this") final Node node,
                        @Exclusive @Cached final ArrayObjectToObjectArrayCopyNode arrayNode,
                        @Cached final TryPrimitiveNaryNode tryPrimitiveNode,
                        @Cached final GetOrCreateContextOrMarkerNode senderNode,
                        @Cached final IndirectCallNode callNode) {
            final Object[] arguments = arrayNode.execute(node, argArray);
            final Object result = tryPrimitiveNode.execute(frame, method, receiver, arguments);
            if (result != null) {
                return result;
            } else {
                return callNode.call(method.getCallTarget(), FrameAccess.newWith(senderNode.execute(frame, node, method), null, receiver, arguments));
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 188)
    protected abstract static class PrimExecuteMethodArgsArray4Node extends AbstractPrimitiveWithFrameNode implements Primitive3WithFallback {
        @Specialization(guards = {"method == cachedMethod", "guard.check(receiver)"}, assumptions = "dispatchNode.getAssumptions()", limit = "EXECUTE_METHOD_CACHE_LIMIT")
        protected static final Object doCached(final VirtualFrame frame, @SuppressWarnings("unused") final ClassObject compiledMethodClass, final Object receiver, final ArrayObject argArray,
                        @SuppressWarnings("unused") final CompiledCodeObject method,
                        @SuppressWarnings("unused") @Cached("method") final CompiledCodeObject cachedMethod,
                        @SuppressWarnings("unused") @Cached("create(receiver)") final LookupClassGuard guard,
                        @Bind("this") final Node node,
                        @Exclusive @Cached final ArrayObjectToObjectArrayCopyNode arrayNode,
                        @Cached("create(cachedMethod, guard)") final DispatchDirectNaryNode dispatchNode) {
            return dispatchNode.execute(frame, receiver, arrayNode.execute(node, argArray));
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(replaces = "doCached")
        protected static final Object doExecute(final VirtualFrame frame, @SuppressWarnings("unused") final ClassObject compiledMethodClass, final Object receiver, final ArrayObject argArray,
                        final CompiledCodeObject method,
                        @Bind("this") final Node node,
                        @Exclusive @Cached final ArrayObjectToObjectArrayCopyNode arrayNode,
                        @Cached final TryPrimitiveNaryNode tryPrimitiveNode,
                        @Cached final GetOrCreateContextOrMarkerNode senderNode,
                        @Cached final IndirectCallNode callNode) {
            return PrimExecuteMethodArgsArray3Node.doExecute(frame, receiver, argArray, method, node, arrayNode, tryPrimitiveNode, senderNode, callNode);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 189)
    protected abstract static class PrimExecuteMethod2Node extends AbstractPrimitiveWithFrameNode implements Primitive1WithFallback {
        @Specialization(guards = {"method == cachedMethod", "guard.check(receiver)"}, assumptions = "dispatchNode.getAssumptions()", limit = "EXECUTE_METHOD_CACHE_LIMIT")
        protected static final Object doCached(final VirtualFrame frame, final Object receiver, @SuppressWarnings("unused") final CompiledCodeObject method,
                        @SuppressWarnings("unused") @Cached("method") final CompiledCodeObject cachedMethod,
                        @SuppressWarnings("unused") @Cached("create(receiver)") final LookupClassGuard guard,
                        @Cached("create(cachedMethod, guard)") final DispatchDirect0Node dispatchNode) {
            return dispatchNode.execute(frame, receiver);
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(replaces = "doCached")
        protected static final Object doExecute(final VirtualFrame frame, final Object receiver, final CompiledCodeObject method,
                        @Bind("this") final Node node,
                        @Cached final GetOrCreateContextOrMarkerNode senderNode,
                        @Cached final IndirectCallNode callNode) {
            return callNode.call(method.getCallTarget(), FrameAccess.newWith(senderNode.execute(frame, node, method), null, receiver));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 189)
    protected abstract static class PrimExecuteMethod3Node extends AbstractPrimitiveWithFrameNode implements Primitive2WithFallback {
        @Specialization(guards = {"method == cachedMethod", "guard.check(receiver)"}, assumptions = "dispatchNode.getAssumptions()", limit = "EXECUTE_METHOD_CACHE_LIMIT")
        protected static final Object doCached(final VirtualFrame frame, final Object receiver, final Object arg1, @SuppressWarnings("unused") final CompiledCodeObject method,
                        @SuppressWarnings("unused") @Cached("method") final CompiledCodeObject cachedMethod,
                        @SuppressWarnings("unused") @Cached("create(receiver)") final LookupClassGuard guard,
                        @Cached("create(cachedMethod, guard)") final DispatchDirect1Node dispatchNode) {
            return dispatchNode.execute(frame, receiver, arg1);
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(replaces = "doCached")
        protected static final Object doExecute(final VirtualFrame frame, final Object receiver, final Object arg1, final CompiledCodeObject method,
                        @Bind("this") final Node node,
                        @Cached final GetOrCreateContextOrMarkerNode senderNode,
                        @Cached final IndirectCallNode callNode) {
            return callNode.call(method.getCallTarget(), FrameAccess.newWith(senderNode.execute(frame, node, method), null, receiver, arg1));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 189)
    protected abstract static class PrimExecuteMethod4Node extends AbstractPrimitiveWithFrameNode implements Primitive3WithFallback {
        @Specialization(guards = {"method == cachedMethod", "guard.check(receiver)"}, assumptions = "dispatchNode.getAssumptions()", limit = "EXECUTE_METHOD_CACHE_LIMIT")
        protected static final Object doCached(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, @SuppressWarnings("unused") final CompiledCodeObject method,
                        @SuppressWarnings("unused") @Cached("method") final CompiledCodeObject cachedMethod,
                        @SuppressWarnings("unused") @Cached("create(receiver)") final LookupClassGuard guard,
                        @Cached("create(cachedMethod, guard)") final DispatchDirect2Node dispatchNode) {
            return dispatchNode.execute(frame, receiver, arg1, arg2);
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(replaces = "doCached")
        protected static final Object doExecute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final CompiledCodeObject method,
                        @Bind("this") final Node node,
                        @Cached final GetOrCreateContextOrMarkerNode senderNode,
                        @Cached final IndirectCallNode callNode) {
            return callNode.call(method.getCallTarget(), FrameAccess.newWith(senderNode.execute(frame, node, method), null, receiver, arg1, arg2));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 189)
    protected abstract static class PrimExecuteMethod5Node extends AbstractPrimitiveWithFrameNode implements Primitive4WithFallback {
        @Specialization(guards = {"method == cachedMethod", "guard.check(receiver)"}, assumptions = "dispatchNode.getAssumptions()", limit = "EXECUTE_METHOD_CACHE_LIMIT")
        protected static final Object doCached(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3,
                        @SuppressWarnings("unused") final CompiledCodeObject method,
                        @SuppressWarnings("unused") @Cached("method") final CompiledCodeObject cachedMethod,
                        @SuppressWarnings("unused") @Cached("create(receiver)") final LookupClassGuard guard,
                        @Cached("create(cachedMethod, guard)") final DispatchDirect3Node dispatchNode) {
            return dispatchNode.execute(frame, receiver, arg1, arg2, arg3);
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(replaces = "doCached")
        protected static final Object doExecute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final CompiledCodeObject method,
                        @Bind("this") final Node node,
                        @Cached final GetOrCreateContextOrMarkerNode senderNode,
                        @Cached final IndirectCallNode callNode) {
            return callNode.call(method.getCallTarget(), FrameAccess.newWith(senderNode.execute(frame, node, method), null, receiver, arg1, arg2, arg3));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 189)
    protected abstract static class PrimExecuteMethod6Node extends AbstractPrimitiveWithFrameNode implements Primitive5WithFallback {
        @Specialization(guards = {"method == cachedMethod", "guard.check(receiver)"}, assumptions = "dispatchNode.getAssumptions()", limit = "EXECUTE_METHOD_CACHE_LIMIT")
        protected static final Object doCached(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4,
                        @SuppressWarnings("unused") final CompiledCodeObject method,
                        @SuppressWarnings("unused") @Cached("method") final CompiledCodeObject cachedMethod,
                        @SuppressWarnings("unused") @Cached("create(receiver)") final LookupClassGuard guard,
                        @Cached("create(cachedMethod, guard)") final DispatchDirect4Node dispatchNode) {
            return dispatchNode.execute(frame, receiver, arg1, arg2, arg3, arg4);
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(replaces = "doCached")
        protected static final Object doExecute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4,
                        final CompiledCodeObject method,
                        @Bind("this") final Node node,
                        @Cached final GetOrCreateContextOrMarkerNode senderNode,
                        @Cached final IndirectCallNode callNode) {
            return callNode.call(method.getCallTarget(), FrameAccess.newWith(senderNode.execute(frame, node, method), null, receiver, arg1, arg2, arg3, arg4));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 218)
    protected abstract static class PrimDoNamedPrimitiveWithArgsNode extends AbstractPrimitiveWithFrameNode implements Primitive3WithFallback {
        @Specialization(guards = {"methodObject == cachedMethodObject", "primitiveNode != null", "sizeNode.execute(node, argumentArray) == cachedArraySize",
                        "cachedArraySize == cachedMethodObject.getNumArgs()"}, limit = "EXECUTE_METHOD_CACHE_LIMIT")
        protected static final Object doNamedPrimitiveWithArgsContextCached(final VirtualFrame frame, @SuppressWarnings("unused") final Object context,
                        @SuppressWarnings("unused") final CompiledCodeObject methodObject, final Object target, final ArrayObject argumentArray,
                        @Bind("this") final Node node,
                        @SuppressWarnings("unused") @Cached("methodObject") final CompiledCodeObject cachedMethodObject,
                        @SuppressWarnings("unused") @Cached final ArrayObjectSizeNode sizeNode,
                        @SuppressWarnings("unused") @Cached("sizeNode.execute(node, argumentArray)") final int cachedArraySize,
                        @Cached("createPrimitiveNode(methodObject)") final AbstractPrimitiveNode primitiveNode,
                        @Cached final ArrayObjectToObjectArrayCopyNode toObjectArrayNode) {
            return primitiveNode.executeWithArguments(frame, target, toObjectArrayNode.execute(node, argumentArray));
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(replaces = "doNamedPrimitiveWithArgsContextCached")
        protected final Object doNamedPrimitiveWithArgsContextUncached(final VirtualFrame frame, @SuppressWarnings("unused") final Object context, final CompiledCodeObject methodObject,
                        final Object target, final ArrayObject argumentArray) {
            /* Deopt might be acceptable because primitive is mostly used for debugging anyway. */
            CompilerDirectives.transferToInterpreter();
            final int arraySize = ArrayObjectSizeNode.executeUncached(argumentArray);
            assert arraySize == methodObject.getNumArgs();
            final AbstractPrimitiveNode primitiveNode = insert(createPrimitiveNode(methodObject));
            if (primitiveNode == null) {
                throw PrimitiveFailed.GENERIC_ERROR;
            } else {
                final Object[] arguments = ArrayObjectToObjectArrayCopyNode.executeUncached(argumentArray);
                return primitiveNode.executeWithArguments(frame, target, arguments);
            }
        }

        protected static final AbstractPrimitiveNode createPrimitiveNode(final CompiledCodeObject method) {
            return PrimitiveNodeFactory.getOrCreateNamed(method, 1 + method.getNumArgs());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 230)
    protected abstract static class PrimRelinquishProcessorNode extends AbstractPrimitiveWithFrameNode implements Primitive1WithFallback {
        @Specialization
        protected static final Object doRelinquish(final VirtualFrame frame, final Object receiver, final long timeMicroseconds,
                        @Cached final CheckForInterruptsFullNode interruptNode,
                        @Cached final FrameStackPushNode pushReceiverNode) {
            MiscUtils.sleep(timeMicroseconds / 1000);
            /*
             * Perform interrupt check (even if interrupt handler is not active), otherwise
             * idleProcess gets stuck.
             */
            try {
                interruptNode.execute(frame);
            } catch (final ProcessSwitch ps) {
                pushReceiverNode.execute(frame, receiver);
                throw ps;
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 231)
    protected abstract static class PrimForceDisplayUpdateNode extends AbstractPrimitiveNode implements Primitive0 {
        @Specialization
        protected static final Object doForceUpdate(final Object receiver) {
            return receiver; // Do nothing.
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 233)
    protected abstract static class PrimSetFullScreenNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected final Object doFullScreen(final Object receiver, final boolean enable) {
            final SqueakImageContext image = getContext();
            if (image.hasDisplay()) {
                image.getDisplay().setFullscreen(enable);
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 256)
    protected static class PrimQuickReturnSelfNode extends AbstractSingletonPrimitiveNode implements Primitive0 {
        @Override
        public Object execute(final VirtualFrame frame, final Object receiver) {
            return receiver;
        }
    }

    @DenyReplace
    @SqueakPrimitive(indices = 257)
    public static final class PrimQuickReturnTrueNode extends AbstractSingletonPrimitiveNode implements Primitive0 {
        @Override
        public Object execute(final VirtualFrame frame, final Object receiver) {
            return BooleanObject.TRUE;
        }
    }

    @DenyReplace
    @SqueakPrimitive(indices = 258)
    public static final class PrimQuickReturnFalseNode extends AbstractSingletonPrimitiveNode implements Primitive0 {
        @Override
        public Object execute(final VirtualFrame frame, final Object receiver) {
            return BooleanObject.FALSE;
        }
    }

    @DenyReplace
    @SqueakPrimitive(indices = 259)
    public static final class PrimQuickReturnNilNode extends AbstractSingletonPrimitiveNode implements Primitive0 {
        @Override
        public Object execute(final VirtualFrame frame, final Object receiver) {
            return NilObject.SINGLETON;
        }
    }

    @DenyReplace
    @SqueakPrimitive(indices = 260)
    public static final class PrimQuickReturnMinusOneNode extends AbstractSingletonPrimitiveNode implements Primitive0 {
        @Override
        public Object execute(final VirtualFrame frame, final Object receiver) {
            return -1L;
        }
    }

    @DenyReplace
    @SqueakPrimitive(indices = 261)
    public static final class PrimQuickReturnZeroNode extends AbstractSingletonPrimitiveNode implements Primitive0 {
        @Override
        public Object execute(final VirtualFrame frame, final Object receiver) {
            return 0L;
        }
    }

    @DenyReplace
    @SqueakPrimitive(indices = 262)
    public static final class PrimQuickReturnOneNode extends AbstractSingletonPrimitiveNode implements Primitive0 {
        @Override
        public Object execute(final VirtualFrame frame, final Object receiver) {
            return 1L;
        }
    }

    @DenyReplace
    @SqueakPrimitive(indices = 263)
    public static final class PrimQuickReturnTwoNode extends AbstractSingletonPrimitiveNode implements Primitive0 {
        @Override
        public Object execute(final VirtualFrame frame, final Object receiver) {
            return 2L;
        }
    }

    public abstract static class PrimLoadInstVarNode extends AbstractPrimitiveNode implements Primitive0 {
        private final long variableIndex;

        protected PrimLoadInstVarNode(final long variableIndex) {
            this.variableIndex = variableIndex;
        }

        public static PrimLoadInstVarNode create(final long variableIndex) {
            return PrimLoadInstVarNodeGen.create(variableIndex);
        }

        @Specialization
        protected final Object doReceiverVariable(final Object receiver,
                        @Bind("this") final Node node,
                        @Cached final SqueakObjectAt0Node at0Node) {
            return at0Node.execute(node, receiver, variableIndex);
        }
    }

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return ControlPrimitivesFactory.getFactories();
    }

    @Override
    public List<? extends AbstractSingletonPrimitiveNode> getSingletonPrimitives() {
        return List.of(
                        new PrimQuickReturnSelfNode(),
                        new PrimQuickReturnTrueNode(),
                        new PrimQuickReturnFalseNode(),
                        new PrimQuickReturnNilNode(),
                        new PrimQuickReturnMinusOneNode(),
                        new PrimQuickReturnZeroNode(),
                        new PrimQuickReturnOneNode(),
                        new PrimQuickReturnTwoNode(),
                        new PrimBytesLeftNode(),
                        new PrimFullGCNode(),
                        new PrimIncrementalGCNode());
    }
}
