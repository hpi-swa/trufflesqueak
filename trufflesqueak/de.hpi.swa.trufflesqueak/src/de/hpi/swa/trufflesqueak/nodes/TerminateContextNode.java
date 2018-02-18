package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameSlotWriteNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

@ImportStatic(FrameAccess.class)
public abstract class TerminateContextNode extends AbstractNodeWithCode {
    @Child private FrameSlotWriteNode instructionPointerWriteNode;

    public static TerminateContextNode create(CompiledCodeObject code) {
        return TerminateContextNodeGen.create(code);
    }

    protected TerminateContextNode(CompiledCodeObject code) {
        super(code);
        instructionPointerWriteNode = FrameSlotWriteNode.create(code.instructionPointerSlot);
    }

    protected abstract void executeTerminate(VirtualFrame frame);

    @Specialization(guards = {"isVirtualized(frame)"})
    protected void doTerminateVirtualized(VirtualFrame frame) {
        CompilerDirectives.ensureVirtualizedHere(frame);
        instructionPointerWriteNode.executeWrite(frame, -1); // cannot set nil, -1 instead.
        // cannot remove sender
    }

    @Specialization(guards = {"!isVirtualized(frame)"})
    protected void doTerminate(VirtualFrame frame) {
        getContext(frame).terminate();
    }
}
