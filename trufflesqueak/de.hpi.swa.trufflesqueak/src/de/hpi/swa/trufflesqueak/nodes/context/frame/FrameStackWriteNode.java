package de.hpi.swa.trufflesqueak.nodes.context.frame;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

public abstract class FrameStackWriteNode extends Node {
    public static FrameStackWriteNode create() {
        return FrameStackWriteNodeGen.create();
    }

    public abstract Object execute(Frame frame, int stackIndex, Object value);

    protected FrameSlot getFrameSlotForIndex(final VirtualFrame frame, final int index) {
        assert index >= 0;
        return frame.getFrameDescriptor().findFrameSlot(index);
    }

    protected static final int SQUEAK_MAX_STACK_SIZE = 200;

    @SuppressWarnings("unused")
    @Specialization(guards = {"index == cachedIndex"}, limit = "SQUEAK_MAX_STACK_SIZE")
    protected Object writeInt(final VirtualFrame frame, final int index, final Object value,
                    @Cached("index") final int cachedIndex,
                    @Cached("getFrameSlotForIndex(frame, index)") final FrameSlot slot,
                    @Cached("create(slot)") final FrameSlotWriteNode writeNode) {
        writeNode.executeWrite(frame, value);
        return null;
    }
}
