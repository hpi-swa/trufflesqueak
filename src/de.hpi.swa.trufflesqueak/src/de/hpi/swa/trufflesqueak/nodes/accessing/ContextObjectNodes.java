/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.accessing;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;

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

        public abstract Object execute(Node node, ContextObject context, long index);

        // --- Sender ---
        @Specialization(guards = "index == SENDER_OR_NIL")
        protected static final Object doSender(final ContextObject context, @SuppressWarnings("unused") final long index) {
            return context.getSender();
        }

        // --- Instruction Pointer ---
        @Specialization(guards = {"index == INSTRUCTION_POINTER", "context.hasTruffleFrame()"})
        protected static final Object doInstructionPointer(final Node node, final ContextObject context, @SuppressWarnings("unused") final long index,
                        @Exclusive @Cached final InlinedConditionProfile nilProfile) {
            return context.getInstructionPointer(nilProfile, node);
        }

        @Specialization(guards = {"index == INSTRUCTION_POINTER", "context.hasContextProxy()"})
        protected static final Object doProxyInstructionPointer(final ContextObject context, @SuppressWarnings("unused") final long index) {
            return context.getProxyInstructionPointer();
        }

        // --- Stack Pointer ---
        @Specialization(guards = {"index == STACKPOINTER", "context.hasTruffleFrame()"})
        protected static final long doStackPointer(final ContextObject context, @SuppressWarnings("unused") final long index) {
            return context.getStackPointer();
        }

        @Specialization(guards = {"index == STACKPOINTER", "context.hasContextProxy()"})
        protected static final Object doProxyStackPointer(final ContextObject context, @SuppressWarnings("unused") final long index) {
            return context.getProxyStackPointer();
        }

        // --- Method ---
        @Specialization(guards = {"index == METHOD", "context.hasTruffleFrame()"})
        protected static final CompiledCodeObject doMethod(final ContextObject context, @SuppressWarnings("unused") final long index) {
            return context.getCodeObject();
        }

        @Specialization(guards = {"index == METHOD", "context.hasContextProxy()"})
        protected static final Object doProxyMethod(final ContextObject context, @SuppressWarnings("unused") final long index) {
            return context.getProxyMethod();
        }

        // --- Closure ---
        @Specialization(guards = {"index == CLOSURE_OR_NIL", "context.hasTruffleFrame()"})
        protected static final Object doClosure(final Node node, final ContextObject context, @SuppressWarnings("unused") final long index,
                        @Exclusive @Cached final InlinedConditionProfile nilProfile) {
            return NilObject.nullToNil(context.getClosure(), nilProfile, node);
        }

        @Specialization(guards = {"index == CLOSURE_OR_NIL", "context.hasContextProxy()"})
        protected static final Object doProxyClosure(final ContextObject context, @SuppressWarnings("unused") final long index) {
            return context.getProxyClosureOrNil();
        }

        // --- Receiver ---
        @Specialization(guards = {"index == RECEIVER", "context.hasTruffleFrame()"})
        protected static final Object doReceiver(final ContextObject context, @SuppressWarnings("unused") final long index) {
            return context.getReceiver();
        }

        @Specialization(guards = {"index == RECEIVER", "context.hasContextProxy()"})
        protected static final Object doProxyReceiver(final ContextObject context, @SuppressWarnings("unused") final long index) {
            return context.getProxyReceiver();
        }

        // --- Temp / Stack ---
        @Specialization(guards = {"index >= TEMP_FRAME_START", "context.hasTruffleFrame()", "isValidTempIndex(context, index)"})
        protected static final Object doTemp(final ContextObject context, final long index) {
            return context.atTemp((int) (index - CONTEXT.TEMP_FRAME_START));
        }

        @Specialization(guards = {"index >= TEMP_FRAME_START", "context.hasContextProxy()", "isValidTempIndex(context, index)"})
        protected static final Object doProxyTemp(final ContextObject context, final long index) {
            return context.getProxyTemp((int) (index - CONTEXT.TEMP_FRAME_START));
        }

        @Specialization(guards = {"index >= TEMP_FRAME_START", "!isValidTempIndex(context, index)"})
        protected static final Object doTempOutOfBounds(@SuppressWarnings("unused") final ContextObject context, @SuppressWarnings("unused") final long index) {
            return NilObject.SINGLETON;
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

        public abstract void execute(Node node, ContextObject context, long index, Object value);

        // --- Sender ---
        @Specialization(guards = {"index == SENDER_OR_NIL", "context.hasTruffleFrame()"})
        protected static final void doSenderFrame(final ContextObject context, @SuppressWarnings("unused") final long index, final AbstractSqueakObject value) {
            context.setSender(value); // Note: NilObject is an AbstractSqueakObject, so this catches both!
        }

        @Specialization(guards = {"index == SENDER_OR_NIL", "context.hasTruffleFrame()", "!isAbstractSqueakObject(value)"})
        protected static final void doSenderDematerialize(final ContextObject context, @SuppressWarnings("unused") final long index, final Object value) {
            context.dematerializeToProxy();
            context.setProxySender(value);
        }

        @Specialization(guards = {"index == SENDER_OR_NIL", "context.hasContextProxy()"})
        protected static final void doProxySender(final ContextObject context, @SuppressWarnings("unused") final long index, final Object value) {
            context.setProxySender(value);
        }

        // --- Instruction Pointer ---
        @Specialization(guards = {"index == INSTRUCTION_POINTER", "context.hasTruffleFrame()"})
        protected static final void doInstructionPointerFrame(final ContextObject context, @SuppressWarnings("unused") final long index, final long value) {
            context.setInstructionPointer((int) value - context.getCodeObject().getInitialPC());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"index == INSTRUCTION_POINTER", "context.hasTruffleFrame()"})
        protected static final void doInstructionPointerTerminated(final ContextObject context, final long index, final NilObject value) {
            context.removeInstructionPointer();
        }

        @Specialization(guards = {"index == INSTRUCTION_POINTER", "context.hasTruffleFrame()", "!isLong(value)", "!isNil(value)"})
        protected static final void doInstructionPointerDematerialize(final ContextObject context, @SuppressWarnings("unused") final long index, final Object value) {
            context.dematerializeToProxy();
            context.setProxyInstructionPointer(value);
        }

        @Specialization(guards = {"index == INSTRUCTION_POINTER", "context.hasContextProxy()"})
        protected static final void doProxyInstructionPointer(final ContextObject context, @SuppressWarnings("unused") final long index, final Object value) {
            context.setProxyInstructionPointer(value);
        }

        // --- Stack Pointer ---
        @Specialization(guards = {"index == STACKPOINTER", "context.hasTruffleFrame()", "isValidStackPointer(context, value)"})
        protected static final void doStackPointerFrame(final ContextObject context, @SuppressWarnings("unused") final long index, final long value) {
            context.setStackPointer((int) value);
        }

        @Specialization(guards = {"index == STACKPOINTER", "context.hasTruffleFrame()", "!isValidStackPointer(context, value)"})
        protected static final void doStackPointerDematerialize(final ContextObject context, @SuppressWarnings("unused") final long index, final Object value) {
            context.dematerializeToProxy();
            context.setProxyStackPointer(value);
        }

        @Specialization(guards = {"index == STACKPOINTER", "context.hasContextProxy()"})
        protected static final void doProxyStackPointer(final ContextObject context, @SuppressWarnings("unused") final long index, final Object value) {
            context.setProxyStackPointer(value);
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
        protected static final void doMethodDematerialize(final ContextObject context, @SuppressWarnings("unused") final long index, final Object value) {
            context.dematerializeToProxy();
            context.setProxyMethod(value);
        }

        @Specialization(guards = {"index == METHOD", "context.hasContextProxy()"})
        protected static final void doProxyMethod(final ContextObject context, @SuppressWarnings("unused") final long index, final Object value) {
            context.setProxyMethod(value);
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
        protected static final void doClosureDematerialize(final ContextObject context, @SuppressWarnings("unused") final long index, final Object value) {
            context.dematerializeToProxy();
            context.setProxyClosureOrNil(value);
        }

        @Specialization(guards = {"index == CLOSURE_OR_NIL", "context.hasContextProxy()"})
        protected static final void doProxyClosure(final ContextObject context, @SuppressWarnings("unused") final long index, final Object value) {
            context.setProxyClosureOrNil(value);
        }

        // --- Receiver & Temps (Naturally polymorphic arrays, no dematerialization needed) ---
        @Specialization(guards = {"index == RECEIVER", "context.hasTruffleFrame()"})
        protected static final void doReceiverFrame(final ContextObject context, @SuppressWarnings("unused") final long index, final Object value) {
            context.setReceiver(value);
        }

        @Specialization(guards = {"index == RECEIVER", "context.hasContextProxy()"})
        protected static final void doProxyReceiver(final ContextObject context, @SuppressWarnings("unused") final long index, final Object value) {
            context.setProxyReceiver(value);
        }

        // --- Temps ---
        @Specialization(guards = {"index >= TEMP_FRAME_START", "context.hasTruffleFrame()", "isValidTempIndex(context, index)"})
        protected static final void doTempFrame(final ContextObject context, final long index, final Object value) {
            context.atTempPut((int) (index - CONTEXT.TEMP_FRAME_START), value);
        }

        @Specialization(guards = {"index >= TEMP_FRAME_START", "context.hasContextProxy()", "isValidTempIndex(context, index)"})
        protected static final void doProxyTemp(final ContextObject context, final long index, final Object value) {
            context.setProxyTemp((int) (index - CONTEXT.TEMP_FRAME_START), value);
        }

        @Specialization(guards = {"index >= TEMP_FRAME_START", "!isValidTempIndex(context, index)"})
        protected static final void doTempOutOfBounds(@SuppressWarnings("unused") final ContextObject context, @SuppressWarnings("unused") final long index,
                        @SuppressWarnings("unused") final Object value) {
            /*
             * No-op. We safely ignore out-of-bounds writes to dead space.
             * This prevents crashing on negative indices.
             */
        }

        protected static boolean isValidTempIndex(final ContextObject context, final long index) {
            return context.isValidTempIndex((int) (index - CONTEXT.TEMP_FRAME_START));
        }
    }
}
