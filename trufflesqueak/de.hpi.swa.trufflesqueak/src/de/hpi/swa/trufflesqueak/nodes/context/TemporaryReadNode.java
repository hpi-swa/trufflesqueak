package de.hpi.swa.trufflesqueak.nodes.context;

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
    @Child private ArgumentNode argumentNode;

    public static SqueakNode create(CompiledCodeObject code, int tempIndex) {
        return TemporaryReadNodeGen.create(code, tempIndex);
    }

    protected TemporaryReadNode(CompiledCodeObject code, int tempIndex) {
        super(code);
        this.tempIndex = tempIndex;
        int stackIndex = code.convertTempIndexToStackIndex(tempIndex);
        if (stackIndex >= 0) {
            readNode = FrameSlotReadNode.create(code.getStackSlot(stackIndex));
        } else {
            argumentNode = ArgumentNode.create(code, 1 + tempIndex);
        }
    }

    @Specialization(guards = {"isVirtualized(frame)"})
    public Object doReadVirtualized(VirtualFrame frame) {
        if (readNode != null) {
            return readNode.executeRead(frame);
        } else {
            return argumentNode.executeGeneric(frame);
        }
    }

    @Specialization(guards = {"!isVirtualized(frame)"})
    public Object doRead(VirtualFrame frame) {
        return getContext(frame).atTemp(tempIndex);
    }
}
