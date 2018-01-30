package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

@ImportStatic(FrameAccess.class)
public abstract class TerminateContextNode extends Node {
    @CompilationFinal private final SqueakImageContext image;

    public static TerminateContextNode create(SqueakImageContext image) {
        return TerminateContextNodeGen.create(image);
    }

    protected TerminateContextNode(SqueakImageContext image) {
        this.image = image;
    }

    protected abstract void executeTerminate(VirtualFrame frame);

    @SuppressWarnings("unused")
    @Specialization(guards = {"isVirtualized(frame)"})
    protected void doTerminateVirtualized(VirtualFrame frame, @Cached("getContextOrMarker(frame)") Object contextOrMarker) {
        // do nothing, context did not leak
    }

    @Specialization(guards = {"!isVirtualized(frame)"})
    protected void doTerminate(@SuppressWarnings("unused") VirtualFrame frame, @Cached("getContextOrMarker(frame)") Object contextOrMarker) {
        ContextObject context = (ContextObject) contextOrMarker;
        context.setSender(image.nil);
        context.atput0(CONTEXT.INSTRUCTION_POINTER, image.nil);
    }
}
