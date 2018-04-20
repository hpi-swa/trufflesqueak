package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CompiledBlockObject;

public abstract class BlockActivationNode extends Node {
    public abstract Object executeBlock(BlockClosureObject block, Object[] arguments);

    @SuppressWarnings("unused")
    @Specialization(guards = {"block.getCompiledBlock() == cachedCompiledBlock"}, assumptions = {"callTargetStable"})
    protected static Object doDirect(final BlockClosureObject block, final Object[] arguments,
                    @Cached("block.getCompiledBlock()") final CompiledBlockObject cachedCompiledBlock,
                    @Cached("block.getCallTarget()") final RootCallTarget cachedTarget,
                    @Cached("block.getCallTargetStable()") final Assumption callTargetStable,
                    @Cached("create(cachedTarget)") final DirectCallNode callNode) {
        return callNode.call(arguments);
    }

    @Specialization(replaces = "doDirect")
    protected static Object doIndirect(final BlockClosureObject block, final Object[] arguments,
                    @Cached("create()") final IndirectCallNode callNode) {
        return callNode.call(block.getCallTarget(), arguments);
    }
}
