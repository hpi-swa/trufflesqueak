/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.accessing;

import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.BLOCK_CLOSURE;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;

public final class BlockClosureObjectNodes {
    @GenerateUncached
    @ImportStatic(BLOCK_CLOSURE.class)
    public abstract static class BlockClosureObjectReadNode extends AbstractNode {

        public abstract Object execute(BlockClosureObject closure, long index);

        @Specialization(guards = "index == OUTER_CONTEXT")
        protected static final AbstractSqueakObject doClosureOuterContext(final BlockClosureObject closure, @SuppressWarnings("unused") final long index) {
            return closure.getOuterContext();
        }

        @Specialization(guards = {"index == START_PC_OR_METHOD", "closure.isABlockClosure()"})
        protected static final long doClosureStartPC(final BlockClosureObject closure, @SuppressWarnings("unused") final long index) {
            return closure.getStartPC();
        }

        @Specialization(guards = {"index == START_PC_OR_METHOD", "closure.isAFullBlockClosure()"})
        protected static final CompiledCodeObject doFullClosureMethod(final BlockClosureObject closure, @SuppressWarnings("unused") final long index) {
            return closure.getCompiledBlock();
        }

        @Specialization(guards = "index == ARGUMENT_COUNT")
        protected static final long doClosureArgumentCount(final BlockClosureObject closure, @SuppressWarnings("unused") final long index) {
            return closure.getNumArgs();
        }

        @Specialization(guards = {"index >= FIRST_COPIED_VALUE", "closure.isABlockClosure()"})
        protected static final Object doClosureCopiedValues(final BlockClosureObject closure, final long index) {
            return closure.getCopiedValue((int) index - BLOCK_CLOSURE.FIRST_COPIED_VALUE);
        }

        @Specialization(guards = {"index == FULL_RECEIVER", "closure.isAFullBlockClosure()"})
        protected static final Object doFullClosureReceiver(final BlockClosureObject closure, @SuppressWarnings("unused") final long index) {
            return closure.getReceiver();
        }

        @Specialization(guards = {"index >= FULL_FIRST_COPIED_VALUE", "closure.isAFullBlockClosure()"})
        protected static final Object doFullClosureCopiedValues(final BlockClosureObject closure, final long index) {
            return closure.getCopiedValue((int) index - BLOCK_CLOSURE.FULL_FIRST_COPIED_VALUE);
        }
    }

    @GenerateUncached
    @ImportStatic(BLOCK_CLOSURE.class)
    public abstract static class BlockClosureObjectWriteNode extends AbstractNode {

        public abstract void execute(BlockClosureObject closure, long index, Object value);

        @Specialization(guards = "index == OUTER_CONTEXT")
        protected static final void doClosureOuterContext(final BlockClosureObject closure, @SuppressWarnings("unused") final long index, final ContextObject value) {
            closure.setOuterContext(value);
        }

        @Specialization(guards = {"index == START_PC_OR_METHOD", "closure.isABlockClosure()"})
        protected static final void doClosureStartPC(final BlockClosureObject closure, @SuppressWarnings("unused") final long index, final long value) {
            closure.setStartPC((int) value);
        }

        @Specialization(guards = {"index == START_PC_OR_METHOD", "closure.isAFullBlockClosure()"})
        protected static final void doClosureStartPC(final BlockClosureObject closure, @SuppressWarnings("unused") final long index, final CompiledCodeObject value) {
            closure.setBlock(value);
        }

        @Specialization(guards = "index == ARGUMENT_COUNT")
        protected static final void doClosureArgumentCount(final BlockClosureObject closure, @SuppressWarnings("unused") final long index, final long value) {
            closure.setNumArgs((int) value);
        }

        @Specialization(guards = {"index >= FIRST_COPIED_VALUE", "closure.isABlockClosure()"})
        protected static final void doClosureCopiedValues(final BlockClosureObject closure, final long index, final Object value) {
            closure.setCopiedValue((int) index - BLOCK_CLOSURE.FIRST_COPIED_VALUE, value);
        }

        @Specialization(guards = {"index == FULL_RECEIVER", "closure.isAFullBlockClosure()"})
        protected static final void doFullClosureReceiver(final BlockClosureObject closure, @SuppressWarnings("unused") final long index, final Object value) {
            closure.setReceiver(value);
        }

        @Specialization(guards = {"index >= FULL_FIRST_COPIED_VALUE", "closure.isAFullBlockClosure()"})
        protected static final void doFullClosureCopiedValues(final BlockClosureObject closure, final long index, final Object value) {
            closure.setCopiedValue((int) index - BLOCK_CLOSURE.FULL_FIRST_COPIED_VALUE, value);
        }
    }
}
