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
    @CompilationFinal private final int tempIndex;
    @Child private FrameSlotReadNode readNode;

    public static SqueakNode create(CompiledCodeObject code, int tempIndex) {
        return TemporaryReadNodeGen.create(code, tempIndex);
    }

    protected TemporaryReadNode(CompiledCodeObject code, int tempIndex) {
        super(code);
        this.tempIndex = tempIndex;
        if (tempIndex >= 0) {
            readNode = FrameSlotReadNode.create(code.getStackSlot(tempIndex));
        }
    }

    @Specialization(guards = {"isVirtualized(frame)"})
    public Object doReadVirtualized(VirtualFrame frame) {
        CompilerDirectives.ensureVirtualizedHere(frame);
        return readNode.executeRead(frame);
    }

    @Specialization(guards = {"!isVirtualized(frame)"})
    public Object doRead(VirtualFrame frame) {
        return getContext(frame).atTemp(tempIndex);
    }
}
