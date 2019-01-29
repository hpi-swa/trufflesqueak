package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class TerminateContextNode extends AbstractNodeWithCode {

    protected TerminateContextNode(final CompiledCodeObject code) {
        super(code);
    }

    public static TerminateContextNode create(final CompiledCodeObject code) {
        return new TerminateContextNode(code);
    }

    public void executeTerminate(final VirtualFrame frame) {
        FrameAccess.setInstructionPointer(frame, code, -1);
        FrameAccess.setSender(frame, code.image.nil);
    }
}
