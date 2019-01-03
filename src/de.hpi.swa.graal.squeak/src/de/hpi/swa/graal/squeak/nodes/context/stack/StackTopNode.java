package de.hpi.swa.graal.squeak.nodes.context.stack;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;

public abstract class StackTopNode extends AbstractStackNode {
    public static StackTopNode create(final CompiledCodeObject code) {
        return StackTopNodeGen.create(code);
    }

    protected StackTopNode(final CompiledCodeObject code) {
        super(code);
    }

    @Specialization
    protected final Object doTop(final VirtualFrame frame) {
        return readNode.execute(frame, frameStackPointer(frame) - 1);
    }
}
