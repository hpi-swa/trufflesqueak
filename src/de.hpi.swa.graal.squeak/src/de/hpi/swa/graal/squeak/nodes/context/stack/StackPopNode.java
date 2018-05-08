package de.hpi.swa.graal.squeak.nodes.context.stack;

import com.oracle.truffle.api.CompilerDirectives;
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

    @Specialization(guards = {"isVirtualized(frame)"})
    public Object doPopVirtualized(final VirtualFrame frame) {
        CompilerDirectives.ensureVirtualizedHere(frame);
        final int sp = frameStackPointer(frame);
        assert sp >= 0;
        setFrameStackPointer(frame, sp - 1);
        return atStackAndClear(frame, sp);
    }

    @Specialization(guards = {"!isVirtualized(frame)"})
    protected Object doPop(final VirtualFrame frame) {
        return getContext(frame).pop();
    }
}
