/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;

import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.CompiledBlockObject;

public abstract class DispatchClosureNode extends AbstractNode {
    protected static final int INLINE_CACHE_SIZE = 3;

    public abstract Object execute(BlockClosureObject closure, Object[] arguments);

    @SuppressWarnings("unused")
    @Specialization(guards = {"closure.getCompiledBlock() == cachedBlock"}, assumptions = {"cachedBlock.getCallTargetStable()"}, limit = "INLINE_CACHE_SIZE")
    protected static final Object doDirect(final BlockClosureObject closure, final Object[] arguments,
                    @Cached("closure.getCompiledBlock()") final CompiledBlockObject cachedBlock,
                    @Cached("create(cachedBlock.getCallTarget())") final DirectCallNode directCallNode) {
        return directCallNode.call(arguments);
    }

    @Specialization(replaces = "doDirect")
    protected static final Object doIndirect(final BlockClosureObject closure, final Object[] arguments,
                    @Cached final IndirectCallNode indirectCallNode) {
        return indirectCallNode.call(closure.getCompiledBlock().getCallTarget(), arguments);
    }
}
