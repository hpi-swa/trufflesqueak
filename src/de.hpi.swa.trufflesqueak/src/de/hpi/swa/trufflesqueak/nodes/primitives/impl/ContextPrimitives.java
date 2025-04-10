/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.FrameMarker;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.nodes.accessing.ContextObjectNodes.ContextObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.ContextObjectNodes.ContextObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive2WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.shared.SqueakLanguageConfig;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

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
        @Specialization(guards = "receiver.hasMaterializedSender()")
        protected static final AbstractSqueakObject doFindNext(final ContextObject receiver, final AbstractSqueakObject previousContextOrNil) {
            ContextObject current = receiver;
            while (current != previousContextOrNil) {
                final Object sender = current.getSender();
                if (sender == NilObject.SINGLETON || sender == previousContextOrNil) {
                    break;
                } else {
                    current = (ContextObject) sender;
                    if (!current.hasClosure() && current.getCodeObject().isUnwindMarked()) {
                        return current;
                    }
                }
            }
            return NilObject.SINGLETON;
        }

        @TruffleBoundary
        @Specialization(guards = "!receiver.hasMaterializedSender()")
        protected final AbstractSqueakObject doFindNextAvoidingMaterialization(final ContextObject receiver, final ContextObject previousContext) {
            // Sender is not materialized, so avoid materialization by walking Truffle frames.
            final boolean[] foundMyself = {false};
            final AbstractSqueakObject result = Truffle.getRuntime().iterateFrames((frameInstance) -> {
                final Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_ONLY);
                if (!FrameAccess.isTruffleSqueakFrame(current)) {
                    return null; // Foreign frame cannot be unwind marked.
                }
                final ContextObject context = FrameAccess.getContext(current);
                if (!foundMyself[0]) {
                    if (receiver == context) {
                        foundMyself[0] = true;
                    }
                } else {
                    if (previousContext == context) {
                        return NilObject.SINGLETON;
                    }
                    if (!FrameAccess.hasClosure(current) && FrameAccess.getCodeObject(current).isUnwindMarked()) {
                        if (context != null) {
                            return context;
                        } else {
                            return ContextObject.create(getContext(), frameInstance);
                        }
                    }
                }
                return null;
            });
            assert foundMyself[0] : "Did not find receiver with virtual sender on Truffle stack";
            return NilObject.nullToNil(result);
        }

        @Specialization(guards = "!receiver.hasMaterializedSender()")
        protected static final AbstractSqueakObject doFindNextAvoidingMaterializationNil(final ContextObject receiver, @SuppressWarnings("unused") final NilObject nil) {
            return doFindNext(receiver, nil);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 196)
    protected abstract static class PrimTerminateToNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected final ContextObject doUnwindAndTerminate(final ContextObject receiver, final ContextObject previousContext) {
            /*
             * Terminate all the Contexts between me and previousContext, if previousContext is on
             * my Context stack. Make previousContext my sender.
             */
            terminateBetween(receiver, previousContext);
            receiver.setSender(previousContext);
            return receiver;
        }

        @Specialization
        protected static final ContextObject doTerminate(final ContextObject receiver, @SuppressWarnings("unused") final NilObject nil) {
            receiver.removeSender();
            return receiver;
        }

        private void terminateBetween(final ContextObject start, final ContextObject end) {
            ContextObject current = start;
            while (current.hasMaterializedSender()) {
                final AbstractSqueakObject sender = current.getSender();
                if (current != start) {
                    current.terminate();
                }
                if (sender == NilObject.SINGLETON || sender == end) {
                    return;
                } else {
                    current = (ContextObject) sender;
                }
            }
            terminateBetween((FrameMarker) current.getFrameSender(), end);
        }

        @TruffleBoundary
        private void terminateBetween(final FrameMarker start, final ContextObject end) {
            assert start != null : "Unexpected `null` value";
            final ContextObject[] bottomContextOnTruffleStack = new ContextObject[1];
            final ContextObject result = Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<>() {
                boolean foundMyself;

                @Override
                public ContextObject visitFrame(final FrameInstance frameInstance) {
                    final Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_ONLY);
                    if (!FrameAccess.isTruffleSqueakFrame(current)) {
                        return null;
                    }
                    if (!foundMyself) {
                        if (start == FrameAccess.getMarker(current)) {
                            foundMyself = true;
                        }
                    } else {
                        final ContextObject context = FrameAccess.getContext(current);
                        if (context == end) {
                            return end;
                        }
                        bottomContextOnTruffleStack[0] = context;
                        final Frame currentWritable = frameInstance.getFrame(FrameInstance.FrameAccess.READ_WRITE);
                        // Terminate frame
                        FrameAccess.setInstructionPointer(currentWritable, -1);
                        FrameAccess.setSender(currentWritable, NilObject.SINGLETON);
                    }
                    return null;
                }
            });
            if (result == null && bottomContextOnTruffleStack[0] != null) {
                terminateBetweenRecursively(bottomContextOnTruffleStack[0], end);
            }
        }

        @TruffleBoundary
        private void terminateBetweenRecursively(final ContextObject start, final ContextObject end) {
            terminateBetween(start, end);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 197)
    protected abstract static class PrimNextHandlerContextNode extends AbstractPrimitiveNode implements Primitive0WithFallback {
        @TruffleBoundary
        @Specialization(guards = {"receiver.hasMaterializedSender()"})
        protected final AbstractSqueakObject findNext(final ContextObject receiver) {
            ContextObject context = receiver;
            while (context.hasMaterializedSender()) {
                if (context.getCodeObject().isExceptionHandlerMarked()) {
                    assert !context.hasClosure();
                    return context;
                }
                final AbstractSqueakObject sender = context.getMaterializedSender();
                if (sender instanceof final ContextObject o) {
                    context = o;
                } else {
                    assert sender == NilObject.SINGLETON;
                    return NilObject.SINGLETON;
                }
            }
            return findNextAvoidingMaterialization(context);
        }

        @TruffleBoundary
        @Specialization(guards = {"!receiver.hasMaterializedSender()"})
        protected final AbstractSqueakObject findNextAvoidingMaterialization(final ContextObject receiver) {
            final boolean[] foundMyself = new boolean[1];
            final Object[] lastSender = new Object[1];
            final ContextObject result = Truffle.getRuntime().iterateFrames(frameInstance -> {
                final Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_ONLY);
                if (!FrameAccess.isTruffleSqueakFrame(current)) {
                    final RootNode rootNode = ((RootCallTarget) frameInstance.getCallTarget()).getRootNode();
                    if (rootNode.isInternal() || rootNode.getLanguageInfo().getId().equals(SqueakLanguageConfig.ID)) {
                        /* Skip internal and all other nodes that belong to TruffleSqueak. */
                        return null;
                    } else {
                        /*
                         * Found a frame of another language. Stop here by returning the receiver
                         * context. This special case will be handled later on.
                         */
                        return receiver;
                    }
                }
                final ContextObject context = FrameAccess.getContext(current);
                if (!foundMyself[0]) {
                    if (context == receiver) {
                        foundMyself[0] = true;
                    }
                } else {
                    if (FrameAccess.getCodeObject(current).isExceptionHandlerMarked()) {
                        if (context != null) {
                            return context;
                        } else {
                            return ContextObject.create(getContext(), frameInstance);
                        }
                    } else {
                        lastSender[0] = FrameAccess.getSender(current);
                    }
                }
                return null;
            });
            if (result == receiver) {
                /*
                 * Foreign frame found during frame iteration. Inject a fake context which will
                 * throw the Smalltalk exception as polyglot exception.
                 */
                return getContext().getInteropExceptionThrowingContext();
            } else if (result == null) {
                if (!foundMyself[0]) {
                    return findNext(receiver); // Fallback to other version.
                }
                if (lastSender[0] instanceof final ContextObject o) {
                    return findNext(o);
                } else {
                    return NilObject.SINGLETON;
                }
            } else {
                return result;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 210)
    protected abstract static class PrimContextAtNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"index <= receiver.getStackSize()"})
        protected static final Object doContextObject(final ContextObject receiver, final long index,
                        @Bind final Node node,
                        @Cached final ContextObjectReadNode readNode) {
            return readNode.execute(node, receiver, CONTEXT.RECEIVER + index);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 211)
    protected abstract static class PrimContextAtPutNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = "index <= receiver.getStackSize()")
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
