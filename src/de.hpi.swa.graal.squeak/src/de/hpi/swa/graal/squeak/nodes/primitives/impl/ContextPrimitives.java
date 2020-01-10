/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.primitives.impl;

import java.util.List;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.FrameMarker;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.CONTEXT;
import de.hpi.swa.graal.squeak.nodes.accessing.ContextObjectNodes.ContextObjectReadNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ContextObjectNodes.ContextObjectWriteNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.graal.squeak.util.FrameAccess;
import de.hpi.swa.graal.squeak.util.FramesAndContextsIterator;

public class ContextPrimitives extends AbstractPrimitiveFactoryHolder {

    @ImportStatic(CONTEXT.class)
    @GenerateNodeFactory
    @SqueakPrimitive(indices = 76)
    protected abstract static class PrimStoreStackPointerNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimStoreStackPointerNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"0 <= newStackPointer", "newStackPointer <= LARGE_FRAMESIZE"})
        protected static final ContextObject store(final ContextObject receiver, final long newStackPointer) {
            /**
             * Not need to "nil any newly accessible cells" as cells are always nil-initialized and
             * their values are cleared (overwritten with nil) on stack pop.
             */
            receiver.setStackPointer((int) newStackPointer);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 195)
    protected abstract static class PrimFindNextUnwindContextUpToNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        private static final FramesAndContextsIterator nextUnwindContextIterator = new FramesAndContextsIterator(
                        (bool, code) -> bool && code instanceof CompiledMethodObject && code.isUnwindMarked(),
                        (context) -> context.getClosure() == null && context.getMethod().isUnwindMarked());

        public PrimFindNextUnwindContextUpToNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "receiver.hasMaterializedSender()")
        protected static final AbstractSqueakObject findNext(final ContextObject receiver, final AbstractSqueakObject previousContextOrNil) {
            return nextUnwindContextIterator.scanFor(receiver, previousContextOrNil, NilObject.SINGLETON);
        }

        @Specialization(guards = "!receiver.hasMaterializedSender()")
        protected static final AbstractSqueakObject findNextAvoidingMaterialization(final ContextObject receiver, final AbstractSqueakObject previousContextOrNil) {
            return nextUnwindContextIterator.scanFor((FrameMarker) receiver.getFrameSender(), previousContextOrNil, NilObject.SINGLETON);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 196)
    protected abstract static class PrimTerminateToNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        private static final FramesAndContextsIterator terminatingIterator = new FramesAndContextsIterator(
                        (frame, code) -> {
                            FrameAccess.setInstructionPointer(frame, code, -1);
                            FrameAccess.setSender(frame, NilObject.SINGLETON);
                        },
                        (context) -> context.terminate());

        public PrimTerminateToNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final ContextObject doUnwindAndTerminate(final ContextObject receiver, final ContextObject previousContext) {
            /*
             * Terminate all the Contexts between me and previousContext, if previousContext is on
             * my Context stack. Make previousContext my sender.
             */
            final Object sender = FrameAccess.getSender(receiver.getOrCreateTruffleFrame());
            if (sender == previousContext || sender == previousContext.getFrameMarker() || sender != NilObject.SINGLETON &&
                            (sender instanceof FrameMarker ? FramesAndContextsIterator.Empty.scanFor((FrameMarker) sender, previousContext, previousContext) == previousContext
                                            : FramesAndContextsIterator.Empty.scanFor((ContextObject) sender, previousContext, previousContext) == previousContext)) {
                terminatingIterator.scanFor(receiver, previousContext, NilObject.SINGLETON);
            }
            receiver.setSender(previousContext);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 197)
    protected abstract static class PrimNextHandlerContextNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        private static final FramesAndContextsIterator nextHandlerContextIterator = new FramesAndContextsIterator(
                        (bool, code) -> bool && code instanceof CompiledMethodObject && ((CompiledMethodObject) code).isExceptionHandlerMarked(),
                        (context) -> context.getClosure() == null && context.getMethod().isExceptionHandlerMarked());

        protected PrimNextHandlerContextNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"receiver.hasMaterializedSender()"})
        protected static final AbstractSqueakObject findNext(final ContextObject receiver) {
            return nextHandlerContextIterator.scanFor(receiver, NilObject.SINGLETON, NilObject.SINGLETON);
        }

        @Specialization(guards = {"!receiver.hasMaterializedSender()"})
        protected static final AbstractSqueakObject findNextAvoidingMaterialization(final ContextObject receiver) {
            return nextHandlerContextIterator.scanFor((FrameMarker) receiver.getFrameSender(), NilObject.SINGLETON, NilObject.SINGLETON);
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    @ImportStatic(FrameAccess.class)
    @GenerateNodeFactory
    @SqueakPrimitive(indices = 210)
    protected abstract static class PrimContextAtNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimContextAtNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"index < receiver.getStackSize()"})
        protected static final Object doContextObject(final ContextObject receiver, final long index,
                        @Cached final ContextObjectReadNode readNode) {
            return readNode.execute(receiver, CONTEXT.RECEIVER + index);
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    @GenerateNodeFactory
    @SqueakPrimitive(indices = 211)
    protected abstract static class PrimContextAtPutNode extends AbstractPrimitiveNode implements TernaryPrimitive {

        protected PrimContextAtPutNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "index < receiver.getStackSize()")
        protected static final Object doContextObject(final ContextObject receiver, final long index, final Object value,
                        @Cached final ContextObjectWriteNode writeNode) {
            writeNode.execute(receiver, CONTEXT.RECEIVER + index, value);
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 212)
    protected abstract static class PrimContextSizeNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimContextSizeNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "receiver.hasTruffleFrame()")
        protected static final long doSize(final ContextObject receiver) {
            return receiver.getStackPointer();
        }

        @Specialization(guards = "!receiver.hasTruffleFrame()")
        protected static final long doSizeWithoutFrame(final ContextObject receiver) {
            return receiver.size() - receiver.instsize();
        }
    }

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return ContextPrimitivesFactory.getFactories();
    }
}
