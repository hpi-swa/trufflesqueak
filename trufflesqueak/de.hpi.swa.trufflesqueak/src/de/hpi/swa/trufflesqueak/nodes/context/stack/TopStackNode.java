package de.hpi.swa.trufflesqueak.nodes.context.stack;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackReadNode;

public abstract class TopStackNode extends AbstractStackNode {
    @Child private FrameStackReadNode readNode;

    public static TopStackNode create(CompiledCodeObject code) {
        return TopStackNodeGen.create(code);
    }

    protected TopStackNode(CompiledCodeObject code) {
        super(code);
        readNode = FrameStackReadNode.create();
    }

    @Specialization(guards = {"isVirtualized(frame)"})
    protected Object doTopVirtualized(VirtualFrame frame) {
        return readNode.execute(frame, frameStackPointer(frame) - 1);
    }

    @Specialization(guards = {"!isVirtualized(frame)"})
    protected Object doTop(VirtualFrame frame) {
        return getContext(frame).top();
    }
}
