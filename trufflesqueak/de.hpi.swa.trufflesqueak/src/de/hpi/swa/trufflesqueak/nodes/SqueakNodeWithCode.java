package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.util.FrameMarker;

/**
 * This is the base class for Squeak bytecode evaluation.
 */
@TypeSystemReference(SqueakTypes.class)
public abstract class SqueakNodeWithCode extends SqueakNode {
    @CompilationFinal protected final CompiledCodeObject code;
    @Child private FrameSlotReadNode readContextNode;

    public SqueakNodeWithCode(CompiledCodeObject code) {
        this.code = code;
        readContextNode = FrameSlotReadNode.create(code.thisContextOrMarkerSlot);
    }

    protected boolean isVirtualized(VirtualFrame frame) {
        return readContextNode.executeRead(frame) instanceof FrameMarker;
    }

    protected Object getContextOrMarker(VirtualFrame frame) {
        return readContextNode.executeRead(frame);
    }

    protected ContextObject getContext(VirtualFrame frame) {
        return (ContextObject) readContextNode.executeRead(frame);
    }

    protected FrameMarker getFrameMarker(VirtualFrame frame) {
        return (FrameMarker) readContextNode.executeRead(frame);
    }
}
