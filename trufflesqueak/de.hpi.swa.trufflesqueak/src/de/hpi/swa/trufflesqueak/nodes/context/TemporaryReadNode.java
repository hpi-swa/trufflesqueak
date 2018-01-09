package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.SqueakNodeWithCode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameSlotReadNode;

public abstract class TemporaryReadNode extends SqueakNodeWithCode {
    @Child private FrameSlotReadNode readNode;
    @CompilationFinal private final int stackIndex;

    public static SqueakNode create(CompiledCodeObject code, int tempIndex) {
        int stackIndex = code.convertTempIndexToStackIndex(tempIndex);
        if (stackIndex >= 0) {
            return TemporaryReadNodeGen.create(code, stackIndex);
        } else {
            return ArgumentNode.create(code, 1 + tempIndex);
        }
    }

    protected TemporaryReadNode(CompiledCodeObject code, int stackIndex) {
        super(code);
        this.stackIndex = stackIndex;
        readNode = FrameSlotReadNode.create(code.getStackSlot(stackIndex));
    }

    @Specialization(guards = {"isVirtualized(frame)"})
    public Object doReadVirtualized(VirtualFrame frame) {
        return readNode.executeRead(frame);
    }

    @Specialization(guards = {"!isVirtualized(frame)"})
    public Object doRead(VirtualFrame frame) {
        return getContext(frame).getArgument(stackIndex);
    }
}
