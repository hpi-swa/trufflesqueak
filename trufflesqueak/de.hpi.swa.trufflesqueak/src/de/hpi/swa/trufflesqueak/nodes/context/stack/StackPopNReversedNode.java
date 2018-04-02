package de.hpi.swa.trufflesqueak.nodes.context.stack;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public abstract class StackPopNReversedNode extends AbstractStackPopNode {
    @CompilationFinal private final int numPop;

    public static StackPopNReversedNode create(CompiledCodeObject code, int numPop) {
        return StackPopNReversedNodeGen.create(code, numPop);
    }

    protected StackPopNReversedNode(CompiledCodeObject code, int numPop) {
        super(code);
        this.numPop = numPop;
    }

    @ExplodeLoop
    @Specialization(guards = {"isVirtualized(frame)"})
    protected Object[] doPopNVirtualized(VirtualFrame frame) {
        CompilerDirectives.ensureVirtualizedHere(frame);
        long sp = frameStackPointer(frame);
        assert sp - numPop >= -1;
        Object[] result = new Object[numPop];
        for (int i = 0; i < numPop; i++) {
            result[numPop - 1 - i] = atStackAndClear(frame, (int) (sp - i));
        }
        setFrameStackPointer(frame, sp - numPop);
        return result;
    }

    @ExplodeLoop
    @Specialization(guards = {"!isVirtualized(frame)"})
    protected Object[] doPopN(VirtualFrame frame) {
        return getContext(frame).popNReversed(numPop);
    }
}
