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

    @Specialization(guards = {"isVirtualized(frame, code)"})
    public Object doPopVirtualized(VirtualFrame frame) {
        int sp = frameStackPointer(frame);
        if (sp < 0) {
            return frame.getArguments()[frame.getArguments().length + sp];
        } else {
            frame.setInt(code.stackPointerSlot, sp - 1);
            return readNode.execute(frame, sp);
        }
    }

    @Specialization(guards = {"!isVirtualized(frame, code)"})
    protected Object doPop(VirtualFrame frame) {
        return getContext(frame).pop();
    }
}
