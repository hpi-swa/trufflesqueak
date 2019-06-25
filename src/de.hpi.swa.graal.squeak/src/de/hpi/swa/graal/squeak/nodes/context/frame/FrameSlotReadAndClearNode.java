package de.hpi.swa.graal.squeak.nodes.context.frame;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;

public abstract class FrameSlotReadAndClearNode extends AbstractFrameSlotReadNode {

    protected FrameSlotReadAndClearNode(final FrameSlot slot) {
        super(slot);
    }

    public static FrameSlotReadAndClearNode create(final FrameSlot slot) {
        return FrameSlotReadAndClearNodeGen.create(slot);
    }

    @Specialization(replaces = {"readBoolean", "readLong", "readDouble"})
    protected final Object readAndClearObject(final Frame frame) {
        if (!frame.isObject(frameSlot)) {
            CompilerDirectives.transferToInterpreter();
            final Object value = frame.getValue(frameSlot);
            assert value != null;
            frame.setObject(frameSlot, null);
            return value;
        }
        final Object value = FrameUtil.getObjectSafe(frame, frameSlot);
        frame.setObject(frameSlot, null);
        return value;
    }
}
