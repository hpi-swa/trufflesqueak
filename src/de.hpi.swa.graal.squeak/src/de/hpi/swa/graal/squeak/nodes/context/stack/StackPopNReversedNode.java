package de.hpi.swa.graal.squeak.nodes.context.stack;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;

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
    @Specialization(guards = {"isVirtualized(frame)"})
    protected final Object[] doPopNVirtualized(final VirtualFrame frame) {
        final int sp = frameStackPointer(frame);
        assert sp - numPop >= -1;
        final Object[] result = new Object[numPop];
        for (int i = 0; i < numPop; i++) {
            result[numPop - 1 - i] = atStackAndClear(frame, sp - i);
            assert result[numPop - 1 - i] != null;
        }
        setFrameStackPointer(frame, sp - numPop);
        return result;
    }

    @ExplodeLoop
    @Specialization(guards = {"!isVirtualized(frame)"})
    protected final Object[] doPopN(final VirtualFrame frame) {
        final ContextObject context = getContext(frame);
        final long sp = context.getStackPointer();
        assert sp - numPop >= 0;
        final Object[] result = new Object[numPop];
        for (int i = 0; i < numPop; i++) {
            result[numPop - 1 - i] = atStackAndClear(context, sp - i);
            assert result[numPop - 1 - i] != null;
        }
        context.setStackPointer(sp - numPop);
        return result;
    }
}
