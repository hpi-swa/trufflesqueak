package de.hpi.swa.graal.squeak.nodes.context.frame;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;

public abstract class FrameSlotReadNode extends AbstractFrameSlotReadNode {
    public static FrameSlotReadNode create(final FrameSlot frameSlot) {
        return FrameSlotReadNodeGen.create(frameSlot);
    }

    @Specialization(replaces = {"readBoolean", "readLong", "readDouble"})
    protected final Object readObject(final Frame frame) {
        final Object value;
        if (!frame.isObject(getSlot())) {
            /*
             * The FrameSlotKind has been set to Object, so from now on all writes to the slot will
             * be Object writes. However, now we are in a frame that still has an old non-Object
             * value. This is a slow-path operation: we read the non-Object value, and write it
             * immediately as an Object value so that we do not hit this path again multiple times
             * for the same slot of the same frame.
             */
            CompilerDirectives.transferToInterpreter();
            value = frame.getValue(getSlot());
            frame.setObject(getSlot(), value);
        } else {
            value = FrameUtil.getObjectSafe(frame, getSlot());
        }
        assert value != null : "Unexpected `null` value";
        return value;
    }
}
