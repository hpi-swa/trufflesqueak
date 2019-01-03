package de.hpi.swa.graal.squeak.nodes.context.stack;

import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.nodes.SqueakNodeWithCode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackReadNode;

public abstract class AbstractStackNode extends SqueakNodeWithCode {
    @Child protected FrameStackReadNode readNode;

    public AbstractStackNode(final CompiledCodeObject code) {
        super(code);
        readNode = FrameStackReadNode.create(code);
    }

    protected final int frameStackPointer(final VirtualFrame frame) {
        return FrameUtil.getIntSafe(frame, code.getStackPointerSlot());
    }

    protected final void setFrameStackPointer(final VirtualFrame frame, final int value) {
        frame.setInt(code.getStackPointerSlot(), value);
    }
}
