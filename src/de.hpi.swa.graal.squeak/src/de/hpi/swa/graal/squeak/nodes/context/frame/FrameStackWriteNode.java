package de.hpi.swa.graal.squeak.nodes.context.frame;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.CONTEXT;

@ImportStatic(CONTEXT.class)
public abstract class FrameStackWriteNode extends Node {

    public static FrameStackWriteNode create() {
        return FrameStackWriteNodeGen.create();
    }

    public abstract void execute(Frame frame, int stackIndex, Object value);

    protected static final FrameSlot getFrameSlotForIndex(final VirtualFrame frame, final int index) {
        assert index >= 0;
        return frame.getFrameDescriptor().findFrameSlot(index);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"index == cachedIndex"}, limit = "MAX_STACK_SIZE")
    protected static final void doWrite(final VirtualFrame frame, final int index, final Object value,
                    @Cached("index") final int cachedIndex,
                    @Cached("getFrameSlotForIndex(frame, index)") final FrameSlot slot,
                    @Cached("create(slot)") final FrameSlotWriteNode writeNode) {
        writeNode.executeWrite(frame, value);
    }

    @SuppressWarnings("unused")
    @Specialization(replaces = "doWrite")
    protected static final void doFail(final Frame frame, final int stackIndex, final Object value) {
        throw new SqueakException("Unexpected failure in FrameStackWriteNode");
    }
}
