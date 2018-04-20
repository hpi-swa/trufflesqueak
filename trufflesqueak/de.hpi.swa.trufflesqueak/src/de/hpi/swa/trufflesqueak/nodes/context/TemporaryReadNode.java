package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.SqueakNodeWithCode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameSlotReadNode;

public abstract class TemporaryReadNode extends SqueakNodeWithCode {
    @CompilationFinal private final long tempIndex;
    @Child private FrameSlotReadNode readNode;

    public static SqueakNode create(final CompiledCodeObject code, final long tempIndex) {
        return TemporaryReadNodeGen.create(code, tempIndex);
    }

    protected TemporaryReadNode(final CompiledCodeObject code, final long tempIndex) {
        super(code);
        this.tempIndex = tempIndex;
        if (tempIndex >= 0 && code.canBeVirtualized()) {
            readNode = FrameSlotReadNode.create(code.getStackSlot((int) tempIndex));
        }
    }

    @Specialization(guards = {"isVirtualized(frame)"})
    public Object doReadVirtualized(final VirtualFrame frame) {
        CompilerDirectives.ensureVirtualizedHere(frame);
        return readNode.executeRead(frame);
    }

    @Specialization(guards = {"!isVirtualized(frame)"})
    public Object doRead(final VirtualFrame frame) {
        return getContext(frame).atTemp(tempIndex);
    }
}
