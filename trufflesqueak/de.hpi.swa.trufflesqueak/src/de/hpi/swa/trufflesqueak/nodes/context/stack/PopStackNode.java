package de.hpi.swa.trufflesqueak.nodes.context.stack;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackWriteNode;

public abstract class PopStackNode extends AbstractStackNode {
    @Child private FrameStackReadNode readNode;
    @Child private FrameStackWriteNode writeNode;

    public static PopStackNode create(CompiledCodeObject code) {
        return PopStackNodeGen.create(code);
    }

    protected PopStackNode(CompiledCodeObject code) {
        super(code);
        readNode = FrameStackReadNode.create();
        writeNode = FrameStackWriteNode.create();
    }

    @Specialization(guards = {"isVirtualized(frame)"})
    public Object doPopVirtualized(VirtualFrame frame) {
        CompilerDirectives.ensureVirtualizedHere(frame);
        long sp = frameStackPointer(frame);
        assert sp >= 0;
        setFrameStackPointer(frame, sp - 1);
        Object value = readNode.execute(frame, (int) sp);
        writeNode.execute(frame, (int) sp, code.image.nil);
        return value;
    }

    @Specialization(guards = {"!isVirtualized(frame)"})
    protected Object doPop(VirtualFrame frame) {
        return getContext(frame).pop();
    }
}
