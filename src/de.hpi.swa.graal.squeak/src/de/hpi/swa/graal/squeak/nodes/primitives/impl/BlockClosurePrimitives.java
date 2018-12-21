package de.hpi.swa.graal.squeak.nodes.primitives.impl;

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.graal.squeak.nodes.BlockActivationNode;
import de.hpi.swa.graal.squeak.nodes.GetBlockFrameArgumentsNode;
import de.hpi.swa.graal.squeak.nodes.GetOrCreateContextNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.GetObjectArrayNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectSizeNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.QuaternaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.QuinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class BlockClosurePrimitives extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return BlockClosurePrimitivesFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 195)
    protected abstract static class PrimFindNextUnwindContextUpToNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        @Child private GetOrCreateContextNode contextNode;

        public PrimFindNextUnwindContextUpToNode(final CompiledMethodObject method) {
            super(method);
            contextNode = GetOrCreateContextNode.create(method);
        }

        @Specialization(guards = {"receiver.hasVirtualSender()"})
        @TruffleBoundary
        protected final Object doFindNextVirtualized(final ContextObject receiver, final ContextObject previousContext) {
            final ContextObject handlerContext = Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<ContextObject>() {
                boolean foundMyself = false;

                @Override
                public ContextObject visitFrame(final FrameInstance frameInstance) {
                    final Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_ONLY);
                    if (!FrameAccess.isGraalSqueakFrame(current)) {
                        return null;
                    }
                    final CompiledCodeObject codeObject = FrameAccess.getMethod(current);
                    final Object contextOrMarker = current.getValue(codeObject.thisContextOrMarkerSlot);
                    if (!foundMyself) {
                        if (receiver == contextOrMarker) {
                            foundMyself = true;
                        }
                    } else {
                        if (previousContext != null && previousContext == contextOrMarker) {
                            return null;
                        } else {
                            final CompiledCodeObject frameMethod = FrameAccess.getMethod(current);
                            if (frameMethod.isUnwindMarked()) {
                                CompilerDirectives.bailout("Finding materializable frames should never be part of compiled code as it triggers deopts");
                                return contextNode.executeGet(frameInstance.getFrame(FrameInstance.FrameAccess.MATERIALIZE));
                            }
                        }
                    }
                    return null;
                }
            });
            if (handlerContext == null) {
                return code.image.nil;
            } else {
                return handlerContext;
            }
        }

        @Specialization(guards = {"receiver.hasVirtualSender()"})
        protected final Object doFindNextVirtualizedNil(final ContextObject receiver, @SuppressWarnings("unused") final NilObject nil) {
            return doFindNextVirtualized(receiver, null);
        }

        @Specialization(guards = {"!receiver.hasVirtualSender()"})
        protected final Object doFindNext(final ContextObject receiver, final AbstractSqueakObject previousContextOrNil) {
            ContextObject current = receiver;
            while (current != previousContextOrNil) {
                final AbstractSqueakObject sender = current.getSender();
                if (sender == code.image.nil || sender == previousContextOrNil) {
                    break;
                } else {
                    current = (ContextObject) sender;
                    if (current.isUnwindContext()) {
                        return current;
                    }
                }
            }
            return code.image.nil;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 196)
    protected abstract static class PrimTerminateToNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        public PrimTerminateToNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "hasSender(receiver, previousContext)")
        protected static final Object doUnwindAndTerminate(final ContextObject receiver, final ContextObject previousContext) {
            /*
             * Terminate all the Contexts between me and previousContext, if previousContext is on
             * my Context stack. Make previousContext my sender.
             */
            ContextObject currentContext = receiver.getNotNilSender();
            while (currentContext != previousContext) {
                final ContextObject sendingContext = currentContext.getNotNilSender();
                currentContext.terminate();
                currentContext = sendingContext;
            }
            receiver.atput0(CONTEXT.SENDER_OR_NIL, previousContext); // flagging context as dirty
            return receiver;
        }

        @Specialization(guards = "!hasSender(receiver, previousContext)")
        protected static final Object doTerminate(final ContextObject receiver, final ContextObject previousContext) {
            receiver.atput0(CONTEXT.SENDER_OR_NIL, previousContext); // flagging context as dirty
            return receiver;
        }

        @Specialization
        protected static final Object doTerminate(final ContextObject receiver, final NilObject nil) {
            receiver.atput0(CONTEXT.SENDER_OR_NIL, nil); // flagging context as dirty
            return receiver;
        }

        /*
         * Answer whether the receiver is strictly above context on the stack (Context>>hasSender:).
         */
        protected final boolean hasSender(final ContextObject context, final ContextObject previousContext) {
            if (context == previousContext) {
                return false;
            }
            AbstractSqueakObject sender = context.getSender();
            while (sender != code.image.nil) {
                if (sender == previousContext) {
                    return true;
                }
                sender = ((ContextObject) sender).getSender();
            }
            return false;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 197)
    protected abstract static class PrimNextHandlerContextNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        @Child private GetOrCreateContextNode contextNode;

        protected PrimNextHandlerContextNode(final CompiledMethodObject method) {
            super(method);
            contextNode = GetOrCreateContextNode.create(code);
        }

        @Specialization(guards = {"receiver.hasVirtualSender()"})
        @TruffleBoundary
        protected final Object findNextVirtualized(final ContextObject receiver) {
            final ContextObject handlerContext = Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<ContextObject>() {
                boolean foundMyself = false;

                @Override
                public ContextObject visitFrame(final FrameInstance frameInstance) {
                    final Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_ONLY);
                    if (!FrameAccess.isGraalSqueakFrame(current)) {
                        return null;
                    }
                    if (!foundMyself) {
                        final CompiledCodeObject codeObject = FrameAccess.getMethod(current);
                        final Object contextOrMarker = current.getValue(codeObject.thisContextOrMarkerSlot);
                        if (receiver == contextOrMarker) {
                            foundMyself = true;
                        }
                    } else {
                        final CompiledCodeObject frameMethod = FrameAccess.getMethod(current);
                        if (frameMethod.isExceptionHandlerMarked()) {
                            CompilerDirectives.bailout("Finding materializable frames should never be part of compiled code as it triggers deopts");
                            return contextNode.executeGet(frameInstance.getFrame(FrameInstance.FrameAccess.MATERIALIZE));
                        }
                    }
                    return null;
                }
            });
            if (handlerContext == null) {
                return code.image.nil;
            } else {
                return handlerContext;
            }
        }

        @Specialization(guards = {"!receiver.hasVirtualSender()"})
        protected final Object findNext(final ContextObject receiver) {
            ContextObject context = receiver;
            while (true) {
                if (context.getMethod().isExceptionHandlerMarked()) {
                    return context;
                }
                final AbstractSqueakObject sender = context.getSender();
                if (sender instanceof ContextObject) {
                    context = (ContextObject) sender;
                } else {
                    assert sender == code.image.nil;
                    return code.image.nil;
                }
            }
        }

    }

    private abstract static class AbstractClosureValuePrimitiveNode extends AbstractPrimitiveNode {
        @Child protected BlockActivationNode dispatch = BlockActivationNode.create();
        @Child protected GetBlockFrameArgumentsNode getFrameArguments = GetBlockFrameArgumentsNode.create();

        protected AbstractClosureValuePrimitiveNode(final CompiledMethodObject method) {
            super(method);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 200)
    public abstract static class PrimClosureCopyWithCopiedValuesNode extends AbstractPrimitiveNode implements TernaryPrimitive {

        protected PrimClosureCopyWithCopiedValuesNode(final CompiledMethodObject method) {
            super(method);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected static final Object doCopy(final VirtualFrame frame, final ContextObject outerContext, final long numArgs, final ArrayObject copiedValues) {
            throw new SqueakException("Not implemented and not used in Squeak anymore");
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 201)
    public abstract static class PrimClosureValue0Node extends AbstractClosureValuePrimitiveNode implements UnaryPrimitive {

        protected PrimClosureValue0Node(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"block.getCompiledBlock().getNumArgs() == 0"})
        protected final Object doClosure(final VirtualFrame frame, final BlockClosureObject block) {
            return dispatch.executeBlock(block, getFrameArguments.execute(block, getContextOrMarker(frame), new Object[0]));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 202)
    protected abstract static class PrimClosureValue1Node extends AbstractClosureValuePrimitiveNode implements BinaryPrimitive {

        protected PrimClosureValue1Node(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"block.getCompiledBlock().getNumArgs() == 1"})
        protected final Object doValue(final VirtualFrame frame, final BlockClosureObject block, final Object arg) {
            return dispatch.executeBlock(block, getFrameArguments.execute(block, getContextOrMarker(frame), new Object[]{arg}));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 203)
    protected abstract static class PrimClosureValue2Node extends AbstractClosureValuePrimitiveNode implements TernaryPrimitive {

        protected PrimClosureValue2Node(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"block.getCompiledBlock().getNumArgs() == 2"})
        protected final Object doValue(final VirtualFrame frame, final BlockClosureObject block, final Object arg1, final Object arg2) {
            return dispatch.executeBlock(block, getFrameArguments.execute(block, getContextOrMarker(frame), new Object[]{arg1, arg2}));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 204)
    protected abstract static class PrimClosureValue3Node extends AbstractClosureValuePrimitiveNode implements QuaternaryPrimitive {

        protected PrimClosureValue3Node(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"block.getCompiledBlock().getNumArgs() == 3"})
        protected final Object doValue(final VirtualFrame frame, final BlockClosureObject block, final Object arg1, final Object arg2, final Object arg3) {
            return dispatch.executeBlock(block, getFrameArguments.execute(block, getContextOrMarker(frame), new Object[]{arg1, arg2, arg3}));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 205)
    protected abstract static class PrimClosureValue4Node extends AbstractClosureValuePrimitiveNode implements QuinaryPrimitive {

        protected PrimClosureValue4Node(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"block.getCompiledBlock().getNumArgs() == 4"})
        protected final Object doValue(final VirtualFrame frame, final BlockClosureObject block, final Object arg1, final Object arg2, final Object arg3, final Object arg4) {
            return dispatch.executeBlock(block, getFrameArguments.execute(block, getContextOrMarker(frame), new Object[]{arg1, arg2, arg3, arg4}));
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    @GenerateNodeFactory
    @SqueakPrimitive(indices = 206)
    protected abstract static class PrimClosureValueAryNode extends AbstractClosureValuePrimitiveNode implements BinaryPrimitive {

        protected PrimClosureValueAryNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"block.getCompiledBlock().getNumArgs() == sizeNode.execute(argArray)"}, limit = "1")
        protected final Object doValue(final VirtualFrame frame, final BlockClosureObject block, final ArrayObject argArray,
                        @SuppressWarnings("unused") @Cached("create()") final SqueakObjectSizeNode sizeNode,
                        @Cached("create()") final GetObjectArrayNode getObjectArrayNode) {
            return dispatch.executeBlock(block, getFrameArguments.execute(block, getContextOrMarker(frame), getObjectArrayNode.execute(argArray)));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 221)
    public abstract static class PrimClosureValueNoContextSwitchNode extends AbstractClosureValuePrimitiveNode implements UnaryPrimitive {
        private static final Object[] noArgument = new Object[0];

        protected PrimClosureValueNoContextSwitchNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"block.getCompiledBlock().getNumArgs() == 0"})
        protected final Object doValue(final VirtualFrame frame, final BlockClosureObject block) {
            return code.image.runWithoutInterrupts(() -> dispatch.executeBlock(block,
                            getFrameArguments.execute(block, getContextOrMarker(frame), noArgument)));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 222)
    protected abstract static class PrimClosureValueAryNoContextSwitchNode extends AbstractClosureValuePrimitiveNode implements BinaryPrimitive {

        protected PrimClosureValueAryNoContextSwitchNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"block.getCompiledBlock().getNumArgs() == sizeNode.execute(argArray)"}, limit = "1")
        protected final Object doValue(final VirtualFrame frame, final BlockClosureObject block, final ArrayObject argArray,
                        @SuppressWarnings("unused") @Cached("create()") final SqueakObjectSizeNode sizeNode,
                        @Cached("create()") final GetObjectArrayNode getObjectArrayNode) {
            final Object[] arguments = getObjectArrayNode.execute(argArray);
            return code.image.runWithoutInterrupts(() -> dispatch.executeBlock(block,
                            getFrameArguments.execute(block, getContextOrMarker(frame), arguments)));
        }
    }
}
