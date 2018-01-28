package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

@ImportStatic(FrameAccess.class)
public abstract class GetOrCreateContextNode extends Node {
    @CompilationFinal protected final CompiledCodeObject code;

    public static GetOrCreateContextNode create(CompiledCodeObject code) {
        return GetOrCreateContextNodeGen.create(code);
    }

    protected GetOrCreateContextNode(CompiledCodeObject code) {
        this.code = code;
    }

    public abstract ContextObject executeGet(VirtualFrame frame, int pc);

    @Specialization(guards = {"isVirtualized(frame)"})
    protected ContextObject doCreateVirtualized(VirtualFrame frame, int pc) {
        ContextObject context = createContext(frame, pc);
        frame.setObject(code.thisContextOrMarkerSlot, context);
        return context;
    }

    @Specialization(guards = {"!isVirtualized(frame)"})
    protected ContextObject doGet(VirtualFrame frame, @SuppressWarnings("unused") int pc) {
        return (ContextObject) FrameAccess.getContextOrMarker(frame);
    }

    private ContextObject createContext(VirtualFrame frame, int pc) {
        CompilerDirectives.transferToInterpreter();
        code.invalidateNoContextNeededAssumption();
        int sp = FrameAccess.getStackPointer(frame);
        return ContextObject.create(code, frame, pc, sp);
    }
}