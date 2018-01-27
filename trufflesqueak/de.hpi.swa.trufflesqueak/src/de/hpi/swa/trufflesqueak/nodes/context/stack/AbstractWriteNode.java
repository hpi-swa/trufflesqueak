package de.hpi.swa.trufflesqueak.nodes.context.stack;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

@ImportStatic(FrameAccess.class)
public abstract class AbstractWriteNode extends Node {
    @CompilationFinal protected final CompiledCodeObject code;

    public AbstractWriteNode(CompiledCodeObject code) {
        this.code = code;
    }

    public abstract void executeWrite(VirtualFrame frame, Object value);

    // This can only be used when non-virtualized
    protected ContextObject getContext(VirtualFrame frame) {
        return (ContextObject) FrameAccess.getContextOrMarker(frame);
    }
}
