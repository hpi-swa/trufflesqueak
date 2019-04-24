package de.hpi.swa.graal.squeak.nodes.context.stack;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.nodes.SqueakNodeWithCode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackReadAndClearNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class StackPopNode extends SqueakNodeWithCode {
    @Child private FrameStackReadAndClearNode readAndClearNode;

    private StackPopNode(final CompiledCodeObject code) {
        super(code);
        readAndClearNode = FrameStackReadAndClearNode.create(code);
    }

    public static StackPopNode create(final CompiledCodeObject code) {
        return new StackPopNode(code);
    }

    @Override
    public Object executeRead(final VirtualFrame frame) {
        final int newSP = FrameAccess.getStackPointer(frame, code) - 1;
        assert newSP >= 0 : "Bad stack pointer";
        FrameAccess.setStackPointer(frame, code, newSP);
        return readAndClearNode.execute(frame, newSP);
    }
}
