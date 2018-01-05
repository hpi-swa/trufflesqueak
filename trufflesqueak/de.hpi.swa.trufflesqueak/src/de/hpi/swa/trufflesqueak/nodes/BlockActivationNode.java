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
    protected static Object doDirect(BlockClosureObject block, Object[] arguments,
                    @Cached("block.getCompiledBlock()") CompiledBlockObject cachedCompiledBlock,
                    @Cached("block.getCallTarget()") RootCallTarget cachedTarget,
                    @Cached("block.getCallTargetStable()") Assumption callTargetStable,
                    @Cached("create(cachedTarget)") DirectCallNode callNode) {
        return callNode.call(arguments);
    }

    @Specialization(replaces = "doDirect")
    protected static Object doIndirect(BlockClosureObject block, Object[] arguments,
                    @Cached("create()") IndirectCallNode callNode) {
        return callNode.call(block.getCallTarget(), arguments);
    }
}
