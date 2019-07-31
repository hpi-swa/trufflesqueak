package de.hpi.swa.graal.squeak.nodes.context.frame;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;

public abstract class FrameSlotReadNode extends AbstractFrameSlotReadNode {

    protected FrameSlotReadNode(final FrameSlot frameSlot) {
        super(frameSlot);
    }

    public static FrameSlotReadNode create(final FrameSlot frameSlot) {
        return FrameSlotReadNodeGen.create(frameSlot);
    }

    @Specialization(replaces = {"readBoolean", "readLong", "readDouble"})
    protected final Object readObject(final Frame frame) {
        if (!frame.isObject(frameSlot)) {
            CompilerDirectives.transferToInterpreter();
            final Object value = frame.getValue(frameSlot);
            assert value != null : "Unexpected `null` value";
            frame.setObject(frameSlot, value);
            return value;
        }
        return FrameUtil.getObjectSafe(frame, frameSlot);
    }
}
