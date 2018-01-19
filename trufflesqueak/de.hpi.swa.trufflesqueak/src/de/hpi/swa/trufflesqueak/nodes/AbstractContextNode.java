package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;

public abstract class AbstractContextNode extends Node {
    @CompilationFinal protected final CompiledCodeObject code;

    protected AbstractContextNode(CompiledCodeObject code) {
        this.code = code;
    }

    protected ContextObject getContext(VirtualFrame frame) {
        return (ContextObject) FrameUtil.getObjectSafe(frame, code.thisContextSlot);
    }
}
