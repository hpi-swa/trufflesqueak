package de.hpi.swa.graal.squeak.nodes.context;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;

import de.hpi.swa.graal.squeak.model.NotProvided;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public abstract class ArgumentNode extends AbstractNode {
    private final int argumentIndex;
    protected final boolean inBounds;

    protected ArgumentNode(final int argumentIndex, final int numArguments) {
        this.argumentIndex = argumentIndex; // argumentIndex == 0 returns receiver
        inBounds = argumentIndex <= numArguments;
    }

    public static ArgumentNode create(final int argumentIndex, final int numArguments) {
        return ArgumentNodeGen.create(argumentIndex, numArguments);
    }

    public abstract Object execute(VirtualFrame frame);

    @Specialization(guards = {"inBounds"})
    protected final Object doArgument(final VirtualFrame frame) {
        return FrameAccess.getArgument(frame, argumentIndex);
    }

    @Specialization(guards = {"!inBounds"})
    protected static final Object doArgumentsExhausted() {
        return NotProvided.SINGLETON;
    }

    @Override
    public NodeCost getCost() {
        return inBounds ? super.getCost() : NodeCost.NONE;
    }
}
