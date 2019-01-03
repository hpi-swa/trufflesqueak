package de.hpi.swa.graal.squeak.nodes.context;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledBlockObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithCode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class UpdateInstructionPointerNode extends AbstractNodeWithCode {
    private final int initialPC;

    public static UpdateInstructionPointerNode create(final CompiledCodeObject code) {
        return new UpdateInstructionPointerNode(code);
    }

    protected UpdateInstructionPointerNode(final CompiledCodeObject code) {
        super(code);
        initialPC = code instanceof CompiledBlockObject ? ((CompiledBlockObject) code).getInitialPC() : ((CompiledMethodObject) code).getInitialPC();
    }

    public void executeUpdate(final VirtualFrame frame, final int value) {
        FrameAccess.setInstructionPointer(frame, code, initialPC + value);
    }
}
