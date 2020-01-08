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
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.CompiledBlockObject;

@NodeInfo(cost = NodeCost.NONE)
public abstract class DispatchBlockNode extends AbstractNode {

    public abstract Object executeBlock(BlockClosureObject block, Object[] arguments);

    @SuppressWarnings("unused")
    @Specialization(guards = {"block.getCompiledBlock() == cachedBlock"}, assumptions = {"cachedBlock.getCallTargetStable()"})
    protected static final Object doDirect(final BlockClosureObject block, final Object[] arguments,
                    @Cached("block.getCompiledBlock()") final CompiledBlockObject cachedBlock,
                    @Cached("create(cachedBlock.getCallTarget())") final DirectCallNode directCallNode) {
        return directCallNode.call(arguments);
    }

    @Specialization(replaces = "doDirect")
    protected static final Object doIndirect(final BlockClosureObject block, final Object[] arguments,
                    @Cached final IndirectCallNode indirectCallNode) {
        return indirectCallNode.call(block.getCompiledBlock().getCallTarget(), arguments);
    }
}
