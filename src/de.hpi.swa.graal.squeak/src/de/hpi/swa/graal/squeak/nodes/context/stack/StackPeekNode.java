package de.hpi.swa.graal.squeak.nodes.context.stack;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;

public abstract class StackPeekNode extends AbstractStackNode {
    private final int offset;

    protected StackPeekNode(final CompiledCodeObject code, final int offset) {
        super(code);
        this.offset = offset;
    }

    public static StackPeekNode create(final CompiledCodeObject code, final int offset) {
        return StackPeekNodeGen.create(code, offset);
    }

    @Specialization(guards = {"isVirtualized(frame)"})
    protected final Object doPeekVirtualized(final VirtualFrame frame) {
        return getReadNode().execute(frame, frameStackPointer(frame) - offset);
    }

    @Fallback
    protected final Object doPeek(final VirtualFrame frame) {
        return getContext(frame).peek(offset);
    }
}
