package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotReadNode;

public class Pop extends StackBytecodeNode {
    public Pop(CompiledMethodObject cm, int idx) {
        super(cm, idx);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        try {
            return super.executeGeneric(frame);
        } finally {
            decSP(frame);
        }
    }

    @Override
    public FrameSlotNode createChild(CompiledMethodObject cm) {
        return FrameSlotReadNode.top(cm);
    }
}
