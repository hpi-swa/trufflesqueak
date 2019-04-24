package de.hpi.swa.graal.squeak.nodes.context.stack;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithCode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackReadAndClearNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class StackPopNReversedNode extends AbstractNodeWithCode {
    @Child private FrameStackReadAndClearNode readAndClearNode;
    private final int numPop;

    private StackPopNReversedNode(final CompiledCodeObject code, final int numPop) {
        super(code);
        this.numPop = numPop;
        readAndClearNode = FrameStackReadAndClearNode.create(code);
    }

    public static StackPopNReversedNode create(final CompiledCodeObject code, final int numPop) {
        return new StackPopNReversedNode(code, numPop);
    }

    @ExplodeLoop
    public Object[] executePopN(final VirtualFrame frame) {
        final int currentSP = FrameAccess.getStackPointer(frame, code);
        assert currentSP - numPop >= 0;
        final Object[] result = new Object[numPop];
        for (int i = 1; i <= numPop; i++) {
            result[numPop - i] = readAndClearNode.execute(frame, currentSP - i);
            assert result[numPop - i] != null;
        }
        FrameAccess.setStackPointer(frame, code, currentSP - numPop);
        return result;
    }
}
