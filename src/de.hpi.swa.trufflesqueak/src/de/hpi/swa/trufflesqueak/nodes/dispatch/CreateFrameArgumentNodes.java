/*
 * Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2023 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.ValueProfile;

import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetContextOrMarkerNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetOrCreateContextNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.CreateFrameArgumentNodesFactory.CreateFrameArgumentsForIndirectCallNodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.CreateFrameArgumentNodesFactory.GetOrCreateContextOrMarkerNodeGen;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class CreateFrameArgumentNodes {
    private abstract static class AbstractCreateFrameArgumentsForExceptionalNode extends AbstractNode {
        protected final NativeObject selector;

        @Child private FrameStackReadNode receiverNode;
        @Children protected FrameStackReadNode[] argumentNodes;

        private AbstractCreateFrameArgumentsForExceptionalNode(final VirtualFrame frame, final NativeObject selector, final int argumentCount) {
            this.selector = selector;
            argumentNodes = new FrameStackReadNode[argumentCount];
            final int stackPointer = FrameAccess.getStackPointer(frame) + 1;
            for (int i = 0; i < argumentNodes.length; i++) {
                argumentNodes[i] = insert(FrameStackReadNode.create(frame, stackPointer + i, true));
            }
        }

        public final NativeObject getSelector() {
            return selector;
        }

        public final int getArgumentCount() {
            return argumentNodes.length;
        }

        protected final Object getReceiver(final VirtualFrame frame) {
            if (receiverNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                final int stackPointer = FrameAccess.getStackPointer(frame);
                receiverNode = insert(FrameStackReadNode.create(frame, stackPointer, true));
            }
            return receiverNode.executeRead(frame);
        }
    }

    public static final class CreateFrameArgumentsForDNUNode extends AbstractCreateFrameArgumentsForExceptionalNode {
        @Child private AbstractPointersObjectWriteNode writeNode = AbstractPointersObjectWriteNode.create();
        @Child private SqueakObjectClassNode classNode = SqueakObjectClassNode.create();

        private CreateFrameArgumentsForDNUNode(final VirtualFrame frame, final NativeObject selector, final int argumentCount) {
            super(frame, selector, argumentCount);
        }

        public static CreateFrameArgumentsForDNUNode create(final VirtualFrame frame, final NativeObject selector, final int argumentCount) {
            return new CreateFrameArgumentsForDNUNode(frame, selector, argumentCount);
        }

        public Object[] execute(final VirtualFrame frame, final Object sender) {
            final Object receiver = getReceiver(frame);
            final Object[] arguments = getArguments(frame, argumentNodes);
            final ClassObject receiverClass = classNode.executeLookup(receiver);
            final PointersObject message = getContext().newMessage(writeNode, selector, receiverClass, arguments);
            return FrameAccess.newDNUWith(sender, receiver, message);
        }
    }

    public static final class CreateFrameArgumentsForOAMNode extends AbstractCreateFrameArgumentsForExceptionalNode {
        private CreateFrameArgumentsForOAMNode(final VirtualFrame frame, final NativeObject selector, final int argumentCount) {
            super(frame, selector, argumentCount);
        }

        public static CreateFrameArgumentsForOAMNode create(final VirtualFrame frame, final NativeObject selector, final int argumentCount) {
            return new CreateFrameArgumentsForOAMNode(frame, selector, argumentCount);
        }

        public Object[] execute(final VirtualFrame frame, final Object cachedObject, final Object sender) {
            final Object receiver = getReceiver(frame);
            final Object[] arguments = getArguments(frame, argumentNodes);
            return FrameAccess.newOAMWith(sender, cachedObject, selector, getContext().asArrayOfObjects(arguments), receiver);
        }
    }

    @ImportStatic(AbstractDispatchNode.class)
    protected abstract static class CreateFrameArgumentsForIndirectCallNode extends AbstractNode {
        private final NativeObject selector;
        @Children private FrameStackReadNode[] argumentNodes;
        @Child private GetOrCreateContextOrMarkerNode senderNode = GetOrCreateContextOrMarkerNode.create();

        protected CreateFrameArgumentsForIndirectCallNode(final VirtualFrame frame, final NativeObject selector, final int argumentCount) {
            this.selector = selector;
            /* +1 for receiver. */
            final int stackPointer = FrameAccess.getStackPointer(frame) + 1;
            assert stackPointer >= 0 : "Bad stack pointer";
            argumentNodes = new FrameStackReadNode[argumentCount];
            for (int i = 0; i < argumentNodes.length; i++) {
                argumentNodes[i] = insert(FrameStackReadNode.create(frame, stackPointer + i, true));
            }
        }

        protected static CreateFrameArgumentsForIndirectCallNode create(final VirtualFrame frame, final NativeObject selector, final int argumentCount) {
            return CreateFrameArgumentsForIndirectCallNodeGen.create(frame, selector, argumentCount);
        }

        protected abstract Object[] execute(VirtualFrame frame, Object receiver, ClassObject receiverClass, Object lookupResult, CompiledCodeObject method);

        @Specialization
        @SuppressWarnings("unused")
        protected final Object[] doMethod(final VirtualFrame frame, final Object receiver, final ClassObject receiverClass, @SuppressWarnings("unused") final CompiledCodeObject lookupResult,
                        final CompiledCodeObject method) {
            return FrameAccess.newWith(frame, senderNode.execute(frame, method), receiver, argumentNodes);
        }

        @Specialization(guards = "lookupResult == null")
        protected final Object[] doDoesNotUnderstand(final VirtualFrame frame, final Object receiver, final ClassObject receiverClass, @SuppressWarnings("unused") final Object lookupResult,
                        final CompiledCodeObject method,
                        @Cached final AbstractPointersObjectWriteNode writeNode) {
            final Object[] arguments = getArguments(frame, argumentNodes);
            final PointersObject message = getContext().newMessage(writeNode, selector, receiverClass, arguments);
            return FrameAccess.newDNUWith(senderNode.execute(frame, method), receiver, message);
        }

        @Specialization(guards = {"targetObject != null", "!isCompiledCodeObject(targetObject)"})
        protected final Object[] doObjectAsMethod(final VirtualFrame frame, final Object receiver, @SuppressWarnings("unused") final ClassObject receiverClass, final Object targetObject,
                        final CompiledCodeObject method) {
            final Object[] arguments = getArguments(frame, argumentNodes);
            return FrameAccess.newOAMWith(senderNode.execute(frame, method), targetObject, selector, getContext().asArrayOfObjects(arguments), receiver);
        }
    }

    @ExplodeLoop
    private static Object[] getArguments(final VirtualFrame frame, final FrameStackReadNode[] argumentNodes) {
        final int argumentCount = argumentNodes.length;
        CompilerAsserts.partialEvaluationConstant(argumentCount);
        final Object[] arguments = new Object[argumentCount];
        for (int i = 0; i < argumentCount; i++) {
            arguments[i] = argumentNodes[i].executeRead(frame);
        }
        return arguments;
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
                        @Cached final GetOrCreateContextNode getOrCreateContextNode) {
            return getOrCreateContextNode.executeGet(frame);
        }

        protected static final boolean doesNotNeedSender(final CompiledCodeObject code, final ValueProfile assumptionProfile) {
            return assumptionProfile.profile(code.getDoesNotNeedSenderAssumption()).isValid();
        }
    }
}
