package de.hpi.swa.trufflesqueak.nodes.context.stack;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public abstract class StackPopNode extends AbstractStackPopNode {
    public static StackPopNode create(CompiledCodeObject code) {
        return StackPopNodeGen.create(code);
    }

    protected StackPopNode(CompiledCodeObject code) {
        super(code);
    }

    @Specialization(guards = {"isVirtualized(frame)"})
    public Object doPopVirtualized(VirtualFrame frame) {
        CompilerDirectives.ensureVirtualizedHere(frame);
        long sp = frameStackPointer(frame);
        assert sp >= 0;
        setFrameStackPointer(frame, sp - 1);
        return atStackAndClear(frame, (int) sp);
    }

    @Specialization(guards = {"!isVirtualized(frame)"})
    protected Object doPop(VirtualFrame frame) {
        return getContext(frame).pop();
    }
}
