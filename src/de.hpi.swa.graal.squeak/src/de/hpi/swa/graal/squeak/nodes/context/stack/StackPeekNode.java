package de.hpi.swa.graal.squeak.nodes.context.stack;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;

public abstract class StackPeekNode extends AbstractStackNode {
    @CompilationFinal private final int offset;

    public static StackPeekNode create(final CompiledCodeObject code, final int offset) {
        return StackPeekNodeGen.create(code, offset);
    }

    protected StackPeekNode(final CompiledCodeObject code, final int offset) {
        super(code);
        this.offset = offset;
    }

    @Specialization(guards = {"isVirtualized(frame)"})
    protected Object doPeekVirtualized(final VirtualFrame frame) {
        CompilerDirectives.ensureVirtualizedHere(frame);
        return readNode.execute(frame, frameStackPointer(frame) - offset);
    }

    @Specialization(guards = {"!isVirtualized(frame)"})
    protected Object doPeek(final VirtualFrame frame) {
        return getContext(frame).peek(offset);
    }
}
