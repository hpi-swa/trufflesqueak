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

    protected FrameSlot getFrameSlotForIndex(Frame frame, int index) {
        assert index >= 0;
        FrameSlot slot = frame.getFrameDescriptor().findFrameSlot(index);
        assert slot != null;
        return slot;
    }

    protected static final int SQUEAK_MAX_STACK_SIZE = 200;

    @SuppressWarnings("unused")
    @Specialization(guards = {"index == cachedIndex"}, limit = "SQUEAK_MAX_STACK_SIZE")
    protected Object readInt(Frame frame, int index,
                    @Cached("index") int cachedIndex,
                    @Cached("getFrameSlotForIndex(frame, index)") FrameSlot slot,
                    @Cached("create(slot)") FrameSlotReadNode readNode) {
        return readNode.executeRead(frame);
    }
}
