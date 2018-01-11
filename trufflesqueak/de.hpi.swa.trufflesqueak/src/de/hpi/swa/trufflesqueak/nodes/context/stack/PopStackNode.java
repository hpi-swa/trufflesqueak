package de.hpi.swa.trufflesqueak.nodes.context.stack;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackReadNode;

public abstract class PopStackNode extends AbstractStackNode {
    @Child private FrameStackReadNode readNode;

    public static PopStackNode create(CompiledCodeObject code) {
        return PopStackNodeGen.create(code);
    }

    protected PopStackNode(CompiledCodeObject code) {
        super(code);
        readNode = FrameStackReadNode.create();
    }

    @Specialization(guards = {"isVirtualized(frame)"})
    public Object doPopVirtualized(VirtualFrame frame) {
        int newSP = frameStackPointer(frame) - 1;
        frame.setInt(code.stackPointerSlot, newSP);
        if (newSP <= 0) {
            return frame.getArguments()[frame.getArguments().length - 1 + newSP];
        }
        return readNode.execute(frame, newSP);
    }

    @Specialization(guards = {"!isVirtualized(frame)"})
    protected Object doPop(VirtualFrame frame) {
        return getContext(frame).pop();
    }
}
