package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.graal.squeak.model.BlockClosureObject;

@NodeInfo(cost = NodeCost.NONE)
public abstract class BlockActivationNode extends AbstractNode {

    public static BlockActivationNode create() {
        return BlockActivationNodeGen.create();
    }

    public abstract Object executeBlock(BlockClosureObject block, Object[] arguments);

    @SuppressWarnings("unused")
    @Specialization(guards = {"block.getCallTarget() == cachedTarget"}, assumptions = {"callTargetStable"})
    protected static final Object doDirect(final BlockClosureObject block, final Object[] arguments,
                    @Cached("block.getCallTarget()") final RootCallTarget cachedTarget,
                    @Cached("block.getCallTargetStable()") final Assumption callTargetStable,
                    @Cached("create(cachedTarget)") final DirectCallNode directCallNode) {
        return directCallNode.call(arguments);
    }

    @Specialization(replaces = "doDirect")
    protected static final Object doIndirect(final BlockClosureObject block, final Object[] arguments,
                    @Cached final IndirectCallNode indirectCallNode) {
        return indirectCallNode.call(block.getCallTarget(), arguments);
    }
}
