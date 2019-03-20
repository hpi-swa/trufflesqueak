package de.hpi.swa.graal.squeak.nodes.accessing;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ContextObjectNodesFactory.ContextObjectReadNodeGen;
import de.hpi.swa.graal.squeak.nodes.accessing.ContextObjectNodesFactory.ContextObjectWriteNodeGen;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackReadNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackWriteNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class ContextObjectNodes {

    @ImportStatic(CONTEXT.class)
    public abstract static class ContextObjectReadNode extends AbstractNode {
        public static ContextObjectReadNode create() {
            return ContextObjectReadNodeGen.create();
        }

        public abstract Object execute(ContextObject context, long index);

        @Specialization(guards = "index == SENDER_OR_NIL")
        protected static final Object doSender(final ContextObject context, @SuppressWarnings("unused") final long index) {
            return context.getSender();
        }

        @Specialization(guards = {"index == INSTRUCTION_POINTER", "context.getInstructionPointer() >= 0"})
        protected static final long doInstructionPointer(final ContextObject context, @SuppressWarnings("unused") final long index) {
            return context.getInstructionPointer(); // Must return a long.
        }

        @Specialization(guards = {"index == INSTRUCTION_POINTER", "context.getInstructionPointer() < 0"})
        protected static final Object doInstructionPointerTerminated(final ContextObject context, @SuppressWarnings("unused") final long index) {
            return context.image.nil;
        }

        @Specialization(guards = "index == STACKPOINTER")
        protected static final long doStackPointer(final ContextObject context, @SuppressWarnings("unused") final long index) {
            return context.getStackPointer(); // Must return a long.
        }

        @Specialization(guards = "index == METHOD")
        protected static final CompiledMethodObject doMethod(final ContextObject context, @SuppressWarnings("unused") final long index) {
            return context.getMethod();
        }

        @Specialization(guards = {"index == CLOSURE_OR_NIL", "context.getClosure() != null"})
        protected static final BlockClosureObject doClosure(final ContextObject context, @SuppressWarnings("unused") final long index) {
            return context.getClosure();
        }

        @Specialization(guards = {"index == CLOSURE_OR_NIL", "context.getClosure() == null"})
        protected static final Object doClosureNil(final ContextObject context, @SuppressWarnings("unused") final long index) {
            return context.image.nil;
        }

        @Specialization(guards = "index == RECEIVER")
        protected static final Object doReceiver(final ContextObject context, @SuppressWarnings("unused") final long index) {
            return context.getReceiver();
        }

        @Specialization(guards = {"index >= TEMP_FRAME_START", "codeObject == context.getBlockOrMethod()"}, //
                        limit = "2" /** thisContext and sender */
        )
        protected static final Object doTempCached(final ContextObject context, @SuppressWarnings("unused") final long index,
                        @SuppressWarnings("unused") @Cached("context.getBlockOrMethod()") final CompiledCodeObject codeObject,
                        @Cached("create(codeObject)") final FrameStackReadNode readNode) {
            final Object value = readNode.execute(context.getTruffleFrame(), (int) (index - CONTEXT.TEMP_FRAME_START));
            return value == null ? context.image.nil : value;
        }

        @Specialization(guards = "index >= TEMP_FRAME_START")
        protected static final Object doTemp(final ContextObject context, final long index) {
            return context.atTemp((int) (index - CONTEXT.TEMP_FRAME_START));
        }

// @Fallback
// protected static final Object doFail(final ContextObject obj, final long index) {
// throw SqueakException.create("Unexpected values:", obj, index);
// }
    }

    @ImportStatic(CONTEXT.class)
    public abstract static class ContextObjectWriteNode extends AbstractNode {

        public static ContextObjectWriteNode create() {
            return ContextObjectWriteNodeGen.create();
        }

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

        @Specialization(guards = {"index >= TEMP_FRAME_START", "context.getBlockOrMethod() == codeObject"}, //
                        limit = "2"/** thisContext and sender */
        )
        protected static final void doTempCached(final ContextObject context, final long index, final Object value,
                        @SuppressWarnings("unused") @Cached("context.getBlockOrMethod()") final CompiledCodeObject codeObject,
                        @Cached("create(codeObject)") final FrameStackWriteNode writeNode) {
            final int stackIndex = (int) (index - CONTEXT.TEMP_FRAME_START);
            FrameAccess.setArgumentIfInRange(context.getTruffleFrame(), stackIndex, value);
            writeNode.execute(context.getTruffleFrame(), stackIndex, value);
        }

        @Specialization(guards = "index >= TEMP_FRAME_START")
        protected static final void doTemp(final ContextObject context, final long index, final Object value) {
            context.atTempPut((int) (index - CONTEXT.TEMP_FRAME_START), value);
        }

// @Fallback
// protected static final void doFail(final ContextObject obj, final long index, final Object value)
// {
// throw SqueakException.create("Unexpected values:", obj, index, value);
// }
    }
}
