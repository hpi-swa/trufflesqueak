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
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
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
    private abstract static class AbstractCreateFrameArgumentsForExceptionalNode extends AbstractNode {
        @CompilationFinal private ContextReference<SqueakImageContext> context;

        @Child private FrameSlotReadNode receiverNode;
        @Children private FrameSlotReadNode[] argumentNodes;

        private AbstractCreateFrameArgumentsForExceptionalNode(final VirtualFrame frame, final int argumentCount) {
            argumentNodes = new FrameSlotReadNode[argumentCount];
            final int stackPointer = FrameAccess.getStackPointerSlow(frame) + 1;
            for (int i = 0; i < argumentNodes.length; i++) {
                argumentNodes[i] = insert(FrameSlotReadNode.create(frame, stackPointer + i));
            }
        }

        public final int getArgumentCount() {
            return argumentNodes.length;
        }

        protected final SqueakImageContext getImage() {
            if (context == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                context = lookupContextReference();
            }
            return context.get();
        }

        protected final Object getReceiver(final VirtualFrame frame) {
            if (receiverNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                final int stackPointer = FrameAccess.getStackPointerSlow(frame);
                receiverNode = insert(FrameSlotReadNode.create(frame, stackPointer));
            }
            return receiverNode.executeRead(frame);
        }

        @ExplodeLoop
        protected final Object[] getArguments(final VirtualFrame frame) {
            final int argumentCount = argumentNodes.length;
            CompilerAsserts.partialEvaluationConstant(argumentCount);
            final Object[] arguments = new Object[argumentCount];
            for (int i = 0; i < argumentCount; i++) {
                arguments[i] = argumentNodes[i].executeRead(frame);
            }
            return arguments;
        }
    }

    public static final class CreateFrameArgumentsForDNUNode extends AbstractCreateFrameArgumentsForExceptionalNode {
        @Child private AbstractPointersObjectWriteNode writeNode = AbstractPointersObjectWriteNode.create();
        @Child private SqueakObjectClassNode classNode = SqueakObjectClassNode.create();

        private CreateFrameArgumentsForDNUNode(final VirtualFrame frame, final int argumentCount) {
            super(frame, argumentCount);
        }

        public static CreateFrameArgumentsForDNUNode create(final VirtualFrame frame, final int argumentCount) {
            return new CreateFrameArgumentsForDNUNode(frame, argumentCount);
        }

        public Object[] execute(final VirtualFrame frame, final NativeObject cachedSelector, final CompiledCodeObject method, final Object sender) {
            final Object receiver = getReceiver(frame);
            final Object[] arguments = getArguments(frame);
            final ClassObject receiverClass = classNode.executeLookup(receiver);
            final PointersObject message = getImage().newMessage(writeNode, cachedSelector, receiverClass, arguments);
            return FrameAccess.newDNUWith(method, sender, receiver, message);
        }
    }

    public static final class CreateFrameArgumentsForOAMNode extends AbstractCreateFrameArgumentsForExceptionalNode {
        private CreateFrameArgumentsForOAMNode(final VirtualFrame frame, final int argumentCount) {
            super(frame, argumentCount);
        }

        public static CreateFrameArgumentsForOAMNode create(final VirtualFrame frame, final int argumentCount) {
            return new CreateFrameArgumentsForOAMNode(frame, argumentCount);
        }

        public Object[] execute(final VirtualFrame frame, final NativeObject cachedSelector, final Object cachedObject, final CompiledCodeObject method, final Object sender) {
            final Object receiver = getReceiver(frame);
            final Object[] arguments = getArguments(frame);
            return FrameAccess.newOAMWith(method, sender, cachedObject, cachedSelector, getImage().asArrayOfObjects(arguments), receiver);
        }
    }

    @ImportStatic(AbstractDispatchNode.class)
    protected abstract static class CreateFrameArgumentsForIndirectCallNode extends AbstractNode {
        @Children private FrameSlotReadNode[] argumentNodes;
        @Child private GetOrCreateContextOrMarkerNode senderNode = GetOrCreateContextOrMarkerNode.create();

        protected CreateFrameArgumentsForIndirectCallNode(final VirtualFrame frame, final int argumentCount) {
            /* +1 for receiver. */
            final int stackPointer = FrameAccess.getStackPointerSlow(frame) + 1;
            assert stackPointer >= 0 : "Bad stack pointer";
            argumentNodes = new FrameSlotReadNode[argumentCount];
            for (int i = 0; i < argumentNodes.length; i++) {
                argumentNodes[i] = insert(FrameSlotReadNode.create(frame, stackPointer + i));
            }
        }

        protected static CreateFrameArgumentsForIndirectCallNode create(final VirtualFrame frame, final int argumentCount) {
            return CreateFrameArgumentsForIndirectCallNodeGen.create(frame, argumentCount);
        }

        protected abstract Object[] execute(VirtualFrame frame, Object receiver, ClassObject receiverClass, Object lookupResult, CompiledCodeObject method, NativeObject cachedSelector);

        @Specialization
        @SuppressWarnings("unused")
        protected final Object[] doMethod(final VirtualFrame frame, final Object receiver, final ClassObject receiverClass, @SuppressWarnings("unused") final CompiledCodeObject lookupResult,
                        final CompiledCodeObject method, final NativeObject cachedSelector) {
            return FrameAccess.newWith(frame, method, senderNode.execute(frame, method), receiver, argumentNodes);
        }

        @Specialization(guards = "lookupResult == null")
        protected final Object[] doDoesNotUnderstand(final VirtualFrame frame, final Object receiver, final ClassObject receiverClass, @SuppressWarnings("unused") final Object lookupResult,
                        final CompiledCodeObject method, final NativeObject cachedSelector,
                        @Cached final AbstractPointersObjectWriteNode writeNode,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            final Object[] arguments = getArguments(frame);
            final PointersObject message = image.newMessage(writeNode, cachedSelector, receiverClass, arguments);
            return FrameAccess.newDNUWith(method, senderNode.execute(frame, method), receiver, message);
        }

        @Specialization(guards = {"targetObject != null", "!isCompiledCodeObject(targetObject)"})
        protected final Object[] doObjectAsMethod(final VirtualFrame frame, final Object receiver, @SuppressWarnings("unused") final ClassObject receiverClass, final Object targetObject,
                        final CompiledCodeObject method, final NativeObject cachedSelector,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            final Object[] arguments = getArguments(frame);
            return FrameAccess.newOAMWith(method, senderNode.execute(frame, method), targetObject, cachedSelector, image.asArrayOfObjects(arguments), receiver);
        }

        @ExplodeLoop
        private Object[] getArguments(final VirtualFrame frame) {
            final int argumentCount = argumentNodes.length;
            CompilerAsserts.partialEvaluationConstant(argumentCount);
            final Object[] arguments = new Object[argumentCount];
            for (int i = 0; i < argumentCount; i++) {
                arguments[i] = argumentNodes[1 + i].executeRead(frame);
            }
            return arguments;
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
}
