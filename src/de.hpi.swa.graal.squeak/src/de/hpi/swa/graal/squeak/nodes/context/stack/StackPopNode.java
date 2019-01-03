package de.hpi.swa.graal.squeak.nodes.context.stack;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;

public abstract class StackPopNode extends AbstractStackPopNode {
    public static StackPopNode create(final CompiledCodeObject code) {
        return StackPopNodeGen.create(code);
    }

    protected StackPopNode(final CompiledCodeObject code) {
        super(code);
    }

    @Specialization
    public final Object doPop(final VirtualFrame frame) {
        final int newSP = frameStackPointer(frame) - 1;
        assert newSP >= 0 : "Bad stack pointer";
        setFrameStackPointer(frame, newSP);
        return atStackAndClear(frame, newSP);
    }
}
