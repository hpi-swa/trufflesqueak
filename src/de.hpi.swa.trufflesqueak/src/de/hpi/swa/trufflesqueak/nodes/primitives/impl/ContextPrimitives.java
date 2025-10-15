/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.util.List;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.nodes.accessing.ContextObjectNodes.ContextObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.ContextObjectNodes.ContextObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive2WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;

public class ContextPrimitives extends AbstractPrimitiveFactoryHolder {

    @ImportStatic(CONTEXT.class)
    @GenerateNodeFactory
    @SqueakPrimitive(indices = 76)
    protected abstract static class PrimStoreStackPointerNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"0 <= newStackPointer", "newStackPointer <= LARGE_FRAMESIZE"})
        protected static final ContextObject store(final ContextObject receiver, final long newStackPointer) {
            /*
             * Not need to "nil any newly accessible cells" as cells are always nil-initialized and
             * their values are cleared (overwritten with nil) on stack pop.
             */
            receiver.setStackPointer((int) newStackPointer);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 195)
    protected abstract static class PrimFindNextUnwindContextUpToNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final AbstractSqueakObject doFindNext(final ContextObject receiver, final AbstractSqueakObject previousContextOrNil) {
            // Search starts with sender.
            if (receiver == previousContextOrNil) {
                return NilObject.SINGLETON;
            }
            AbstractSqueakObject currentLink = receiver.getFrameSender();

            while (currentLink instanceof final ContextObject context) {
                // Exit if we've found the previous Context.
                if (context == previousContextOrNil) {
                    return NilObject.SINGLETON;
                }
                // Watch for unwind-marked ContextObjects.
                if (context.isUnwindMarked()) {
                    return context;
                }
                // Move to the next link.
                currentLink = context.getFrameSender();
            }
            // Reached the end of the chain without finding previous Context.
            return NilObject.SINGLETON;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 196)
    protected abstract static class PrimTerminateToNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final ContextObject doUnwindAndTerminate(final ContextObject receiver, final ContextObject previousContext) {
            /*
             * Terminate all the Contexts between me and previousContext, if previousContext is on
             * my Context stack. Make previousContext my sender.
             */
            if (hasSenderChainFromToAndTerminateIf(receiver, previousContext, false)) {
                hasSenderChainFromToAndTerminateIf(receiver, previousContext, true);
            }
            receiver.setSender(previousContext);
            return receiver;
        }

        @Specialization
        protected static final ContextObject doTerminate(final ContextObject receiver, @SuppressWarnings("unused") final NilObject nil) {
            receiver.removeSender();
            return receiver;
        }

        /**
         * Returns true if endContext is found on the sender chain of startContext. If terminate is
         * true, terminate Contexts while following the sender chain.
         */
        public static boolean hasSenderChainFromToAndTerminateIf(final ContextObject startContext, final AbstractSqueakObject endContext, final boolean terminate) {
            // Search starts with sender.
            AbstractSqueakObject currentLink = startContext.getFrameSender();

            while (currentLink instanceof final ContextObject context) {
                // Exit if we've found endContext.
                if (context == endContext) {
                    return true;
                }
                // Move to the next link.
                currentLink = context.getFrameSender();
                // Terminate if requested.
                if (terminate && context.hasTruffleFrame()) {
                    context.terminate();
                }
            }
            // Reached the end of the chain without finding endContext.
            return false;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 197)
    protected abstract static class PrimNextHandlerContextNode extends AbstractPrimitiveNode implements Primitive0WithFallback {
        @Specialization
        protected static final AbstractSqueakObject doNextHandler(final ContextObject receiver) {
            // Search starts with receiver.
            if (receiver.isExceptionHandlerMarked()) {
                return receiver;
            }
            AbstractSqueakObject currentLink = receiver.getFrameSender();

            while (currentLink instanceof final ContextObject context) {
                // Watch for exception handler marked ContextObjects.
                if (context.isExceptionHandlerMarked()) {
                    return context;
                }
                // Move to the next link.
                currentLink = context.getFrameSender();
            }

            // Reached the end of the chain without finding an exception handler Context.
            return NilObject.SINGLETON;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 210)
    protected abstract static class PrimContextAtNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"index <= receiver.size()"})
        protected static final Object doContextObject(final ContextObject receiver, final long index,
                        @Bind final Node node,
                        @Cached final ContextObjectReadNode readNode) {
            return readNode.execute(node, receiver, CONTEXT.RECEIVER + index);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 211)
    protected abstract static class PrimContextAtPutNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = "index <= receiver.size()")
        protected static final Object doContextObject(final ContextObject receiver, final long index, final Object value,
                        @Bind final Node node,
                        @Cached final ContextObjectWriteNode writeNode) {
            writeNode.execute(node, receiver, CONTEXT.RECEIVER + index, value);
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 212)
    protected abstract static class PrimContextSizeNode extends AbstractPrimitiveNode implements Primitive0WithFallback {
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
