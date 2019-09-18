/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.accessing;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.CONTEXT;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotReadNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotWriteNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class ContextObjectNodes {

    @GenerateUncached
    @ImportStatic(CONTEXT.class)
    public abstract static class ContextObjectReadNode extends AbstractNode {

        public abstract Object execute(ContextObject context, long index);

        @Specialization(guards = "index == SENDER_OR_NIL")
        protected static final Object doSender(final ContextObject context, @SuppressWarnings("unused") final long index) {
            return context.getSender();
        }

        @Specialization(guards = {"index == INSTRUCTION_POINTER"})
        protected static final Object doInstructionPointer(final ContextObject context, @SuppressWarnings("unused") final long index,
                        @Cached("createBinaryProfile()") final ConditionProfile nilProfile) {
            final long pc = context.getInstructionPointer(); // Must be a long.
            return nilProfile.profile(pc < 0) ? NilObject.SINGLETON : pc;
        }

        @Specialization(guards = "index == STACKPOINTER")
        protected static final long doStackPointer(final ContextObject context, @SuppressWarnings("unused") final long index) {
            return context.getStackPointer(); // Must return a long.
        }

        @Specialization(guards = "index == METHOD")
        protected static final CompiledMethodObject doMethod(final ContextObject context, @SuppressWarnings("unused") final long index) {
            return context.getMethod();
        }

        @Specialization(guards = {"index == CLOSURE_OR_NIL"})
        protected static final Object doClosure(final ContextObject context, @SuppressWarnings("unused") final long index,
                        @Cached("createBinaryProfile()") final ConditionProfile nilProfile) {
            return NilObject.nullToNil(context.getClosure(), nilProfile);
        }

        @Specialization(guards = "index == RECEIVER")
        protected static final Object doReceiver(final ContextObject context, @SuppressWarnings("unused") final long index) {
            return context.getReceiver();
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"index >= TEMP_FRAME_START", "context == cachedContext", "index == cachedIndex"}, limit = "4")
        protected static final Object doTempCached(final ContextObject context, final long index,
                        @Cached("context") final ContextObject cachedContext,
                        @Cached("index") final long cachedIndex,
                        @Cached("createReadNode(cachedContext, cachedIndex)") final FrameSlotReadNode readNode,
                        @Cached("createBinaryProfile()") final ConditionProfile isNullProfile) {
            return NilObject.nullToNil(readNode.executeRead(cachedContext.getTruffleFrame()), isNullProfile);
        }

        protected static final FrameSlotReadNode createReadNode(final ContextObject context, final long index) {
            return FrameSlotReadNode.create(context.getBlockOrMethod().getStackSlot((int) (index - CONTEXT.TEMP_FRAME_START)));
        }

        @Specialization(guards = "index >= TEMP_FRAME_START", replaces = "doTempCached")
        protected static final Object doTemp(final ContextObject context, final long index) {
            return context.atTemp((int) (index - CONTEXT.TEMP_FRAME_START));
        }
    }

    @GenerateUncached
    @ImportStatic(CONTEXT.class)
    public abstract static class ContextObjectWriteNode extends AbstractNode {

        public abstract void execute(ContextObject context, long index, Object value);

        @Specialization(guards = "index == SENDER_OR_NIL")
        protected static final void doSender(final ContextObject context, @SuppressWarnings("unused") final long index, final ContextObject value) {
            context.setSender(value);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "index == SENDER_OR_NIL")
        protected static final void doSender(final ContextObject context, final long index, final NilObject value) {
            context.removeSender();
        }

        @Specialization(guards = {"index == INSTRUCTION_POINTER"})
        protected static final void doInstructionPointer(final ContextObject context, @SuppressWarnings("unused") final long index, final long value) {
            context.setInstructionPointer((int) value);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"index == INSTRUCTION_POINTER"})
        protected static final void doInstructionPointerTerminated(final ContextObject context, final long index, final NilObject value) {
            context.setInstructionPointer(-1);
        }

        @Specialization(guards = "index == STACKPOINTER")
        protected static final void doStackPointer(final ContextObject context, @SuppressWarnings("unused") final long index, final long value) {
            context.setStackPointer((int) value);
        }

        @Specialization(guards = "index == METHOD")
        protected static final void doMethod(final ContextObject context, @SuppressWarnings("unused") final long index, final CompiledMethodObject value) {
            context.setMethod(value);
        }

        @Specialization(guards = {"index == CLOSURE_OR_NIL"})
        protected static final void doClosure(final ContextObject context, @SuppressWarnings("unused") final long index, final BlockClosureObject value) {
            context.setClosure(value);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"index == CLOSURE_OR_NIL"})
        protected static final void doClosure(final ContextObject context, final long index, final NilObject value) {
            context.setClosure(null);
        }

        @Specialization(guards = "index == RECEIVER")
        protected static final void doReceiver(final ContextObject context, @SuppressWarnings("unused") final long index, final Object value) {
            context.setReceiver(value);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"index >= TEMP_FRAME_START", "context == cachedContext", "index == cachedIndex"}, limit = "4")
        protected static final void doTempCached(final ContextObject context, final long index, final Object value,
                        @Cached("context") final ContextObject cachedContext,
                        @Cached("index") final long cachedIndex,
                        @Cached("createWriteNode(cachedContext, cachedIndex)") final FrameSlotWriteNode writeNode) {
            final MaterializedFrame truffleFrame = cachedContext.getTruffleFrame();
            FrameAccess.setArgumentIfInRange(truffleFrame, (int) (index - CONTEXT.TEMP_FRAME_START), value);
            writeNode.executeWrite(truffleFrame, value);
        }

        protected static final FrameSlotWriteNode createWriteNode(final ContextObject context, final long index) {
            return FrameSlotWriteNode.create(context.getBlockOrMethod().getStackSlot((int) (index - CONTEXT.TEMP_FRAME_START)));
        }

        @Specialization(guards = "index >= TEMP_FRAME_START", replaces = "doTempCached")
        protected static final void doTemp(final ContextObject context, final long index, final Object value) {
            context.atTempPut((int) (index - CONTEXT.TEMP_FRAME_START), value);
        }
    }
}
