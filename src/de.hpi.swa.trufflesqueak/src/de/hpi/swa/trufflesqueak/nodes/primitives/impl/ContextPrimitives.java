/*
 * Copyright (c) 2017-2021 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.FrameMarker;
import de.hpi.swa.trufflesqueak.model.InteropSenderMarker;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.nodes.accessing.ContextObjectNodes.ContextObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.ContextObjectNodes.ContextObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.MiscellaneousBytecodes.CallPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveFallbacks.BinaryPrimitiveFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveFallbacks.TernaryPrimitiveFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveFallbacks.UnaryPrimitiveFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.shared.SqueakLanguageConfig;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public class ContextPrimitives extends AbstractPrimitiveFactoryHolder {

    @ImportStatic(CONTEXT.class)
    @GenerateNodeFactory
    @SqueakPrimitive(indices = 76)
    protected abstract static class PrimStoreStackPointerNode extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {
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
    protected abstract static class PrimFindNextUnwindContextUpToNode extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization(guards = "receiver.hasMaterializedSender()")
        protected static final AbstractSqueakObject doFindNext(final ContextObject receiver, final AbstractSqueakObject previousContextOrNil) {
            ContextObject current = receiver;
            while (current != previousContextOrNil) {
                final Object sender = current.getSender();
                if (sender == NilObject.SINGLETON || sender == previousContextOrNil) {
                    break;
                } else {
                    current = (ContextObject) sender;
                    if (current.getClosure() == null && current.getCodeObject().isUnwindMarked()) {
                        return current;
                    }
                }
            }
            return NilObject.SINGLETON;
        }

        @Specialization(guards = "!receiver.hasMaterializedSender()")
        protected static final AbstractSqueakObject doFindNextAvoidingMaterialization(final ContextObject receiver, final ContextObject previousContext,
                        @CachedContext(SqueakLanguage.class) final ContextReference<SqueakImageContext> ref) {
            // Sender is not materialized, so avoid materialization by walking Truffle frames.
            final boolean[] foundMyself = {false};
            final AbstractSqueakObject result = Truffle.getRuntime().iterateFrames((frameInstance) -> {
                final Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_ONLY);
                if (!FrameAccess.isTruffleSqueakFrame(current)) {
                    return null; // Foreign frame cannot be unwind marked.
                }
                final ContextObject context = FrameAccess.findContext(current);
                if (!foundMyself[0]) {
                    if (receiver == context) {
                        foundMyself[0] = true;
                    }
                } else {
                    if (previousContext == context) {
                        return NilObject.SINGLETON;
                    }
                    if (FrameAccess.getClosure(current) == null && FrameAccess.getCodeObject(current).isUnwindMarked()) {
                        if (context != null) {
                            return context;
                        } else {
                            return ContextObject.create(ref.get(), frameInstance);
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
    protected abstract static class PrimTerminateToNode extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {
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

        private void terminateBetween(final FrameMarker start, final ContextObject end) {
            assert start != null : "Unexpected `null` value";
            final ContextObject[] bottomContextOnTruffleStack = new ContextObject[1];
            final ContextObject result = Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<ContextObject>() {
                boolean foundMyself;

                @Override
                public ContextObject visitFrame(final FrameInstance frameInstance) {
                    final Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_ONLY);
                    if (!FrameAccess.isTruffleSqueakFrame(current)) {
                        return null;
                    }
                    if (!foundMyself) {
                        if (start == FrameAccess.findMarker(current)) {
                            foundMyself = true;
                        }
                    } else {
                        final ContextObject context = FrameAccess.findContext(current);
                        if (context == end) {
                            return end;
                        }
                        bottomContextOnTruffleStack[0] = context;
                        final Frame currentWritable = frameInstance.getFrame(FrameInstance.FrameAccess.READ_WRITE);
                        // Terminate frame
                        FrameAccess.setInstructionPointer(currentWritable, FrameAccess.getMethodOrBlock(current), -1);
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
    protected abstract static class PrimNextHandlerContextNode extends AbstractPrimitiveNode implements UnaryPrimitiveFallback {
        private ContextObject interopExceptionThrowingContextPrototype;

        @TruffleBoundary
        @Specialization(guards = {"receiver.hasMaterializedSender()"})
        protected final AbstractSqueakObject findNext(final ContextObject receiver,
                        @CachedContext(SqueakLanguage.class) final ContextReference<SqueakImageContext> ref) {
            ContextObject context = receiver;
            while (context.hasMaterializedSender()) {
                if (context.getMethodOrBlock().isExceptionHandlerMarked()) {
                    assert context.getClosure() == null;
                    return context;
                }
                final AbstractSqueakObject sender = context.getMaterializedSender();
                if (sender instanceof ContextObject) {
                    context = (ContextObject) sender;
                } else {
                    if (sender == InteropSenderMarker.SINGLETON) {
                        return getInteropExceptionThrowingContext();
                    } else {
                        assert sender == NilObject.SINGLETON;
                        return NilObject.SINGLETON;
                    }
                }
            }
            return findNextAvoidingMaterialization(context, ref);
        }

        @TruffleBoundary
        @Specialization(guards = {"!receiver.hasMaterializedSender()"})
        protected final AbstractSqueakObject findNextAvoidingMaterialization(final ContextObject receiver,
                        @CachedContext(SqueakLanguage.class) final ContextReference<SqueakImageContext> ref) {
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
                final ContextObject context = FrameAccess.findContext(current);
                if (!foundMyself[0]) {
                    if (context == receiver) {
                        foundMyself[0] = true;
                    }
                } else {
                    if (FrameAccess.getCodeObject(current).isExceptionHandlerMarked()) {
                        if (context != null) {
                            return context;
                        } else {
                            return ContextObject.create(ref.get(), frameInstance);
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
                return getInteropExceptionThrowingContext();
            } else if (result == null) {
                if (!foundMyself[0]) {
                    return findNext(receiver, ref); // Fallback to other version.
                }
                if (lastSender[0] instanceof ContextObject) {
                    return findNext((ContextObject) lastSender[0], ref);
                } else {
                    return NilObject.SINGLETON;
                }
            } else {
                return result;
            }
        }

        /**
         * Returns a fake context for BlockClosure>>#on:do: that handles any exception (and may
         * rethrow it as Interop exception). This allows Smalltalk exceptions to be thrown to other
         * languages, so that they can catch them. The mechanism works essentially like this:
         *
         * <pre>
         * <code>[ ... ] on: Exception do: [ :e | "handle e" ]</code>
         * </pre>
         *
         * (see Context>>#handleSignal:)
         */
        private ContextObject getInteropExceptionThrowingContext() {
            if (interopExceptionThrowingContextPrototype == null) {
                final SqueakImageContext image = lookupContext();
                assert image.evaluate("Interop") != NilObject.SINGLETON : "Interop class must be present";
                final CompiledCodeObject onDoMethod = (CompiledCodeObject) image.evaluate("BlockClosure>>#on:do:");
                interopExceptionThrowingContextPrototype = ContextObject.create(image, onDoMethod.getSqueakContextSize());
                interopExceptionThrowingContextPrototype.setCodeObject(onDoMethod);
                interopExceptionThrowingContextPrototype.setReceiver(NilObject.SINGLETON);
                /*
                 * Need to catch all exceptions here. Otherwise, the contexts sender is used to find
                 * the next handler context (see Context>>#nextHandlerContext).
                 */
                interopExceptionThrowingContextPrototype.atTempPut(0, image.evaluate("Exception"));
                /*
                 * Throw Error and Halt as interop, ignore warnings, handle all other exceptions the
                 * usual way via UndefinedObject>>#handleSignal:.
                 */
                interopExceptionThrowingContextPrototype.atTempPut(1, image.evaluate(
                                "[ :e | ((e isKindOf: Error) or: [ e isKindOf: Halt ]) ifTrue: [ Interop throwException: e \"rethrow as interop\" ] ifFalse: [(e isKindOf: Warning) ifTrue: [ e resume \"ignore\" ] " +
                                                "ifFalse: [ nil handleSignal: e \"handle the usual way\" ] ] ]"));
                interopExceptionThrowingContextPrototype.atTempPut(2, BooleanObject.TRUE);
                interopExceptionThrowingContextPrototype.setInstructionPointer(onDoMethod.getInitialPC() + CallPrimitiveNode.NUM_BYTECODES);
                interopExceptionThrowingContextPrototype.setStackPointer(onDoMethod.getNumTemps());
                interopExceptionThrowingContextPrototype.removeSender();
            }
            return interopExceptionThrowingContextPrototype.shallowCopy();
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    @GenerateNodeFactory
    @SqueakPrimitive(indices = 210)
    protected abstract static class PrimContextAtNode extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization(guards = {"index < receiver.getStackSize()"})
        protected static final Object doContextObject(final ContextObject receiver, final long index,
                        @Cached final ContextObjectReadNode readNode) {
            return readNode.execute(receiver, CONTEXT.RECEIVER + index);
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    @GenerateNodeFactory
    @SqueakPrimitive(indices = 211)
    protected abstract static class PrimContextAtPutNode extends AbstractPrimitiveNode implements TernaryPrimitiveFallback {
        @Specialization(guards = "index < receiver.getStackSize()")
        protected static final Object doContextObject(final ContextObject receiver, final long index, final Object value,
                        @Cached final ContextObjectWriteNode writeNode) {
            writeNode.execute(receiver, CONTEXT.RECEIVER + index, value);
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 212)
    protected abstract static class PrimContextSizeNode extends AbstractPrimitiveNode implements UnaryPrimitiveFallback {
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
