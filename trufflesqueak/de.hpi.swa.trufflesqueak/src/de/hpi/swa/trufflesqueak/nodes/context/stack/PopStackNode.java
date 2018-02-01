package de.hpi.swa.trufflesqueak.nodes.context.stack;

import com.oracle.truffle.api.CompilerDirectives;
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
        CompilerDirectives.ensureVirtualizedHere(frame);
        int sp = frameStackPointer(frame);
        if (sp < 0) {
            return frame.getArguments()[frame.getArguments().length + sp];
        } else {
            setFrameStackPointer(frame, sp - 1);
            return readNode.execute(frame, sp);
        }
    }

    @Specialization(guards = {"!isVirtualized(frame)"})
    protected Object doPop(VirtualFrame frame) {
        return getContext(frame).pop();
    }
}
