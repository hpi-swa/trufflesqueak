package de.hpi.swa.trufflesqueak.nodes.context.stack;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public abstract class StackPeekNode extends AbstractStackNode {
    @CompilationFinal private final int offset;

    public static StackPeekNode create(CompiledCodeObject code, int offset) {
        return StackPeekNodeGen.create(code, offset);
    }

    protected StackPeekNode(CompiledCodeObject code, int offset) {
        super(code);
        this.offset = offset;
    }

    @Specialization(guards = {"isVirtualized(frame)"})
    protected Object doPeekVirtualized(VirtualFrame frame) {
        CompilerDirectives.ensureVirtualizedHere(frame);
        return readNode.execute(frame, (int) frameStackPointer(frame) - offset);
    }

    @Specialization(guards = {"!isVirtualized(frame)"})
    protected Object doPeek(VirtualFrame frame) {
        return getContext(frame).peek(offset);
    }
}
