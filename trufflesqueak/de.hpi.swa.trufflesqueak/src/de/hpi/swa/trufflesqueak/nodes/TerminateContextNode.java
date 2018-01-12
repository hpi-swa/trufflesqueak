package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.MethodContextObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.CONTEXT;

public abstract class TerminateContextNode extends AbstractContextNode {

    public static TerminateContextNode create(CompiledCodeObject code) {
        return TerminateContextNodeGen.create(code);
    }

    protected TerminateContextNode(CompiledCodeObject code) {
        super(code);
    }

    protected abstract void executeTerminate(VirtualFrame frame);

    @SuppressWarnings("unused")
    @Specialization(guards = {"context == null"})
    protected void doTerminateVirtualized(VirtualFrame frame, @Cached("getContext(frame)") MethodContextObject context) {
        // do nothing, context did not leak
    }

    @Specialization(guards = {"context != null"})
    protected void doTerminate(@SuppressWarnings("unused") VirtualFrame frame, @Cached("getContext(frame)") MethodContextObject context) {
        context.setSender(code.image.nil);
        context.atput0(CONTEXT.INSTRUCTION_POINTER, code.image.nil);
    }
}
