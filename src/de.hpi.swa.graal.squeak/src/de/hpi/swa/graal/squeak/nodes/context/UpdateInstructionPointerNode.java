package de.hpi.swa.graal.squeak.nodes.context;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithCode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotWriteNode;

public abstract class UpdateInstructionPointerNode extends AbstractNodeWithCode {
    @Child private FrameSlotWriteNode instructionPointerWriteNode;

    public static UpdateInstructionPointerNode create(final CompiledCodeObject code) {
        return UpdateInstructionPointerNodeGen.create(code);
    }

    protected UpdateInstructionPointerNode(final CompiledCodeObject code) {
        super(code);
        instructionPointerWriteNode = FrameSlotWriteNode.create(code.instructionPointerSlot);
    }

    public abstract void executeUpdate(VirtualFrame frame, int value);

    @Specialization(guards = {"isVirtualized(frame)"})
    protected final void doUpdateVirtualized(final VirtualFrame frame, final int value) {
        CompilerDirectives.ensureVirtualizedHere(frame);
        instructionPointerWriteNode.executeWrite(frame, value);
    }

    @Specialization(guards = {"!isVirtualized(frame)"})
    protected final void doUpdate(final VirtualFrame frame, final int value) {
        getContext(frame).setInstructionPointer(value);
    }
}
