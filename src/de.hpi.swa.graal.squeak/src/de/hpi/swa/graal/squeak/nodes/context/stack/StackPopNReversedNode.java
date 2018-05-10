package de.hpi.swa.graal.squeak.nodes.context.stack;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;

public abstract class StackPopNReversedNode extends AbstractStackPopNode {
    @CompilationFinal private final int numPop;

    public static StackPopNReversedNode create(final CompiledCodeObject code, final int numPop) {
        return StackPopNReversedNodeGen.create(code, numPop);
    }

    protected StackPopNReversedNode(final CompiledCodeObject code, final int numPop) {
        super(code);
        this.numPop = numPop;
    }

    @ExplodeLoop
    @Specialization(guards = {"isVirtualized(frame)"})
    protected Object[] doPopNVirtualized(final VirtualFrame frame) {
        CompilerDirectives.ensureVirtualizedHere(frame);
        final int sp = frameStackPointer(frame);
        assert sp - numPop >= -1;
        final Object[] result = new Object[numPop];
        for (int i = 0; i < numPop; i++) {
            result[numPop - 1 - i] = atStackAndClear(frame, sp - i);
        }
        setFrameStackPointer(frame, sp - numPop);
        return result;
    }

    @Specialization(guards = {"!isVirtualized(frame)"})
    protected Object[] doPopN(final VirtualFrame frame) {
        final ContextObject context = getContext(frame);
        final long sp = context.getStackPointer();
        assert sp - numPop >= 0;
        final Object[] result = new Object[numPop];
        for (int i = 0; i < numPop; i++) {
            result[numPop - 1 - i] = atStackAndClear(context, sp - i);
        }
        context.setStackPointer(sp - numPop);
        return result;
    }
}
