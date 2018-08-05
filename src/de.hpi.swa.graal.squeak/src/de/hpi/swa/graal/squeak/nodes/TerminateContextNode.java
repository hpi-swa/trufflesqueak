package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.util.FrameAccess;

@ImportStatic(FrameAccess.class)
public abstract class TerminateContextNode extends AbstractNodeWithCode {

    public static TerminateContextNode create(final CompiledCodeObject code) {
        return TerminateContextNodeGen.create(code);
    }

    protected TerminateContextNode(final CompiledCodeObject code) {
        super(code);
    }

    protected abstract void executeTerminate(VirtualFrame frame);

    @Specialization(guards = {"isVirtualized(frame)"})
    protected final void doTerminateVirtualized(final VirtualFrame frame) {
        // TODO: check the below is actually needed (see also GetOrCreateContextNode.materialize())
        frame.setInt(code.instructionPointerSlot, -1); // cannot set nil, -1 instead.
        // cannot remove sender
    }

    @Fallback
    protected final void doTerminate(final VirtualFrame frame) {
        getContext(frame).terminate();
    }
}
