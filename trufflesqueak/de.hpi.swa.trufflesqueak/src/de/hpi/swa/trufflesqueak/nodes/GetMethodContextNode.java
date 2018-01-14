package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.MethodContextObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public abstract class GetMethodContextNode extends Node {
    @CompilationFinal private final CompiledCodeObject code;

    public static GetMethodContextNode create(CompiledCodeObject code) {
        return GetMethodContextNodeGen.create(code);
    }

    public abstract MethodContextObject executeGetMethodContext(VirtualFrame frame, int pc);

    protected GetMethodContextNode(CompiledCodeObject code) {
        super();
        this.code = code;
    }

    @Specialization
    protected MethodContextObject doGet(VirtualFrame frame, int pc) {
        MethodContextObject context = (MethodContextObject) FrameUtil.getObjectSafe(frame, code.thisContextSlot);
        if (context == null) {
            CompilerDirectives.transferToInterpreter();
            code.invalidateNoContextNeededAssumption();
            context = MethodContextObject.createWriteableContextObject(code.image, code.frameSize());
            context.atput0(CONTEXT.METHOD, code);
            context.setSender(FrameAccess.getSender(frame));
            context.atput0(CONTEXT.INSTRUCTION_POINTER, pc);
            context.atput0(CONTEXT.RECEIVER, FrameAccess.getReceiver(frame));
            BlockClosureObject closure = FrameAccess.getClosure(frame);
            context.atput0(CONTEXT.CLOSURE_OR_NIL, closure == null ? code.image.nil : closure);
            context.atput0(CONTEXT.STACKPOINTER, 1);
            frame.setObject(code.thisContextSlot, context);
        }
        return context;
    }
}