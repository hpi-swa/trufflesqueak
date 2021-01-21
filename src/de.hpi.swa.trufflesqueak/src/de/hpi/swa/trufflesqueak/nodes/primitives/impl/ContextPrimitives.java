/*
 * Copyright (c) 2017-2021 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
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
        @Specialization
        protected static final AbstractSqueakObject doFindNext(final ContextObject receiver, final AbstractSqueakObject previousContextOrNil) {
            ContextObject current = receiver;
            while (current != previousContextOrNil) {
                final Object sender = current.getSender();
                if (sender == NilObject.SINGLETON || sender == previousContextOrNil) {
                    break;
                } else {
                    current = (ContextObject) sender;
                    if (current.getCodeObject().isUnwindMarked()) {
                        assert current.getClosure() == null;
                        return current;
                    }
                }
            }
            return NilObject.SINGLETON;
        }
    }

// @GenerateNodeFactory
// @SqueakPrimitive(indices = 196)
    protected abstract static class PrimTerminateToNode extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization
        protected static final ContextObject doUnwindAndTerminate(final ContextObject receiver, final ContextObject previousContext) {
            /*
             * Terminate all the Contexts between me and previousContext, if previousContext is on
             * my Context stack. Make previousContext my sender.
             */
            if (receiver.hasSender(previousContext)) {
                ContextObject current = receiver;
                while (current != previousContext) {
                    final ContextObject sendingContext = (ContextObject) current.getSender();
                    current.terminate();
                    current = sendingContext;
                }
            }
            receiver.setSender(previousContext);
            return receiver;
        }

        @Specialization
        protected static final ContextObject doTerminate(final ContextObject receiver, @SuppressWarnings("unused") final NilObject nil) {
            receiver.removeSender();
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 197)
    protected abstract static class PrimNextHandlerContextNode extends AbstractPrimitiveNode implements UnaryPrimitiveFallback {
        @CompilationFinal private ContextObject interopExceptionThrowingContextPrototype;

        @Specialization
        protected final AbstractSqueakObject findNext(final ContextObject receiver) {
            ContextObject context = receiver;
            while (true) {
                if (context.getMethodOrBlock() != null && context.getMethodOrBlock().isExceptionHandlerMarked()) {
                    assert context.getClosure() == null;
                    return context;
                }
                final AbstractSqueakObject sender = context.getSender();
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
                CompilerDirectives.transferToInterpreterAndInvalidate();
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
