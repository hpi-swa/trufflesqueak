package de.hpi.swa.graal.squeak.nodes.context.stack;

import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.nodes.SqueakNodeWithCode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackReadAndClearNode;

public abstract class AbstractStackPopNode extends SqueakNodeWithCode {
    @Child private FrameStackReadAndClearNode readAndClearNode;

    public AbstractStackPopNode(final CompiledCodeObject code) {
        super(code);
        readAndClearNode = FrameStackReadAndClearNode.create(code);
    }

    protected final Object atStackAndClear(final VirtualFrame frame, final int index) {
        final Object value = readAndClearNode.execute(frame, index);
        assert value != null;
        return value;
    }

    protected final int frameStackPointer(final VirtualFrame frame) {
        return FrameUtil.getIntSafe(frame, code.getStackPointerSlot());
    }

    protected final void setFrameStackPointer(final VirtualFrame frame, final int value) {
        frame.setInt(code.getStackPointerSlot(), value);
    }
}
