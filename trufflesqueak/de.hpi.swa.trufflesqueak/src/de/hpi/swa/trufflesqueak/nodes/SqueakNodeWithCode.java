package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

/**
 * This is the base class for Squeak bytecode evaluation.
 */
@TypeSystemReference(SqueakTypes.class)
public abstract class SqueakNodeWithCode extends SqueakNode {
    @CompilationFinal protected final CompiledCodeObject code;

    public SqueakNodeWithCode(CompiledCodeObject code) {
        this.code = code;
    }

    protected boolean isVirtualized(VirtualFrame frame) {
        return FrameUtil.getObjectSafe(frame, code.thisContextSlot) == null;
    }

    protected ContextObject getContext(VirtualFrame frame) {
        return FrameAccess.getContext(frame, code.thisContextSlot);
    }
}
