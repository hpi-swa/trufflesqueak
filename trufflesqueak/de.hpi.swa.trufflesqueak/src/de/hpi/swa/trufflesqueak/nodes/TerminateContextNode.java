package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

@ImportStatic(FrameAccess.class)
public abstract class TerminateContextNode extends AbstractNodeWithCode {

    public static TerminateContextNode create(CompiledCodeObject code) {
        return TerminateContextNodeGen.create(code);
    }

    protected TerminateContextNode(CompiledCodeObject code) {
        super(code);
    }

    protected abstract void executeTerminate(VirtualFrame frame);

    @SuppressWarnings("unused")
    @Specialization(guards = {"isVirtualized(frame)"})
    protected void doTerminateVirtualized(VirtualFrame frame, @Cached("getContextOrMarker(frame)") Object contextOrMarker) {
        // do nothing, context did not leak
    }

    @Specialization(guards = {"!isVirtualized(frame)"})
    protected void doTerminate(VirtualFrame frame) {
        ContextObject context = getContext(frame);
        context.setSender(code.image.nil);
        context.atput0(CONTEXT.INSTRUCTION_POINTER, code.image.nil);
    }
}
