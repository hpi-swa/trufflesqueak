package de.hpi.swa.graal.squeak.nodes.context.frame;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithCode;

@NodeInfo(cost = NodeCost.NONE)
@ImportStatic(CONTEXT.class)
public abstract class FrameStackReadNode extends AbstractNodeWithCode {

    protected FrameStackReadNode(final CompiledCodeObject code) {
        super(code);
    }

    public static FrameStackReadNode create(final CompiledCodeObject code) {
        return FrameStackReadNodeGen.create(code);
    }

    public abstract Object execute(Frame frame, int stackIndex);

    @SuppressWarnings("unused")
    @Specialization(guards = {"index == cachedIndex"}, limit = "MAX_STACK_SIZE")
    protected static final Object doRead(final Frame frame, final int index,
                    @Cached("index") final int cachedIndex,
                    @Cached("code.getStackSlot(index)") final FrameSlot slot,
                    @Cached("create(slot)") final FrameSlotReadNode readNode) {
        return readNode.executeRead(frame);
    }
}
