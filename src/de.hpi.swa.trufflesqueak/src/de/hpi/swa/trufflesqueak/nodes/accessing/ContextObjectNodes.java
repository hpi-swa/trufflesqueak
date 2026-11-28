/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.accessing;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.SqueakGuards;

public final class ContextObjectNodes {
    @GenerateInline
    @GenerateUncached
    @GenerateCached(false)
    @ImportStatic(CONTEXT.class)
    public abstract static class ContextObjectReadNode extends AbstractNode {

        public final Object execute(final Node node, final ContextObject context, final long index) {
            /* Bare-sender contexts are always materialized before reaching node dispatch. */
            assert context.hasTruffleFrame() || context.hasNonExecutableState();
            return executeSpecialized(node, context, index);
        }

        protected abstract Object executeSpecialized(Node node, ContextObject context, long index);

        // --- Sender ---
        @Specialization(guards = {"index == SENDER_OR_NIL", "!context.hasNonExecutableState()"})
        protected static final Object doSender(final ContextObject context, @SuppressWarnings("unused") final long index) {
            return context.getSender();
        }

        // --- Instruction Pointer ---
        @Specialization(guards = {"index == INSTRUCTION_POINTER", "context.hasTruffleFrame()"})
        protected static final Object doInstructionPointer(final Node node, final ContextObject context, @SuppressWarnings("unused") final long index,
                        @Exclusive @Cached final InlinedConditionProfile nilProfile) {
            return context.getInstructionPointer(nilProfile, node);
        }

        // --- Stack Pointer ---
        @Specialization(guards = {"index == STACKPOINTER", "context.hasTruffleFrame()"})
        protected static final long doStackPointer(final ContextObject context, @SuppressWarnings("unused") final long index) {
            return context.getStackPointer();
        }

        // --- Method ---
        @Specialization(guards = {"index == METHOD", "context.hasTruffleFrame()"})
        protected static final CompiledCodeObject doMethod(final ContextObject context, @SuppressWarnings("unused") final long index) {
            return context.getCodeObject();
        }

        // --- Closure ---
        @Specialization(guards = {"index == CLOSURE_OR_NIL", "context.hasTruffleFrame()"})
        protected static final Object doClosure(final Node node, final ContextObject context, @SuppressWarnings("unused") final long index,
                        @Exclusive @Cached final InlinedConditionProfile nilProfile) {
            return NilObject.nullToNil(context.getClosure(), nilProfile, node);
        }

        // --- Receiver ---
        @Specialization(guards = {"index == RECEIVER", "context.hasTruffleFrame()"})
        protected static final Object doReceiver(final ContextObject context, @SuppressWarnings("unused") final long index) {
            return context.getReceiver();
        }

        // --- Temp / Stack ---
        @Specialization(guards = {"index >= TEMP_FRAME_START", "context.hasTruffleFrame()", "isValidTempIndex(context, index)"})
        protected static final Object doTemp(final ContextObject context, final long index) {
            return context.atTemp((int) (index - CONTEXT.TEMP_FRAME_START));
        }

        @Specialization(guards = {"index >= TEMP_FRAME_START", "context.hasTruffleFrame()", "!isValidTempIndex(context, index)"})
        protected static final Object doTempOutOfBounds(@SuppressWarnings("unused") final ContextObject context, @SuppressWarnings("unused") final long index) {
            return NilObject.SINGLETON;
        }

        /* Slow path: all slots of a context that cannot be executed as it stands. */
        @Specialization(guards = "context.hasNonExecutableState()")
        protected static final Object doNonExecutable(final ContextObject context, final long index) {
            return context.rawAt(index);
        }

        protected static boolean isValidTempIndex(final ContextObject context, final long index) {
            return context.isValidTempIndex((int) (index - CONTEXT.TEMP_FRAME_START));
        }
    }

    @GenerateInline
    @GenerateUncached
    @GenerateCached(false)
    @ImportStatic({CONTEXT.class, SqueakGuards.class})
    public abstract static class ContextObjectWriteNode extends AbstractNode {

        public final void execute(final Node node, final ContextObject context, final long index, final Object value) {
            /* Bare-sender contexts are always materialized before reaching node dispatch. */
            assert context.hasTruffleFrame() || context.hasNonExecutableState();
            executeSpecialized(node, context, index, value);
        }

        protected abstract void executeSpecialized(Node node, ContextObject context, long index, Object value);

        // --- Sender ---
        @Specialization(guards = {"index == SENDER_OR_NIL", "context.hasTruffleFrame()"})
        protected static final void doSenderFrame(final ContextObject context, @SuppressWarnings("unused") final long index, final Object value) {
            /* A frame can hold any object as sender; returning to a non-context ends in cannotReturn:. */
            context.setSender(value);
        }

        // --- Instruction Pointer ---
        @Specialization(guards = {"index == INSTRUCTION_POINTER", "context.hasTruffleFrame()", "context.isValidInstructionPointer(value)"})
        protected static final void doInstructionPointerFrame(final ContextObject context, @SuppressWarnings("unused") final long index, final long value) {
            context.setInstructionPointer((int) (value - context.getCodeObject().getInitialPC()));
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"index == INSTRUCTION_POINTER", "context.hasTruffleFrame()"})
        protected static final void doInstructionPointerTerminated(final ContextObject context, final long index, final NilObject value) {
            context.removeInstructionPointer();
        }

        @Specialization(guards = {"index == INSTRUCTION_POINTER", "context.hasTruffleFrame()", "!isValidInstructionPointerOrNil(context, value)"})
        protected static final void doInstructionPointerUnbind(final ContextObject context, @SuppressWarnings("unused") final long index, final Object value) {
            context.unbindFromFrame();
            context.rawAtPut(CONTEXT.INSTRUCTION_POINTER, value);
        }

        protected static boolean isValidInstructionPointerOrNil(final ContextObject context, final Object value) {
            return value == NilObject.SINGLETON || (value instanceof Long l && context.isValidInstructionPointer(l));
        }

        // --- Stack Pointer ---
        @Specialization(guards = {"index == STACKPOINTER", "context.hasTruffleFrame()", "isValidStackPointer(context, value)"})
        protected static final void doStackPointerFrame(final ContextObject context, @SuppressWarnings("unused") final long index, final long value) {
            context.setStackPointer((int) value);
        }

        @Specialization(guards = {"index == STACKPOINTER", "context.hasTruffleFrame()", "!isValidStackPointer(context, value)"})
        protected static final void doInvalidStackPointer(@SuppressWarnings("unused") final ContextObject context, @SuppressWarnings("unused") final long index,
                        @SuppressWarnings("unused") final Object value) {
            CompilerDirectives.transferToInterpreter();
            throw PrimitiveFailed.GENERIC_ERROR;
        }

        protected static boolean isValidStackPointer(final ContextObject context, final Object value) {
            return value instanceof Long l && context.isValidStackPointer(l);
        }

        // --- Method ---
        @Specialization(guards = {"index == METHOD", "context.hasTruffleFrame()"})
        protected static final void doMethodFrame(final ContextObject context, @SuppressWarnings("unused") final long index, final CompiledCodeObject value) {
            context.overwriteCodeObject(value);
        }

        @Specialization(guards = {"index == METHOD", "context.hasTruffleFrame()", "!isCompiledCodeObject(value)"})
        protected static final void doMethodUnbind(final ContextObject context, @SuppressWarnings("unused") final long index, final Object value) {
            context.unbindFromFrame();
            context.rawAtPut(CONTEXT.METHOD, value);
        }

        // --- Closure ---
        @Specialization(guards = {"index == CLOSURE_OR_NIL", "context.hasTruffleFrame()"})
        protected static final void doClosureFrame(final ContextObject context, @SuppressWarnings("unused") final long index, final BlockClosureObject value) {
            context.setClosure(value);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"index == CLOSURE_OR_NIL", "context.hasTruffleFrame()"})
        protected static final void doClosureTerminated(final ContextObject context, final long index, final NilObject value) {
            context.removeClosure();
        }

        @Specialization(guards = {"index == CLOSURE_OR_NIL", "context.hasTruffleFrame()", "!isBlockClosureObject(value)", "!isNil(value)"})
        protected static final void doClosureUnbind(final ContextObject context, @SuppressWarnings("unused") final long index, final Object value) {
            context.unbindFromFrame();
            context.rawAtPut(CONTEXT.CLOSURE_OR_NIL, value);
        }

        // --- Receiver & Temps (any value fits into a frame) ---
        @Specialization(guards = {"index == RECEIVER", "context.hasTruffleFrame()"})
        protected static final void doReceiverFrame(final ContextObject context, @SuppressWarnings("unused") final long index, final Object value) {
            context.setReceiver(value);
        }

        // --- Temps ---
        @Specialization(guards = {"index >= TEMP_FRAME_START", "context.hasTruffleFrame()", "isValidTempIndex(context, index)"})
        protected static final void doTempFrame(final ContextObject context, final long index, final Object value) {
            context.atTempPut((int) (index - CONTEXT.TEMP_FRAME_START), value);
        }

        @Specialization(guards = {"index >= TEMP_FRAME_START", "context.hasTruffleFrame()", "!isValidTempIndex(context, index)"})
        protected static final void doTempOutOfBounds(@SuppressWarnings("unused") final ContextObject context, @SuppressWarnings("unused") final long index,
                        @SuppressWarnings("unused") final Object value) {
            /*
             * Ignore out-of-bounds writes. For materialized frame contexts, valid temp indices are
             * bounded by getNumStackSlots(frame) rather than size(), so writes to [numStackSlots,
             * size) are silently discarded and subsequent reads return nil.
             */
        }

        /* Slow path: all slots of a context that cannot be executed as it stands. */
        @Specialization(guards = "context.hasNonExecutableState()")
        protected static final void doNonExecutable(final ContextObject context, final long index, final Object value) {
            context.rawAtPut(index, value);
        }

        protected static boolean isValidTempIndex(final ContextObject context, final long index) {
            return context.isValidTempIndex((int) (index - CONTEXT.TEMP_FRAME_START));
        }
    }
}
