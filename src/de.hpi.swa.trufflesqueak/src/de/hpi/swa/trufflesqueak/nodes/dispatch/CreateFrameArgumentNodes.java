/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.ValueProfile;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetContextOrMarkerNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetOrCreateContextNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.CreateFrameArgumentNodesFactory.CreateFrameArgumentsForIndirectCallNodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.CreateFrameArgumentNodesFactory.GetOrCreateContextOrMarkerNodeGen;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class CreateFrameArgumentNodes {
    public static final class UpdateStackPointerNode extends AbstractNode {
        @CompilationFinal private FrameSlot stackPointerSlot;
        @CompilationFinal private int stackPointer;

        public static UpdateStackPointerNode create() {
            return new UpdateStackPointerNode();
        }

        public void execute(final VirtualFrame frame, final FrameSlotReadNode[] receiverAndArgumentsNodes) {
            executeInternal(frame, receiverAndArgumentsNodes.length);
        }

        public void execute(final VirtualFrame frame, final int argumentCount) {
            executeInternal(frame, 1 + argumentCount);
        }

        private void executeInternal(final VirtualFrame frame, final int numReceiverAndArguments) {
            if (stackPointerSlot == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                stackPointerSlot = FrameAccess.getStackPointerSlot(frame);
                stackPointer = FrameAccess.getStackPointer(frame, stackPointerSlot) - numReceiverAndArguments;
                assert stackPointer >= 0 : "Bad stack pointer";
            }
            FrameAccess.setStackPointer(frame, stackPointerSlot, stackPointer);
        }

        public void executeRestore(final VirtualFrame frame, final int argumentCount) {
            FrameAccess.setStackPointer(frame, stackPointerSlot, stackPointer + 1 + argumentCount);
        }

        public int getStackPointer() {
            return stackPointer;
        }
    }

    private abstract static class AbstractCreateFrameArgumentsForExceptionalNode extends AbstractNode {
        @CompilationFinal private ContextReference<SqueakImageContext> context;

        protected final SqueakImageContext getImage() {
            if (context == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                context = lookupContextReference();
            }
            return context.get();
        }
    }

    public static final class CreateFrameArgumentsForDNUNode extends AbstractCreateFrameArgumentsForExceptionalNode {
        @Child private AbstractPointersObjectWriteNode writeNode = AbstractPointersObjectWriteNode.create();
        @Child private SqueakObjectClassNode classNode = SqueakObjectClassNode.create();

        public static CreateFrameArgumentsForDNUNode create() {
            return new CreateFrameArgumentsForDNUNode();
        }

        public Object[] execute(final VirtualFrame frame, final NativeObject cachedSelector, final CompiledCodeObject method, final Object sender,
                        final FrameSlotReadNode[] receiverAndArgumentsNodes, final UpdateStackPointerNode updateStackPointerNode) {
            final Object receiver = getReceiver(frame, receiverAndArgumentsNodes);
            final Object[] arguments = getArguments(frame, receiverAndArgumentsNodes);
            updateStackPointerNode.execute(frame, receiverAndArgumentsNodes);
            final ClassObject receiverClass = classNode.executeLookup(receiver);
            final PointersObject message = getImage().newMessage(writeNode, cachedSelector, receiverClass, arguments);
            return FrameAccess.newDNUWith(method, sender, receiver, message);
        }
    }

    public static final class CreateFrameArgumentsForOAMNode extends AbstractCreateFrameArgumentsForExceptionalNode {
        public static CreateFrameArgumentsForOAMNode create() {
            return new CreateFrameArgumentsForOAMNode();
        }

        public Object[] execute(final VirtualFrame frame, final NativeObject cachedSelector, final Object cachedObject, final CompiledCodeObject method, final Object sender,
                        final FrameSlotReadNode[] receiverAndArgumentsNodes, final UpdateStackPointerNode updateStackPointerNode) {
            final Object receiver = getReceiver(frame, receiverAndArgumentsNodes);
            final Object[] arguments = getArguments(frame, receiverAndArgumentsNodes);
            updateStackPointerNode.execute(frame, receiverAndArgumentsNodes);
            return FrameAccess.newOAMWith(method, sender, cachedObject, cachedSelector, getImage().asArrayOfObjects(arguments), receiver);
        }
    }

    protected abstract static class CreateFrameArgumentsForIndirectCallNode extends AbstractNode {
        @Child private GetOrCreateContextOrMarkerNode senderNode = GetOrCreateContextOrMarkerNode.create();

        protected static CreateFrameArgumentsForIndirectCallNode create() {
            return CreateFrameArgumentsForIndirectCallNodeGen.create();
        }

        protected abstract Object[] execute(VirtualFrame frame, Object lookupResult, ClassObject receiverClass, CompiledCodeObject method, NativeObject cachedSelector,
                        FrameSlotReadNode[] receiverAndArgumentsNodes, UpdateStackPointerNode updateStackPointerNode);

        @Specialization
        @SuppressWarnings("unused")
        protected final Object[] doMethod(final VirtualFrame frame, @SuppressWarnings("unused") final CompiledCodeObject lookupResult, final ClassObject receiverClass,
                        final CompiledCodeObject method, final NativeObject cachedSelector, final FrameSlotReadNode[] receiverAndArgumentsNodes, final UpdateStackPointerNode updateStackPointerNode) {
            updateStackPointerNode.execute(frame, receiverAndArgumentsNodes);
            return FrameAccess.newWith(frame, method, senderNode.execute(frame, method), receiverAndArgumentsNodes);
        }

        @Specialization(guards = "lookupResult == null")
        protected final Object[] doDoesNotUnderstand(final VirtualFrame frame, @SuppressWarnings("unused") final Object lookupResult, final ClassObject receiverClass,
                        final CompiledCodeObject method, final NativeObject cachedSelector, final FrameSlotReadNode[] receiverAndArgumentsNodes, final UpdateStackPointerNode updateStackPointerNode,
                        @Cached final AbstractPointersObjectWriteNode writeNode,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            final Object receiver = getReceiver(frame, receiverAndArgumentsNodes);
            final Object[] arguments = getArguments(frame, receiverAndArgumentsNodes);
            updateStackPointerNode.execute(frame, receiverAndArgumentsNodes);
            final PointersObject message = image.newMessage(writeNode, cachedSelector, receiverClass, arguments);
            return FrameAccess.newDNUWith(method, senderNode.execute(frame, method), receiver, message);
        }

        @Specialization(guards = {"targetObject != null", "!isCompiledCodeObject(targetObject)"})
        protected final Object[] doObjectAsMethod(final VirtualFrame frame, final Object targetObject, @SuppressWarnings("unused") final ClassObject receiverClass,
                        final CompiledCodeObject method, final NativeObject cachedSelector, final FrameSlotReadNode[] receiverAndArgumentsNodes, final UpdateStackPointerNode updateStackPointerNode,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            final Object receiver = getReceiver(frame, receiverAndArgumentsNodes);
            final Object[] arguments = getArguments(frame, receiverAndArgumentsNodes);
            updateStackPointerNode.execute(frame, receiverAndArgumentsNodes);
            return FrameAccess.newOAMWith(method, senderNode.execute(frame, method), targetObject, cachedSelector, image.asArrayOfObjects(arguments), receiver);
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    protected abstract static class GetOrCreateContextOrMarkerNode extends AbstractNode {

        protected static GetOrCreateContextOrMarkerNode create() {
            return GetOrCreateContextOrMarkerNodeGen.create();
        }

        protected abstract Object execute(VirtualFrame frame, CompiledCodeObject code);

        @Specialization(guards = "doesNotNeedSender(code, assumptionProfile)", limit = "1")
        protected static final Object doGetContextOrMarker(final VirtualFrame frame, @SuppressWarnings("unused") final CompiledCodeObject code,
                        @SuppressWarnings("unused") @Shared("assumptionProfile") @Cached("createClassProfile()") final ValueProfile assumptionProfile,
                        @Cached final GetContextOrMarkerNode getContextOrMarkerNode) {
            return getContextOrMarkerNode.execute(frame);
        }

        @Specialization(guards = "!doesNotNeedSender(code, assumptionProfile)", limit = "1")
        protected static final ContextObject doGetOrCreateContext(final VirtualFrame frame, @SuppressWarnings("unused") final CompiledCodeObject code,
                        @SuppressWarnings("unused") @Shared("assumptionProfile") @Cached("createClassProfile()") final ValueProfile assumptionProfile,
                        @Cached("create(true)") final GetOrCreateContextNode getOrCreateContextNode) {
            return getOrCreateContextNode.executeGet(frame);
        }

        protected static final boolean doesNotNeedSender(final CompiledCodeObject code, final ValueProfile assumptionProfile) {
            return assumptionProfile.profile(code.getDoesNotNeedSenderAssumption()).isValid();
        }
    }

    private static Object getReceiver(final VirtualFrame frame, final FrameSlotReadNode[] receiverAndArgumentsNodes) {
        return receiverAndArgumentsNodes[0].executeRead(frame);
    }

    @ExplodeLoop
    private static Object[] getArguments(final VirtualFrame frame, final FrameSlotReadNode[] receiverAndArgumentsNodes) {
        final int argumentCount = receiverAndArgumentsNodes.length - 1;
        CompilerAsserts.partialEvaluationConstant(argumentCount);
        final Object[] arguments = new Object[argumentCount];
        for (int i = 0; i < argumentCount; i++) {
            arguments[i] = receiverAndArgumentsNodes[1 + i].executeRead(frame);
        }
        return arguments;
    }
}
