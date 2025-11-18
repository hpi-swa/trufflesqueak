/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
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

import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;

public final class ContextObjectNodes {
    @GenerateInline
    @GenerateUncached
    @GenerateCached(false)
    @ImportStatic(CONTEXT.class)
    public abstract static class ContextObjectReadNode extends AbstractNode {

        public abstract Object execute(Node node, ContextObject context, long index);

        @Specialization(guards = "index == SENDER_OR_NIL")
        protected static final Object doSender(final ContextObject context, @SuppressWarnings("unused") final long index) {
            return context.getSender();
        }

        @Specialization(guards = {"index == INSTRUCTION_POINTER"})
        protected static final Object doInstructionPointer(final Node node, final ContextObject context, @SuppressWarnings("unused") final long index,
                        @Exclusive @Cached final InlinedConditionProfile nilProfile) {
            return context.getInstructionPointer(nilProfile, node);
        }

        @Specialization(guards = "index == STACKPOINTER")
        protected static final long doStackPointer(final ContextObject context, @SuppressWarnings("unused") final long index) {
            return context.getStackPointer(); // Must return a long.
        }

        @Specialization(guards = {"index == METHOD", "context.hasTruffleFrame()"})
        protected static final CompiledCodeObject doMethod(final ContextObject context, @SuppressWarnings("unused") final long index) {
            return context.getCodeObject();
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"index == METHOD", "!context.hasTruffleFrame()"})
        protected static final NilObject doNilMethod(final ContextObject context, final long index) {
            return NilObject.SINGLETON;
        }

        @Specialization(guards = {"index == CLOSURE_OR_NIL"})
        protected static final Object doClosure(final Node node, final ContextObject context, @SuppressWarnings("unused") final long index,
                        @Exclusive @Cached final InlinedConditionProfile nilProfile) {
            return NilObject.nullToNil(context.getClosure(), nilProfile, node);
        }

        @Specialization(guards = "index == RECEIVER")
        protected static final Object doReceiver(final ContextObject context, @SuppressWarnings("unused") final long index) {
            return context.getReceiver();
        }

        @Specialization(guards = "index >= TEMP_FRAME_START")
        protected static final Object doTemp(final ContextObject context, final long index) {
            return context.atTemp((int) (index - CONTEXT.TEMP_FRAME_START));
        }
    }

    @GenerateInline
    @GenerateUncached
    @GenerateCached(false)
    @ImportStatic(CONTEXT.class)
    public abstract static class ContextObjectWriteNode extends AbstractNode {

        public abstract void execute(Node node, ContextObject context, long index, Object value);

        @Specialization(guards = "index == SENDER_OR_NIL")
        protected static final void doSender(final ContextObject context, @SuppressWarnings("unused") final long index, final ContextObject value) {
            context.setSender(value);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "index == SENDER_OR_NIL")
        protected static final void doSender(final ContextObject context, final long index, final NilObject value) {
            context.setSender(NilObject.SINGLETON);
        }

        @Specialization(guards = {"index == INSTRUCTION_POINTER"})
        protected static final void doInstructionPointer(final ContextObject context, @SuppressWarnings("unused") final long index, final long value) {
            context.setInstructionPointer((int) value - context.getCodeObject().getInitialPC());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"index == INSTRUCTION_POINTER"})
        protected static final void doInstructionPointerTerminated(final ContextObject context, final long index, final NilObject value) {
            context.removeInstructionPointer();
        }

        @Specialization(guards = "index == STACKPOINTER")
        protected static final void doStackPointer(final ContextObject context, @SuppressWarnings("unused") final long index, final long value) {
            context.setStackPointer((int) value);
        }

        @Specialization(guards = "index == METHOD")
        protected static final void doMethod(final ContextObject context, @SuppressWarnings("unused") final long index, final CompiledCodeObject value) {
            context.overwriteCodeObject(value);
        }

        @Specialization(guards = {"index == CLOSURE_OR_NIL"})
        protected static final void doClosure(final ContextObject context, @SuppressWarnings("unused") final long index, final BlockClosureObject value) {
            context.setClosure(value);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"index == CLOSURE_OR_NIL"})
        protected static final void doClosure(final ContextObject context, final long index, final NilObject value) {
            context.removeClosure();
        }

        @Specialization(guards = "index == RECEIVER")
        protected static final void doReceiver(final ContextObject context, @SuppressWarnings("unused") final long index, final Object value) {
            context.setReceiver(value);
        }

        @Specialization(guards = "index >= TEMP_FRAME_START")
        protected static final void doTemp(final ContextObject context, final long index, final Object value) {
            context.atTempPut((int) (index - CONTEXT.TEMP_FRAME_START), value);
        }
    }
}
