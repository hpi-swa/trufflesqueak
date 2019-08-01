package de.hpi.swa.graal.squeak.nodes.context.frame;

import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameUtil;

@ImportStatic(FrameSlotKind.class)
public abstract class AbstractFrameSlotReadNode extends AbstractFrameSlotNode {
    public abstract Object executeRead(Frame frame);

    @Specialization(guards = "frame.getFrameDescriptor().getFrameSlotKind(getSlot()) == Boolean")
    protected final boolean readBoolean(final Frame frame) {
        return FrameUtil.getBooleanSafe(frame, getSlot());
    }

    @Specialization(guards = "frame.getFrameDescriptor().getFrameSlotKind(getSlot()) == Long")
    protected final long readLong(final Frame frame) {
        return FrameUtil.getLongSafe(frame, getSlot());
    }

    @Specialization(guards = "frame.getFrameDescriptor().getFrameSlotKind(getSlot()) == Double")
    protected final double readDouble(final Frame frame) {
        return FrameUtil.getDoubleSafe(frame, getSlot());
    }
}
