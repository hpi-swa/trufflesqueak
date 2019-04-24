package de.hpi.swa.graal.squeak.nodes.context.stack;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.nodes.SqueakNodeWithCode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackReadNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class StackTopNode extends SqueakNodeWithCode {
    @Child private FrameStackReadNode readNode;

    private StackTopNode(final CompiledCodeObject code) {
        super(code);
        readNode = FrameStackReadNode.create(code);
    }

    public static StackTopNode create(final CompiledCodeObject code) {
        return new StackTopNode(code);
    }

    @Override
    public Object executeRead(final VirtualFrame frame) {
        return readNode.execute(frame, FrameAccess.getStackPointer(frame, code) - 1);
    }
}
