package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotWriteNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

@ImportStatic(FrameAccess.class)
public abstract class TerminateContextNode extends AbstractNodeWithCode {
    @Child private FrameSlotWriteNode instructionPointerWriteNode = FrameSlotWriteNode.createForInstructionPointer();

    public static TerminateContextNode create(final CompiledCodeObject code) {
        return TerminateContextNodeGen.create(code);
    }

    protected TerminateContextNode(final CompiledCodeObject code) {
        super(code);
    }

    protected abstract void executeTerminate(VirtualFrame frame);

    @Specialization(guards = {"isVirtualized(frame)"})
    protected void doTerminateVirtualized(final VirtualFrame frame) {
        // TODO: check the below is actually needed (see also GetOrCreateContextNode.materialize())
        instructionPointerWriteNode.executeWrite(frame, -1); // cannot set nil, -1 instead.
        // cannot remove sender
    }

    @Specialization(guards = {"!isVirtualized(frame)"})
    protected void doTerminate(final VirtualFrame frame) {
        getContext(frame).terminate();
    }
}
