package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.MethodContextObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.CONTEXT;

public abstract class GetMethodContextNode extends Node {
    @CompilationFinal private final CompiledCodeObject code;

    public static GetMethodContextNode create(CompiledCodeObject code) {
        return GetMethodContextNodeGen.create(code);
    }

    protected abstract MethodContextObject executeGetMethodContext(VirtualFrame frame, int pc);

    public GetMethodContextNode(CompiledCodeObject code) {
        super();
        this.code = code;
    }

    @Specialization
    public MethodContextObject doGet(VirtualFrame frame, int pc) {
        MethodContextObject context = (MethodContextObject) FrameUtil.getObjectSafe(frame, code.thisContextSlot);
        if (context == null) {
            context = MethodContextObject.createWriteableContextObject(code.image, code.frameSize());
            context.atput0(CONTEXT.METHOD, code);
            context.atput0(CONTEXT.SENDER, null, false);
            context.atput0(CONTEXT.INSTRUCTION_POINTER, pc);
            context.atput0(CONTEXT.RECEIVER, frame.getArguments()[0]);
            BlockClosureObject closure = FrameAccess.getClosure(frame);
            context.atput0(CONTEXT.CLOSURE_OR_NIL, closure == null ? code.image.nil : closure);
            context.atput0(CONTEXT.STACKPOINTER, FrameUtil.getIntSafe(frame, code.stackPointerSlot));
            frame.setObject(code.thisContextSlot, context);
        }
        return context;
    }
}