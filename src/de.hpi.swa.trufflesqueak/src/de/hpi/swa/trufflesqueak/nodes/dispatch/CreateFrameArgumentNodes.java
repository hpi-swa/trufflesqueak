/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackPopNNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackPopNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetOrCreateContextNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.CreateFrameArgumentNodesFactory.CreateFrameArgumentsForIndirectCallNodeGen;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class CreateFrameArgumentNodes {
    public static final class CreateFrameArgumentsNode extends AbstractNode {
        @CompilationFinal private FrameSlot stackPointerSlot;
        @CompilationFinal private int stackPointer;
        @Children private FrameSlotReadNode[] receiverAndArgumentsNodes;

        private CreateFrameArgumentsNode(final int argumentCount) {
            receiverAndArgumentsNodes = new FrameSlotReadNode[1 + argumentCount];
        }

        public static CreateFrameArgumentsNode create(final int argumentCount) {
            return new CreateFrameArgumentsNode(argumentCount);
        }

        public Object[] execute(final VirtualFrame frame, final CompiledCodeObject method, final Object sender) {
            if (stackPointerSlot == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                stackPointerSlot = FrameAccess.getStackPointerSlot(frame);
                stackPointer = FrameAccess.getStackPointer(frame, stackPointerSlot) - receiverAndArgumentsNodes.length;
                assert stackPointer >= 0 : "Bad stack pointer";
                for (int i = 0; i < receiverAndArgumentsNodes.length; i++) {
                    receiverAndArgumentsNodes[i] = insert(FrameSlotReadNode.create(frame, stackPointer + i));
                }
            }
            FrameAccess.setStackPointer(frame, stackPointerSlot, stackPointer);
            return FrameAccess.newWith(frame, method, sender, null, receiverAndArgumentsNodes);
        }
    }

    public static final class CreateFrameArgumentsForDNUNode extends AbstractNode {
        @Child private FrameStackPopNNode popArguments;
        @Child private FrameStackPopNode popReceiver = FrameStackPopNode.create();
        @Child private AbstractPointersObjectWriteNode writeNode = AbstractPointersObjectWriteNode.create();
        @Child private SqueakObjectClassNode classNode = SqueakObjectClassNode.create();

        private CreateFrameArgumentsForDNUNode(final int argumentCount) {
            popArguments = FrameStackPopNNode.create(argumentCount);
        }

        public static CreateFrameArgumentsForDNUNode create(final int argumentCount) {
            return new CreateFrameArgumentsForDNUNode(argumentCount);
        }

        public Object[] execute(final VirtualFrame frame, final NativeObject cachedSelector, final CompiledCodeObject method, final Object sender) {
            final Object[] arguments = popArguments.execute(frame);
            final Object receiver = popReceiver.execute(frame);
            final ClassObject receiverClass = classNode.executeLookup(receiver);
            final PointersObject message = method.image.newMessage(writeNode, cachedSelector, receiverClass, arguments);
            return FrameAccess.newDNUWith(method, sender, receiver, message);
        }
    }

    public static final class CreateFrameArgumentsForOAMNode extends AbstractNode {
        @Child private FrameStackPopNNode popArguments;
        @Child private FrameStackPopNode popReceiver = FrameStackPopNode.create();

        private CreateFrameArgumentsForOAMNode(final int argumentCount) {
            popArguments = FrameStackPopNNode.create(argumentCount);
        }

        public static CreateFrameArgumentsForOAMNode create(final int argumentCount) {
            return new CreateFrameArgumentsForOAMNode(argumentCount);
        }

        public Object[] execute(final VirtualFrame frame, final NativeObject cachedSelector, final Object cachedObject, final CompiledCodeObject method, final Object sender) {
            final Object[] arguments = popArguments.execute(frame);
            final Object receiver = popReceiver.execute(frame);
            return FrameAccess.newOAMWith(method, sender, cachedObject, cachedSelector, method.image.asArrayOfObjects(arguments), receiver);
        }
    }

    protected abstract static class CreateFrameArgumentsForIndirectCallNode extends AbstractNode {
        protected final int argumentCount;

        protected CreateFrameArgumentsForIndirectCallNode(final int argumentCount) {
            this.argumentCount = argumentCount;
        }

        protected static CreateFrameArgumentsForIndirectCallNode create(final int argumentCount) {
            return CreateFrameArgumentsForIndirectCallNodeGen.create(argumentCount);
        }

        protected abstract Object[] execute(VirtualFrame frame, Object lookupResult, ClassObject receiverClass, CompiledCodeObject method, NativeObject cachedSelector);

        @Specialization
        @SuppressWarnings("unused")
        protected static final Object[] doMethod(final VirtualFrame frame, @SuppressWarnings("unused") final CompiledCodeObject lookupResult,
                        final ClassObject receiverClass, final CompiledCodeObject method, final NativeObject cachedSelector,
                        @Cached("getStackPointerSlot(frame)") final FrameSlot stackPointerSlot,
                        @Cached("subtract(getStackPointerSlow(frame), add(1, argumentCount))") final int newStackPointer,
                        @Cached("createReceiverAndArgumentsNodes(frame, newStackPointer, argumentCount)") final FrameSlotReadNode[] receiverAndArgumentsNodes,
                        @Cached("create(true)") final GetOrCreateContextNode getOrCreateContextNode) {
            FrameAccess.setStackPointer(frame, stackPointerSlot, newStackPointer);
            return FrameAccess.newWith(frame, method, getOrCreateContextNode.executeGet(frame), null, receiverAndArgumentsNodes);
        }

        protected static final FrameSlotReadNode[] createReceiverAndArgumentsNodes(final VirtualFrame frame, final int newStackPointer, final int argumentCount) {
            final FrameSlotReadNode[] receiverAndArgumentsNodes = new FrameSlotReadNode[1 + argumentCount];
            assert newStackPointer >= 0 : "Bad stack pointer";
            for (int i = 0; i < receiverAndArgumentsNodes.length; i++) {
                receiverAndArgumentsNodes[i] = FrameSlotReadNode.create(frame, newStackPointer + i);
            }
            return receiverAndArgumentsNodes;
        }

        @Specialization(guards = "lookupResult == null")
        protected static final Object[] doDoesNotUnderstand(final VirtualFrame frame, @SuppressWarnings("unused") final Object lookupResult,
                        final ClassObject receiverClass, final CompiledCodeObject method, final NativeObject cachedSelector,
                        @Cached final AbstractPointersObjectWriteNode writeNode,
                        @Shared("popArgumentsNode") @Cached("create(argumentCount)") final FrameStackPopNNode popArgumentsNode,
                        @Shared("popReceiverNode") @Cached final FrameStackPopNode popReceiverNode,
                        @Cached("create(true)") final GetOrCreateContextNode getOrCreateContextNode,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            final Object[] arguments = popArgumentsNode.execute(frame);
            final Object receiver = popReceiverNode.execute(frame);
            final PointersObject message = image.newMessage(writeNode, cachedSelector, receiverClass, arguments);
            return FrameAccess.newDNUWith(method, getOrCreateContextNode.executeGet(frame), receiver, message);
        }

        @Specialization(guards = {"targetObject != null", "!isCompiledCodeObject(targetObject)"})
        protected static final Object[] doObjectAsMethod(final VirtualFrame frame, final Object targetObject,
                        @SuppressWarnings("unused") final ClassObject receiverClass, final CompiledCodeObject method, final NativeObject cachedSelector,
                        @Shared("popArgumentsNode") @Cached("create(argumentCount)") final FrameStackPopNNode popArgumentsNode,
                        @Shared("popReceiverNode") @Cached final FrameStackPopNode popReceiverNode,
                        @Cached("create(true)") final GetOrCreateContextNode getOrCreateContextNode) {
            final Object[] arguments = popArgumentsNode.execute(frame);
            final Object receiver = popReceiverNode.execute(frame);
            return FrameAccess.newOAMWith(method, getOrCreateContextNode.executeGet(frame), targetObject, cachedSelector, method.image.asArrayOfObjects(arguments), receiver);
        }
    }
}
