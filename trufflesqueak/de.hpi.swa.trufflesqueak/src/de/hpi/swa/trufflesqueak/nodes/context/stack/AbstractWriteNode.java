package de.hpi.swa.trufflesqueak.nodes.context.stack;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.MethodContextObject;

public abstract class AbstractWriteNode extends Node {
    @CompilationFinal protected final CompiledCodeObject code;

    public AbstractWriteNode(CompiledCodeObject code) {
        this.code = code;
    }

    public abstract void executeWrite(VirtualFrame frame, Object value);

    protected boolean isVirtualized(VirtualFrame frame) {
        return FrameUtil.getObjectSafe(frame, code.thisContextSlot) == null;
    }

    protected MethodContextObject getContext(VirtualFrame frame) {
        return (MethodContextObject) FrameUtil.getObjectSafe(frame, code.thisContextSlot);
    }
}
