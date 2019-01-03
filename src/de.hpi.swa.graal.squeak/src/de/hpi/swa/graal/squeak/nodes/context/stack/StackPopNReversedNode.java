package de.hpi.swa.graal.squeak.nodes.context.stack;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;

public abstract class StackPopNReversedNode extends AbstractStackPopNode {
    private final int numPop;

    public static StackPopNReversedNode create(final CompiledCodeObject code, final int numPop) {
        return StackPopNReversedNodeGen.create(code, numPop);
    }

    protected StackPopNReversedNode(final CompiledCodeObject code, final int numPop) {
        super(code);
        this.numPop = numPop;
    }

    @ExplodeLoop
    @Specialization
    protected final Object[] doPopN(final VirtualFrame frame) {
        final int currentSP = frameStackPointer(frame);
        assert currentSP - numPop >= 0;
        final Object[] result = new Object[numPop];
        for (int i = 1; i <= numPop; i++) {
            result[numPop - i] = atStackAndClear(frame, currentSP - i);
            assert result[numPop - i] != null;
        }
        setFrameStackPointer(frame, currentSP - numPop);
        return result;
    }
}
