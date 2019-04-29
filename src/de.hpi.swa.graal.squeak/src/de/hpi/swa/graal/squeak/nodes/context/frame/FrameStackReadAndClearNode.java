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
public abstract class FrameStackReadAndClearNode extends AbstractNodeWithCode {

    protected FrameStackReadAndClearNode(final CompiledCodeObject code) {
        super(code);
    }

    public static FrameStackReadAndClearNode create(final CompiledCodeObject code) {
        return FrameStackReadAndClearNodeGen.create(code);
    }

    public abstract Object execute(Frame frame, int stackIndex);

    @SuppressWarnings("unused")
    @Specialization(guards = {"index == cachedIndex"}, limit = "MAX_STACK_SIZE")
    protected static final Object doClear(final Frame frame, final int index,
                    @Cached("index") final int cachedIndex,
                    @Cached("code.getStackSlot(index)") final FrameSlot slot,
                    @Cached("createReadNode(slot, index)") final AbstractFrameSlotReadNode clearNode) {
        return clearNode.executeRead(frame);
    }

    protected final AbstractFrameSlotReadNode createReadNode(final FrameSlot frameSlot, final int index) {
        // Only clear stack values, not receiver, arguments, or temporary variables.
        if (index >= code.getNumArgsAndCopied() + code.getNumTemps()) {
            return FrameSlotReadAndClearNode.create(frameSlot);
        } else {
            return FrameSlotReadNode.create(frameSlot);
        }
    }
}
