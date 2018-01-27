package de.hpi.swa.trufflesqueak.nodes.context.stack;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackReadNode;

public abstract class PopNReversedStackNode extends AbstractStackNode {
    @Child private FrameStackReadNode readNode;
    @CompilationFinal private final int numPop;

    public static PopNReversedStackNode create(CompiledCodeObject code, int numPop) {
        return PopNReversedStackNodeGen.create(code, numPop);
    }

    protected PopNReversedStackNode(CompiledCodeObject code, int numPop) {
        super(code);
        this.numPop = numPop;
        readNode = FrameStackReadNode.create();
    }

    @ExplodeLoop
    @Specialization(guards = {"isVirtualized(frame)"})
    protected Object[] doPopNVirtualized(VirtualFrame frame) {
        int sp = frameStackPointer(frame);
        assert sp - numPop >= 0;
        Object[] result = new Object[numPop];
        for (int i = 0; i < numPop; i++) {
            result[numPop - 1 - i] = readNode.execute(frame, sp - i);
        }
        frame.setInt(code.stackPointerSlot, sp - numPop);
        return result;
    }

    @ExplodeLoop
    @Specialization(guards = {"!isVirtualized(frame)"})
    protected Object[] doPopN(VirtualFrame frame) {
        return getContext(frame).popNReversed(numPop);
    }
}
