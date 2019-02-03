package de.hpi.swa.graal.squeak.nodes.context.stack;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.nodes.SqueakNodeWithCode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackReadNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class StackPeekNode extends SqueakNodeWithCode {
    @Child private FrameStackReadNode readNode;
    private final int offset;

    protected StackPeekNode(final CompiledCodeObject code, final int offset) {
        super(code);
        readNode = FrameStackReadNode.create(code);
        this.offset = offset;
    }

    public static StackPeekNode create(final CompiledCodeObject code, final int offset) {
        return new StackPeekNode(code, offset);
    }

    @Override
    public Object executeRead(final VirtualFrame frame) {
        return readNode.execute(frame, FrameAccess.getStackPointer(frame, code) - offset);
    }
}
