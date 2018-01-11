package de.hpi.swa.trufflesqueak.nodes.context.frame;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

public abstract class FrameStackWriteNode extends Node {
    public static FrameStackWriteNode create() {
        return FrameStackWriteNodeGen.create();
    }

    public abstract Object execute(VirtualFrame frame, int stackIndex, Object value);

    protected FrameSlot getFrameSlotForIndex(VirtualFrame frame, int index) {
        assert index >= 0;
        FrameSlot slot = frame.getFrameDescriptor().findFrameSlot(index);
        assert slot != null;
        return slot;
    }

    protected static final int SQUEAK_MAX_STACK_SIZE = 200;

    @SuppressWarnings("unused")
    @Specialization(guards = {"index == cachedIndex"}, limit = "SQUEAK_MAX_STACK_SIZE")
    protected Object writeInt(VirtualFrame frame, int index, Object value,
                    @Cached("index") int cachedIndex,
                    @Cached("getFrameSlotForIndex(frame, index)") FrameSlot slot,
                    @Cached("create(slot)") FrameSlotWriteNode writeNode) {
        writeNode.executeWrite(frame, value);
        return null;
    }
}
