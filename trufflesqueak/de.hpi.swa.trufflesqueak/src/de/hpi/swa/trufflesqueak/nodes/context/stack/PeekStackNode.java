package de.hpi.swa.trufflesqueak.nodes.context.stack;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackReadNode;

public abstract class PeekStackNode extends AbstractStackNode {
    @CompilationFinal private final int offset;
    @Child private FrameStackReadNode readNode = FrameStackReadNode.create();

    public static PeekStackNode create(CompiledCodeObject code, int offset) {
        return PeekStackNodeGen.create(code, offset);
    }

    protected PeekStackNode(CompiledCodeObject code, int offset) {
        super(code);
        this.offset = offset;
    }

    @Specialization(guards = {"isVirtualized(frame)"})
    protected Object doPeekVirtualized(VirtualFrame frame) {
        CompilerDirectives.ensureVirtualizedHere(frame);
        return readNode.execute(frame, frameStackPointer(frame) - offset);
    }

    @Specialization(guards = {"!isVirtualized(frame)"})
    protected Object doPeek(VirtualFrame frame) {
        return getContext(frame).peek(offset);
    }
}
