package de.hpi.swa.graal.squeak.nodes.context.stack;

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

    @Specialization
    protected final Object doPeek(final VirtualFrame frame) {
        return readNode.execute(frame, frameStackPointer(frame) - offset);
    }
}
