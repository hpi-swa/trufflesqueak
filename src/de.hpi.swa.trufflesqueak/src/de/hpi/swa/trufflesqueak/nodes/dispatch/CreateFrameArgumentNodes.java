/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.ImportStatic;
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
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetContextOrMarkerNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetOrCreateContextNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.CreateFrameArgumentNodesFactory.CreateFrameArgumentsForIndirectCallNodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.CreateFrameArgumentNodesFactory.GetOrCreateContextOrMarkerNodeGen;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class CreateFrameArgumentNodes {
    private abstract static class AbstractCreateFrameArgumentsForExceptionalNode extends AbstractNode {
        protected final NativeObject selector;
        protected final int argumentCount;
        protected final FrameSlot stackSlot;
        @CompilationFinal protected int stackPointer = -1;
        @CompilationFinal private ContextReference<SqueakImageContext> context;

        private AbstractCreateFrameArgumentsForExceptionalNode(final VirtualFrame frame, final NativeObject selector, final int argumentCount) {
            this.selector = selector;
            this.argumentCount = argumentCount;
            stackSlot = FrameAccess.getStackSlot(frame);
        }

        public final NativeObject getSelector() {
            return selector;
        }

        public final int getArgumentCount() {
            return argumentCount;
        }

        protected final SqueakImageContext getImage() {
            if (context == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                context = lookupContextReference(SqueakLanguage.class);
            }
            return context.get();
        }

        protected final Object getReceiver(final VirtualFrame frame) {
            if (stackPointer == -1) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                stackPointer = FrameAccess.getStackPointerSlow(frame);
            }
            return FrameAccess.getStack(frame, stackSlot)[stackPointer];
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

        public Object[] execute(final VirtualFrame frame, final CompiledCodeObject method, final Object sender) {
            final Object receiver = getReceiver(frame);
            final Object[] arguments = getArguments(frame, stackSlot, stackPointer + 1, argumentCount);
            final ClassObject receiverClass = classNode.executeLookup(receiver);
            final PointersObject message = getImage().newMessage(writeNode, selector, receiverClass, arguments);
            return FrameAccess.newDNUWith(method, sender, receiver, message);
        }
    }

    public static final class CreateFrameArgumentsForOAMNode extends AbstractCreateFrameArgumentsForExceptionalNode {
        private CreateFrameArgumentsForOAMNode(final VirtualFrame frame, final NativeObject selector, final int argumentCount) {
            super(frame, selector, argumentCount);
        }

        public static CreateFrameArgumentsForOAMNode create(final VirtualFrame frame, final NativeObject selector, final int argumentCount) {
            return new CreateFrameArgumentsForOAMNode(frame, selector, argumentCount);
        }

        public Object[] execute(final VirtualFrame frame, final Object cachedObject, final CompiledCodeObject method, final Object sender) {
            final Object receiver = getReceiver(frame);
            final Object[] arguments = getArguments(frame, stackSlot, stackPointer + 1, argumentCount);
            return FrameAccess.newOAMWith(method, sender, cachedObject, selector, getImage().asArrayOfObjects(arguments), receiver);
        }
    }

    @ImportStatic(AbstractDispatchNode.class)
    protected abstract static class CreateFrameArgumentsForIndirectCallNode extends AbstractNode {
        private final NativeObject selector;
        protected final int argumentCount;
        protected final FrameSlot stackSlot;
        @CompilationFinal protected int stackPointer = -1;
        @Child private GetOrCreateContextOrMarkerNode senderNode = GetOrCreateContextOrMarkerNode.create();

        protected CreateFrameArgumentsForIndirectCallNode(final VirtualFrame frame, final NativeObject selector, final int argumentCount) {
            this.selector = selector;
            /* +1 for receiver. */
            stackPointer = FrameAccess.getStackPointerSlow(frame) + 1;
            assert stackPointer >= 0 : "Bad stack pointer";
            stackSlot = FrameAccess.getStackSlot(frame);
            this.argumentCount = argumentCount;
        }

        protected static CreateFrameArgumentsForIndirectCallNode create(final VirtualFrame frame, final NativeObject selector, final int argumentCount) {
            return CreateFrameArgumentsForIndirectCallNodeGen.create(frame, selector, argumentCount);
        }

        protected abstract Object[] execute(VirtualFrame frame, Object receiver, ClassObject receiverClass, Object lookupResult, CompiledCodeObject method);

        @Specialization
        @SuppressWarnings("unused")
        protected final Object[] doMethod(final VirtualFrame frame, final Object receiver, final ClassObject receiverClass, @SuppressWarnings("unused") final CompiledCodeObject lookupResult,
                        final CompiledCodeObject method) {
            return FrameAccess.newWith(frame, method, senderNode.execute(frame, method), receiver, stackSlot, stackPointer, argumentCount);
        }

        @Specialization(guards = "lookupResult == null")
        protected final Object[] doDoesNotUnderstand(final VirtualFrame frame, final Object receiver, final ClassObject receiverClass, @SuppressWarnings("unused") final Object lookupResult,
                        final CompiledCodeObject method,
                        @Cached final AbstractPointersObjectWriteNode writeNode,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            final Object[] arguments = getArguments(frame, stackSlot, stackPointer, argumentCount);
            final PointersObject message = image.newMessage(writeNode, selector, receiverClass, arguments);
            return FrameAccess.newDNUWith(method, senderNode.execute(frame, method), receiver, message);
        }

        @Specialization(guards = {"targetObject != null", "!isCompiledCodeObject(targetObject)"})
        protected final Object[] doObjectAsMethod(final VirtualFrame frame, final Object receiver, @SuppressWarnings("unused") final ClassObject receiverClass, final Object targetObject,
                        final CompiledCodeObject method,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            final Object[] arguments = getArguments(frame, stackSlot, stackPointer, argumentCount);
            return FrameAccess.newOAMWith(method, senderNode.execute(frame, method), targetObject, selector, image.asArrayOfObjects(arguments), receiver);
        }
    }

    @ExplodeLoop
    private static Object[] getArguments(final VirtualFrame frame, final FrameSlot stackSlot, final int stackPointer, final int argumentCount) {
        CompilerAsserts.partialEvaluationConstant(argumentCount);
        return Arrays.copyOfRange(FrameAccess.getStack(frame, stackSlot), stackPointer, stackPointer + argumentCount);
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
