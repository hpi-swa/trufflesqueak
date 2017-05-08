package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.BlockClosure;

public abstract class BlockActivationNode extends Node {
    public abstract Object executeBlock(BlockClosure block, Object[] arguments);

    @SuppressWarnings("unused")
    @Specialization(guards = {"block.getCallTarget() == cachedTarget"})
    protected static Object doDirect(BlockClosure block, Object[] arguments,
                    @Cached("block.getCallTarget()") RootCallTarget cachedTarget,
                    @Cached("create(cachedTarget)") DirectCallNode callNode) {
        return callNode.call(arguments);
    }

    @Specialization(replaces = "doDirect")
    protected static Object doIndirect(BlockClosure block, Object[] arguments,
                    @Cached("create()") IndirectCallNode callNode) {
        return callNode.call(block.getCallTarget(), arguments);
    }
}
