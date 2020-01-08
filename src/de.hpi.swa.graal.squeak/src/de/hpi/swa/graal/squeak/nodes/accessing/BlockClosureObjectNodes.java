/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.accessing;

import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.BLOCK_CLOSURE;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;
import de.hpi.swa.graal.squeak.nodes.accessing.BlockClosureObjectNodesFactory.BlockClosureObjectReadNodeGen;
import de.hpi.swa.graal.squeak.nodes.accessing.BlockClosureObjectNodesFactory.BlockClosureObjectWriteNodeGen;

public final class BlockClosureObjectNodes {

    @GenerateUncached
    @ImportStatic(BLOCK_CLOSURE.class)
    public abstract static class BlockClosureObjectReadNode extends AbstractNode {
        public static BlockClosureObjectReadNode create() {
            return BlockClosureObjectReadNodeGen.create();
        }

        public abstract Object execute(BlockClosureObject closure, long index);

        @Specialization(guards = "index == OUTER_CONTEXT")
        protected static final AbstractSqueakObject doClosureOuterContext(final BlockClosureObject closure, @SuppressWarnings("unused") final long index) {
            return closure.getOuterContext();
        }

        @Specialization(guards = "index == START_PC")
        protected static final long doClosureStartPC(final BlockClosureObject closure, @SuppressWarnings("unused") final long index) {
            return closure.getStartPC();
        }

        @Specialization(guards = "index == ARGUMENT_COUNT")
        protected static final long doClosureArgumentCount(final BlockClosureObject closure, @SuppressWarnings("unused") final long index) {
            return closure.getNumArgs();
        }

        @Specialization(guards = "index > ARGUMENT_COUNT")
        protected static final Object doClosureCopiedValues(final BlockClosureObject closure, final long index) {
            return closure.getCopiedAt0((int) index);
        }
    }

    @GenerateUncached
    @ImportStatic(BLOCK_CLOSURE.class)
    public abstract static class BlockClosureObjectWriteNode extends AbstractNode {

        public static BlockClosureObjectWriteNode create() {
            return BlockClosureObjectWriteNodeGen.create();
        }

        public abstract void execute(BlockClosureObject closure, long index, Object value);

        @Specialization(guards = "index == OUTER_CONTEXT")
        protected static final void doClosureOuterContext(final BlockClosureObject closure, @SuppressWarnings("unused") final long index, final ContextObject value) {
            closure.setOuterContext(value);
        }

        @Specialization(guards = "index == START_PC")
        protected static final void doClosureStartPC(final BlockClosureObject closure, @SuppressWarnings("unused") final long index, final long value) {
            closure.setStartPC((int) value);
        }

        @Specialization(guards = "index == ARGUMENT_COUNT")
        protected static final void doClosureArgumentCount(final BlockClosureObject closure, @SuppressWarnings("unused") final long index, final long value) {
            closure.setNumArgs((int) value);
        }

        @Specialization(guards = "index > ARGUMENT_COUNT")
        protected static final void doClosureCopiedValues(final BlockClosureObject closure, final long index, final Object value) {
            closure.setCopiedAt0((int) index, value);
        }
    }
}
