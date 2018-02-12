package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.util.FrameMarker;

public abstract class AbstractNodeWithCode extends Node {
    @CompilationFinal protected final CompiledCodeObject code;
    @Child protected FrameSlotReadNode contextOrMarkerReadNode;

    protected AbstractNodeWithCode(CompiledCodeObject code) {
        this.code = code;
        contextOrMarkerReadNode = FrameSlotReadNode.create(code.thisContextOrMarkerSlot);
    }

    protected boolean isVirtualized(VirtualFrame frame) {
        return contextOrMarkerReadNode.executeRead(frame) instanceof FrameMarker;
    }

    protected Object getContextOrMarker(VirtualFrame frame) {
        return contextOrMarkerReadNode.executeRead(frame);
    }

    protected ContextObject getContext(VirtualFrame frame) {
        return (ContextObject) contextOrMarkerReadNode.executeRead(frame);
    }

    protected FrameMarker getFrameMarker(VirtualFrame frame) {
        return (FrameMarker) contextOrMarkerReadNode.executeRead(frame);
    }
}
