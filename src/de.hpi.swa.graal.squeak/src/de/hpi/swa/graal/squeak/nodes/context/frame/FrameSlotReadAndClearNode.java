package de.hpi.swa.graal.squeak.nodes.context.frame;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;

public abstract class FrameSlotReadAndClearNode extends AbstractFrameSlotNode {

    protected FrameSlotReadAndClearNode(final FrameSlot slot) {
        super(slot);
    }

    public static FrameSlotReadAndClearNode create(final FrameSlot slot) {
        return FrameSlotReadAndClearNodeGen.create(slot);
    }

    public abstract Object executeReadAndClear(Frame frame, boolean clear);

    @Specialization(rewriteOn = FrameSlotTypeException.class)
    protected final boolean readBoolean(final Frame frame, @SuppressWarnings("unused") final boolean clear) throws FrameSlotTypeException {
        return frame.getBoolean(frameSlot);
    }

    @Specialization(rewriteOn = FrameSlotTypeException.class)
    protected final long readLong(final Frame frame, @SuppressWarnings("unused") final boolean clear) throws FrameSlotTypeException {
        return frame.getLong(frameSlot);
    }

    @Specialization(rewriteOn = FrameSlotTypeException.class)
    protected final double readDouble(final Frame frame, @SuppressWarnings("unused") final boolean clear) throws FrameSlotTypeException {
        return frame.getDouble(frameSlot);
    }

    @Specialization(guards = "clear", rewriteOn = FrameSlotTypeException.class)
    protected final Object readObjectClear(final Frame frame, @SuppressWarnings("unused") final boolean clear) throws FrameSlotTypeException {
        final Object value = frame.getObject(frameSlot);
        frame.setObject(frameSlot, null);
        return value;
    }

    @Specialization(guards = "!clear", rewriteOn = FrameSlotTypeException.class)
    protected final Object readObject(final Frame frame, @SuppressWarnings("unused") final boolean clear) throws FrameSlotTypeException {
        return frame.getObject(frameSlot);
    }

    @Specialization(guards = "clear", replaces = {"readBoolean", "readLong", "readDouble", "readObjectClear"})
    protected final Object readAnyClear(final Frame frame, @SuppressWarnings("unused") final boolean clear) {
        final Object value = frame.getValue(frameSlot);
        assert value != null;
        frame.setObject(frameSlot, null);
        return value;
    }

    @Specialization(guards = "!clear", replaces = {"readBoolean", "readLong", "readDouble", "readObject"})
    protected final Object readAny(final Frame frame, @SuppressWarnings("unused") final boolean clear) {
        final Object value = frame.getValue(frameSlot);
        assert value != null;
        return value;
    }
}
