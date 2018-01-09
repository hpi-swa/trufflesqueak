package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.MethodContextObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.CONTEXT;

public abstract class UnwindNode extends Node {
    @CompilationFinal private final CompiledCodeObject code;

    public static UnwindNode create(CompiledCodeObject code) {
        return UnwindNodeGen.create(code);
    }

    protected UnwindNode(CompiledCodeObject code) {
        super();
        this.code = code;
    }

    protected abstract void executeUnwind(VirtualFrame frame);

    protected MethodContextObject getContext(VirtualFrame frame) {
        return (MethodContextObject) FrameUtil.getObjectSafe(frame, code.thisContextSlot);
    }

    @Specialization(guards = {"context != null"})
    protected void doUnwind(@SuppressWarnings("unused") VirtualFrame frame, @Cached("getContext(frame)") MethodContextObject context) {
        context.atput0(CONTEXT.INSTRUCTION_POINTER, -1); // FIXME: reset pc to "zero", this is a hack... MCO is a call target and therefore cached
        context.atput0(CONTEXT.SENDER, code.image.nil, false);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"context == null"})
    protected void doUnwindNoContext(VirtualFrame frame, @Cached("getContext(frame)") MethodContextObject context) {
        // do nothing, context did not leak
    }
}
