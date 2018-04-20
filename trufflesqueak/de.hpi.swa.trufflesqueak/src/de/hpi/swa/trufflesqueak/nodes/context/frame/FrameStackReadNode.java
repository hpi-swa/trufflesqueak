package de.hpi.swa.trufflesqueak.nodes.context.frame;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.nodes.Node;

public abstract class FrameStackReadNode extends Node {
    public static FrameStackReadNode create() {
        return FrameStackReadNodeGen.create();
    }

    public abstract Object execute(Frame frame, int stackIndex);

    protected FrameSlot getFrameSlotForIndex(final Frame frame, final int index) {
        assert index >= 0;
        return frame.getFrameDescriptor().findFrameSlot(index);
    }

    protected static final int SQUEAK_MAX_STACK_SIZE = 200;

    @SuppressWarnings("unused")
    @Specialization(guards = {"index == cachedIndex"}, limit = "SQUEAK_MAX_STACK_SIZE")
    protected Object readInt(final Frame frame, final int index,
                    @Cached("index") final int cachedIndex,
                    @Cached("getFrameSlotForIndex(frame, index)") final FrameSlot slot,
                    @Cached("create(slot)") final FrameSlotReadNode readNode) {
        return readNode.executeRead(frame);
    }
}
