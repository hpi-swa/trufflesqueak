package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public class GetOrCreateMethodContextNode extends Node {
    @CompilationFinal private final CompiledCodeObject code;

    public static GetOrCreateMethodContextNode create(CompiledCodeObject code) {
        return new GetOrCreateMethodContextNode(code);
    }

    protected GetOrCreateMethodContextNode(CompiledCodeObject code) {
        this.code = code;
    }

    public ContextObject executeGetMethodContext(VirtualFrame frame, int pc) {
        ContextObject context = (ContextObject) FrameUtil.getObjectSafe(frame, code.thisContextSlot);
        if (context == null) {
            context = createContext(frame, pc);
            frame.setObject(code.thisContextSlot, context);
        }
        return context;
    }

    private ContextObject createContext(VirtualFrame frame, int pc) {
        CompilerDirectives.transferToInterpreter();
        code.invalidateNoContextNeededAssumption();
        ContextObject context = ContextObject.create(code.image, code.frameSize());
        context.atput0(CONTEXT.METHOD, code);
        context.setSender(FrameAccess.getSender(frame));
        context.atput0(CONTEXT.INSTRUCTION_POINTER, pc);
        context.atput0(CONTEXT.RECEIVER, FrameAccess.getReceiver(frame));
        BlockClosureObject closure = FrameAccess.getClosure(frame);
        context.atput0(CONTEXT.CLOSURE_OR_NIL, closure == null ? code.image.nil : closure);
        context.atput0(CONTEXT.STACKPOINTER, 1);
        return context;
    }
}